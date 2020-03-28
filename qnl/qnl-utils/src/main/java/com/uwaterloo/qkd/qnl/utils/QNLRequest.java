package com.uwaterloo.qkd.qnl.utils;

import java.util.Formatter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class QNLRequest {

    private String srcSiteId;
    private int srcSiteIdLen;
    private String dstSiteId;
    private int dstSiteIdLen;
    private short opId;
    private short respOpId;
    private long keyBlockIndex;
    private int kpBlockBytesSz;
    private int frameSz;
    private int hmacSz;
    private int payLoadSz;
    private ByteBuf payBuf;
    private boolean payLoadMode = false;
    private byte[] hmac;
    private String uuid;

    //private static Logger LOGGER = LoggerFactory.getLogger(QNLRequest.class);

    public QNLRequest(int kpBlockByteSz) {
        this.kpBlockBytesSz = kpBlockByteSz;
        payBuf = Unpooled.buffer(kpBlockByteSz + 128);
        frameSz = 0;
        hmacSz = 0;
        payLoadSz = 0;
    }

    public void setOpId(short op) {
        frameSz += Short.BYTES;
        opId = op;
    }

    public short getOpId() {
        return opId;
    }

    public void setRespOpId(short op) {
        frameSz += Short.BYTES;
        respOpId = op;
    }

    public short getRespOpId() {
        return respOpId;
    }

    public void setUUID(String id) {
        frameSz += id.length() + 2;
        uuid = id;
    }

    public String getUUID() {
        return uuid;
    }

    public void setKeyBlockIndex(long index) {
        frameSz += Long.BYTES;
        keyBlockIndex = index;
    }

    public long getKeyBlockIndex() {
        return keyBlockIndex;
    }

    public void setSiteIds(String src, String dst) {
        srcSiteIdLen = src.length();
        frameSz += srcSiteIdLen + 2;
        srcSiteId = src;

        dstSiteIdLen = dst.length();
        frameSz += srcSiteIdLen + 2;
        dstSiteId = dst;
    }

    public String getSrcSiteId() {
        return srcSiteId;
    }

    public String getDstSiteId() {
        return dstSiteId;
    }

    public void encode(ByteBuf out) {
        switch (opId) {
        case QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
        case QNLConstants.REQ_GET_KP_BLOCK_INDEX:
            break;
        case QNLConstants.REQ_POST_ALLOC_KP_BLOCK:
            frameSz += payBuf.readableBytes();
            payLoadSz = payBuf.readableBytes();
            break;
        case QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
            frameSz += payBuf.readableBytes();
            payLoadSz = payBuf.readableBytes();
            break;
        case QNLConstants.REQ_POST_KP_BLOCK_INDEX:
            break;
        }
        frameSz += Integer.BYTES; // for frameSz itself
        frameSz += Integer.BYTES; // for hmac
        out.writeInt(frameSz); // frameSz does not include calculated MAC
        out.writeInt(hmacSz); // need to reset it after calculate HMAC
        out.writeShort(opId);
        out.writeShort(srcSiteIdLen);
        out.writeBytes(srcSiteId.getBytes(), 0, srcSiteIdLen);
        out.writeShort(dstSiteIdLen);
        out.writeBytes(dstSiteId.getBytes(), 0, dstSiteIdLen);

        switch (opId) {
        case QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
        case QNLConstants.REQ_GET_KP_BLOCK_INDEX:
            out.writeInt(this.payLoadSz);
            break;
        case QNLConstants.REQ_POST_ALLOC_KP_BLOCK:
            out.writeShort(uuid.length());
            out.writeBytes(uuid.getBytes(), 0, uuid.length());
            out.writeLong(keyBlockIndex);
            out.writeShort(respOpId);
            out.writeInt(this.payLoadSz);
            out.writeBytes(payBuf);
            break;
        case QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
            out.writeShort(uuid.length());
            out.writeBytes(uuid.getBytes(), 0, uuid.length());
            out.writeLong(keyBlockIndex);
            out.writeInt(this.payLoadSz);
            out.writeBytes(payBuf);
            break;
        case QNLConstants.REQ_POST_KP_BLOCK_INDEX:
            out.writeShort(uuid.length());
            out.writeBytes(uuid.getBytes(), 0, uuid.length());
            out.writeLong(keyBlockIndex);
            out.writeInt(this.payLoadSz);
            break;
        }
        calculateHMAC(out, this.frameSz);
        if (hmacSz > 0) {
            out.writeBytes(hmac);
            out.setInt(Integer.BYTES, hmacSz); // reset hmacSz
        }
        System.out.println("QNLRequest-encode:" + this + ",frameSz:" + this.frameSz + ",hmacSz:" + this.hmacSz);
    }

    public void setPayLoad(byte[] payLoad) {
        payBuf.writeBytes(payLoad);
        this.payLoadSz = payLoad.length;
    }

    public ByteBuf getPayLoad() {
        return payBuf;
    }

    public boolean decode(ByteBuf frame) {
        int m =  frame.readableBytes();
        short uuidLen;
        byte [] id;
        int savedFrameSz = 0;

        if (!this.payLoadMode) {
            frameSz = frame.readInt();
            savedFrameSz = frameSz;
            System.out.println("QNLRequest-decode:" + this + ",frameSz:" + this.frameSz);

            // Read hmacSz
            this.hmacSz = frame.readInt();
            frameSz -= Integer.BYTES;
            System.out.println("QNLRequest-decode:" + this + ",hmacSz:" + this.hmacSz);

            this.opId = frame.readShort();
            frameSz -= Short.BYTES;

            this.srcSiteIdLen = frame.readShort();
            frameSz -= Short.BYTES;
            byte [] src = new byte[srcSiteIdLen];
            frame.readBytes(src);
            this.srcSiteId = new String(src);
            frameSz -= this.srcSiteIdLen;

            this.dstSiteIdLen = frame.readShort();
            frameSz -= Short.BYTES;
            byte [] dst = new byte[dstSiteIdLen];
            frame.readBytes(dst);
            this.dstSiteId = new String(dst);
            frameSz -= this.dstSiteIdLen;

            switch (opId) {
            case QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
            case QNLConstants.REQ_GET_KP_BLOCK_INDEX:
                // Read payLoadSz
                this.payLoadSz = frame.readInt(); // it must be 0
                frameSz -= Integer.BYTES; // payLoadSz
                System.out.println("QNLRequest-decode:" + this + ",payLoadSz:" + this.payLoadSz);

                if (this.hmacSz > 0) {
                    // read hmac
                    this.hmac = new byte[this.hmacSz];
                    frame.readBytes(this.hmac);
                    frameSz -= this.hmacSz;
                }
                break;
            case QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
                uuidLen = frame.readShort();
                frameSz -= Short.BYTES;
                id = new byte[uuidLen];
                frame.readBytes(id);
                this.uuid = new String(id);
                frameSz -= uuidLen;

                this.keyBlockIndex = frame.readLong();
                frameSz -= Long.BYTES;

                this.payLoadSz = frame.readInt();
                frameSz -= Integer.BYTES; // payloadSZ
                System.out.println("QNLRequest-decode:" + this + ",payLoadSz:" + this.payLoadSz);
                frame.readBytes(payBuf, this.payLoadSz);
                frameSz -= this.payLoadSz;
                payLoadMode = !(this.payLoadSz == 0);

                if (this.hmacSz > 0) {
                    // read hmac
                    this.hmac = new byte[this.hmacSz];
                    frame.readBytes(this.hmac);
                    frameSz -= this.hmacSz;
                }
                break;
            case QNLConstants.REQ_POST_KP_BLOCK_INDEX:
                uuidLen = frame.readShort();
                frameSz -= Short.BYTES;
                id = new byte[uuidLen];
                frame.readBytes(id);
                this.uuid = new String(id);
                frameSz -= uuidLen;

                frameSz -= Long.BYTES;
                this.keyBlockIndex = frame.readLong();

                // Read payLoadSz
                this.payLoadSz = frame.readInt(); // it must be 0
                frameSz -= Integer.BYTES; // payLoadSz
                System.out.println("QNLRequest-decode:" + this + ",payLoadSz:" + this.payLoadSz);

                if (this.hmacSz > 0) {
                    // read hmac
                    this.hmac = new byte[this.hmacSz];
                    frame.readBytes(this.hmac);
                    frameSz -= this.hmacSz;
                }
                break;
            case QNLConstants.REQ_POST_ALLOC_KP_BLOCK:
                uuidLen = frame.readShort();
                frameSz -= Short.BYTES;
                id = new byte[uuidLen];
                frame.readBytes(id);
                this.uuid = new String(id);
                frameSz -= uuidLen;

                this.keyBlockIndex = frame.readLong();
                frameSz -= Long.BYTES;

                this.respOpId = frame.readShort();
                frameSz -= Short.BYTES;

                this.payLoadSz = frame.readInt();
                frameSz -= Integer.BYTES; // payloadSZ
                frame.readBytes(payBuf, this.payLoadSz);
                frameSz -= this.payLoadSz;
                payLoadMode = !(this.payLoadSz == 0);

                if (this.hmacSz > 0) {
                    // read hmac
                    this.hmac = new byte[this.hmacSz];
                    frame.readBytes(this.hmac);
                    frameSz -= this.hmacSz;
                }
                break;
            }
        } else {
            int k = frame.readableBytes();
            frameSz -= frame.readableBytes();
            frame.readBytes(payBuf, k);
            payLoadMode = !(frameSz == 0);
        }
        verifyHMAC(frame, savedFrameSz);
        return !payLoadMode;
    }

    public String opIdToString(short id) {
        switch (id) {
        case QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
            return "REQ_GET_ALLOC_KP_BLOCK";
        case QNLConstants.REQ_POST_ALLOC_KP_BLOCK:
            return "REQ_POST_ALLOC_KP_BLOCK";
        case QNLConstants.REQ_GET_KP_BLOCK_INDEX:
            return "REQ_GET_KP_BLOCK_INDEX";
        case QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
            return "REQ_POST_PEER_ALLOC_KP_BLOCK";
        case QNLConstants.REQ_POST_OTP_BLOCK_INDEX:
            return "REQ_POST_OTP_BLOCK_INDEX";
        case QNLConstants.REQ_POST_KP_BLOCK_INDEX:
            return "REQ_POST_KP_BLOCK_INDEX";
        default:
            return "";
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        fmt.format("QNLRequest:%n  opId: %s%n  srcSiteId: %s%n  dstSiteId: %s%n",
                   opIdToString(opId), srcSiteId, dstSiteId);
        switch (opId) {
        case QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
        case QNLConstants.REQ_GET_KP_BLOCK_INDEX:
            break;
        case QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
        case QNLConstants.REQ_POST_ALLOC_KP_BLOCK:
        case QNLConstants.REQ_POST_KP_BLOCK_INDEX:
            fmt.format("  KeyBlockIndex: %s%n", keyBlockIndex);
            fmt.format("  UUID: %s%n", uuid);
            break;
        }
        fmt.close();
        return sb.toString();
    }

    public void calculateHMAC(ByteBuf frame, int frameSize) {
        //if (frameSize == 0)
        //    return;
        //byte[] f = new byte[frameSize];
        //frame.getBytes(0, f, 0, frameSize);
        // calculate HMAC
    }

    public void verifyHMAC(ByteBuf frame, int frameSize) {
        //if (this.hmacSz == 0)
        //    return;
        //byte[] f = new byte[frameSz];
        //frame.getBytes(0, f, 0, frameSz);
        // calculate HMAC
    }

    static public void test() {
      QNLRequest getAllocKPBlock = new QNLRequest(1024);
      getAllocKPBlock.setSiteIds("A", "B");
      getAllocKPBlock.setOpId(QNLConstants.REQ_GET_ALLOC_KP_BLOCK);
      ByteBuf bb1 = Unpooled.buffer(1024 + 128);
      getAllocKPBlock.encode(bb1);

      QNLRequest getAllocKPBlock2 = new QNLRequest(1024);
      getAllocKPBlock2.decode(bb1);

      assert(getAllocKPBlock.getSrcSiteId().equalsIgnoreCase(getAllocKPBlock2.getSrcSiteId()));
      assert(getAllocKPBlock.getDstSiteId().equalsIgnoreCase(getAllocKPBlock2.getDstSiteId()));
      assert(getAllocKPBlock.getOpId() == getAllocKPBlock2.getOpId());
    }
}
