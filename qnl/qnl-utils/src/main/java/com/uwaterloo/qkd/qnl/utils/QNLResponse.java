package com.uwaterloo.qkd.qnl.utils;

import java.util.Formatter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class QNLResponse {
    private int frameSz;
    private ByteBuf payBuf;
    private String srcSiteId;
    private int srcSiteIdLen;
    private String dstSiteId;
    private int dstSiteIdLen;
    private short opId;
    private short respOpId;
    private long keyBlockIndex;
    private int kpBlockBytesSz;
    private String uuid;
    private boolean payLoadMode =  false;

    public QNLResponse(int kpBlockByteSz) {
        frameSz = 0;
        this.kpBlockBytesSz = kpBlockByteSz;
        payBuf = Unpooled.buffer(kpBlockByteSz + 128);
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
        frameSz += dstSiteIdLen + 2;
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
        case QNLConstants.RESP_GET_ALLOC_KP_BLOCK:
            frameSz += payBuf.readableBytes();
        case QNLConstants.RESP_POST_PEER_ALLOC_KP_BLOCK:
        case QNLConstants.RESP_POST_KP_BLOCK_INDEX:
        case QNLConstants.RESP_GET_KP_BLOCK_INDEX:
        case QNLConstants.RESP_POST_ALLOC_KP_BLOCK:
            break;
        }
        out.writeInt(frameSz);
        out.writeShort(opId);
        out.writeShort(srcSiteIdLen);
        out.writeBytes(srcSiteId.getBytes(), 0, srcSiteIdLen);
        out.writeShort(dstSiteIdLen);
        out.writeBytes(dstSiteId.getBytes(), 0, dstSiteIdLen);

        switch (opId) {
        case QNLConstants.RESP_POST_ALLOC_KP_BLOCK:
            out.writeShort(uuid.length());
            out.writeBytes(uuid.getBytes(), 0, uuid.length());
            out.writeLong(keyBlockIndex);
            out.writeShort(respOpId);
            break;
        case QNLConstants.RESP_GET_ALLOC_KP_BLOCK:
            out.writeShort(uuid.length());
            out.writeBytes(uuid.getBytes(), 0, uuid.length());
            out.writeBytes(payBuf);
            break;
        case QNLConstants.RESP_POST_PEER_ALLOC_KP_BLOCK:
        case QNLConstants.RESP_POST_KP_BLOCK_INDEX:
        case QNLConstants.RESP_GET_KP_BLOCK_INDEX:
            out.writeShort(uuid.length());
            out.writeBytes(uuid.getBytes(), 0, uuid.length());
            out.writeLong(keyBlockIndex);
            break;
        }
    }

    public void setPayLoad(byte[] payLoad) {
        payBuf.writeBytes(payLoad);
    }

    public ByteBuf getPayLoad() {
        return payBuf;
    }

    public boolean decode(ByteBuf frame) {
        short uuidLen;
        byte [] id;

        if (!this.payLoadMode) {
            frameSz = frame.readInt();
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
            case QNLConstants.RESP_POST_ALLOC_KP_BLOCK:
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
                break;
            case QNLConstants.RESP_POST_KP_BLOCK_INDEX:
            case QNLConstants.RESP_POST_PEER_ALLOC_KP_BLOCK:
            case QNLConstants.RESP_GET_KP_BLOCK_INDEX:
                uuidLen = frame.readShort();
                frameSz -= Short.BYTES;
                id = new byte[uuidLen];
                frame.readBytes(id);
                this.uuid = new String(id);
                frameSz -= uuidLen;
                this.keyBlockIndex = frame.readLong();
                frameSz -= Long.BYTES;
                break;
            case QNLConstants.RESP_GET_ALLOC_KP_BLOCK:
                uuidLen = frame.readShort();
                frameSz -= Short.BYTES;
                id = new byte[uuidLen];
                frame.readBytes(id);
                this.uuid = new String(id);
                frameSz -= uuidLen;

                frameSz -= frame.readableBytes();
                frame.readBytes(payBuf, frame.readableBytes());
                payLoadMode = !(frameSz== 0);
                break;
            }
        } else {
            int k = frame.readableBytes();
            frameSz -= frame.readableBytes();
            frame.readBytes(payBuf, k);
            payLoadMode = !(frameSz== 0);
        }
        return !payLoadMode;
    }

    public String opIdToString(short id) {
        switch (id) {
        case QNLConstants.RESP_GET_ALLOC_KP_BLOCK:
            return "RESP_GET_ALLOC_KP_BLOCK";
        case QNLConstants.RESP_POST_ALLOC_KP_BLOCK:
            return "RESP_POST_ALLOC_KP_BLOCK";
        case QNLConstants.RESP_GET_KP_BLOCK_INDEX:
            return "RESP_GET_KP_BLOCK_INDEX";
        case QNLConstants.RESP_POST_PEER_ALLOC_KP_BLOCK:
            return "RESP_POST_PEER_ALLOC_KP_BLOCK";
        case QNLConstants.RESP_POST_OTP_BLOCK_INDEX:
            return "RESP_POST_OTP_BLOCK_INDEX";
        case QNLConstants.RESP_POST_KP_BLOCK_INDEX:
            return "RESP_POST_KP_BLOCK_INDEX";
        default:
            return "";
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        fmt.format("QNLResponse:%n  opId: %s%n  srcSiteId: %s%n  dstSiteId: %s%n",
                   opIdToString(opId), srcSiteId, dstSiteId);
        switch (opId) {
        case QNLConstants.RESP_GET_ALLOC_KP_BLOCK:
            fmt.format("  UUID: %s%n", uuid);
            break;
        case QNLConstants.RESP_POST_ALLOC_KP_BLOCK:
            break;
        case QNLConstants.RESP_POST_KP_BLOCK_INDEX:
        case QNLConstants.RESP_POST_PEER_ALLOC_KP_BLOCK:
        case QNLConstants.RESP_GET_KP_BLOCK_INDEX:
            fmt.format("  KeyBlockIndex: %s%n", keyBlockIndex);
            fmt.format("  UUID: %s%n", uuid);
            break;
        }
        fmt.close();
        return sb.toString();
    }

}
