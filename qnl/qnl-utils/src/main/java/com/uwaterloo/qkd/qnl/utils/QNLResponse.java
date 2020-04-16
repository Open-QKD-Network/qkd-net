package com.uwaterloo.qkd.qnl.utils;

import java.util.UUID;
import java.util.Formatter;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class QNLResponse {
    private int frameSz;
    private int hmacSz;
    private int payLoadSz;
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
    private byte[] hmac;
    private byte[] hmacKey;

    public QNLResponse(int kpBlockByteSz) {
        frameSz = 0;
        this.kpBlockBytesSz = kpBlockByteSz;
        payBuf = Unpooled.buffer(kpBlockByteSz + 128);
        hmacSz = 0;
        payLoadSz = 0;
        setHMACKey("b6a57ad9-d41e-4443-adf0-61042710c521");
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

    public void setHMACKey(String key) {
        if (key == null || "".equals(key))
            return;
        this.hmacKey = key.getBytes();
    }

    public void setHMACKey(byte[] key) {
        if (key == null)
            return;
        this.hmacKey = key;
    }

    public void encode(ByteBuf out) {
        switch (opId) {
        case QNLConstants.RESP_GET_ALLOC_KP_BLOCK:
            this.payLoadSz = payBuf.readableBytes();
            frameSz += this.payLoadSz;
        case QNLConstants.RESP_POST_PEER_ALLOC_KP_BLOCK:
        case QNLConstants.RESP_POST_KP_BLOCK_INDEX:
        case QNLConstants.RESP_GET_KP_BLOCK_INDEX:
        case QNLConstants.RESP_POST_ALLOC_KP_BLOCK:
            break;
        }
        frameSz += Integer.BYTES; // for frameSz itself
        frameSz += Integer.BYTES; // for hmacSz
        frameSz += Integer.BYTES; // payloadSz
        out.writeInt(frameSz); // frameSz does not include calculaed MAC
        out.writeInt(hmacSz); // need to reset it after calculate HAMC
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
            out.writeInt(this.payLoadSz);
            break;
        case QNLConstants.RESP_GET_ALLOC_KP_BLOCK:
            out.writeShort(uuid.length());
            out.writeBytes(uuid.getBytes(), 0, uuid.length());
            out.writeInt(this.payLoadSz);
            out.writeBytes(payBuf);
            break;
        case QNLConstants.RESP_POST_PEER_ALLOC_KP_BLOCK:
        case QNLConstants.RESP_POST_KP_BLOCK_INDEX:
        case QNLConstants.RESP_GET_KP_BLOCK_INDEX:
            out.writeShort(uuid.length());
            out.writeBytes(uuid.getBytes(), 0, uuid.length());
            out.writeLong(keyBlockIndex);
            out.writeInt(this.payLoadSz);
            break;
        }
        calculateHMAC(out, this.frameSz);
        if (this.hmacSz > 0) {
            out.writeBytes(this.hmac);
            out.setInt(Integer.BYTES, this.hmacSz); // reset hmacSz
        }
    }

    public void setPayLoad(byte[] payLoad) {
        if (payLoad == null)
            return;
        payBuf.writeBytes(payLoad);
        this.payLoadSz = payLoad.length;
    }

    public ByteBuf getPayLoad() {
        return payBuf;
    }

    // For the case where hamcKey is known before decoding,
    // decode() function can be used directly.

    // For case where hmacKey is not known before decoding,
    // we introduce the following two methods to decode meta data first
    // then get the hmacKey based on the OpId and src/dest siteId.
    // The usage should be as below

    // 1. QNLResponse response = new QNLResponse(1024);
    // 2. int frameSz = response.decodeNonMeta(frame);
    // 3. byte[] hmacKey = getHMACKey(request);
    // 4. response.setHMACKey(hnacKey);
    // 5. response.decodeNoMetaData(frame, frameSz);

    // Decode metadata
    // frameSz, macSz, opId, srtSiteId, dstSiteId
    public int decodeMetaData(ByteBuf frame) {
        int savedFrameSz = 0;
        frameSz = frame.readInt();
        savedFrameSz = frameSz;
        frameSz -= Integer.BYTES;
        System.out.println("QNLResponse-decode:" + this + ",frameSz:" + savedFrameSz);

        // Read hmacSz
        this.hmacSz = frame.readInt();
        frameSz -= Integer.BYTES;
        System.out.println("QNLResponse-decode:" + this + ",hmacSz:" + this.hmacSz);

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

        return savedFrameSz;
    }

    public boolean decodeNonMetaData(ByteBuf frame, int savedFrameSz) {
        short uuidLen;
        byte [] id;

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

            // Read payLoadSz
            this.payLoadSz = frame.readInt(); // it must be 0
            frameSz -= Integer.BYTES; // payLoadSz
            System.out.println("QNLResponse-decode:" + this + ",payLoadSz:" + this.payLoadSz);
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

            // Read payLoadSz
            this.payLoadSz = frame.readInt(); // it must be 0
            frameSz -= Integer.BYTES; // payLoadSz
            System.out.println("QNLResponse-decode:" + this + ",payLoadSz:" + this.payLoadSz);
        break;
        case QNLConstants.RESP_GET_ALLOC_KP_BLOCK:
            uuidLen = frame.readShort();
            frameSz -= Short.BYTES;
            id = new byte[uuidLen];
            frame.readBytes(id);
            this.uuid = new String(id);
            frameSz -= uuidLen;

            this.payLoadSz = frame.readInt();
            frameSz -= Integer.BYTES; // payloadSz
            System.out.println("QNLResponse-decode:" + this + ",payLoadSz:" + this.payLoadSz);
            frame.readBytes(payBuf, this.payLoadSz);
            frameSz -= this.payLoadSz;
            payLoadMode = !(this.payLoadSz == 0);
        break;
        }

        if (this.hmacSz > 0) {
            // read hmac
            this.hmac = new byte[this.hmacSz];
            frame.readBytes(this.hmac);
            frameSz -= this.hmacSz;
            for (int i = 0; i < this.hmacSz; i++) {
                System.out.print(this.hmac[i] + " ");
            }
            System.out.println("\n");
        }
        return verifyHMAC(frame, savedFrameSz);
    }

    public boolean decode(ByteBuf frame) {
        short uuidLen;
        byte [] id;
        int savedFrameSz = 0;

        if (!this.payLoadMode) {
            frameSz = frame.readInt();
            savedFrameSz = frameSz;
            frameSz -= Integer.BYTES;
            System.out.println("QNLResponse-decode:" + this + ",frameSz:" + savedFrameSz);

            // Read hmacSz
            this.hmacSz = frame.readInt();
            frameSz -= Integer.BYTES;
            System.out.println("QNLResponse-decode:" + this + ",hmacSz:" + this.hmacSz);

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

                // Read payLoadSz
                this.payLoadSz = frame.readInt(); // it must be 0
                frameSz -= Integer.BYTES; // payLoadSz
                System.out.println("QNLResponse-decode:" + this + ",payLoadSz:" + this.payLoadSz);

                if (this.hmacSz > 0) {
                    // read hmac
                    this.hmac = new byte[this.hmacSz];
                    frame.readBytes(this.hmac);
                    frameSz -= this.hmacSz;
                }
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

                // Read payLoadSz
                this.payLoadSz = frame.readInt(); // it must be 0
                frameSz -= Integer.BYTES; // payLoadSz
                System.out.println("QNLResponse-decode:" + this + ",payLoadSz:" + this.payLoadSz);

                if (this.hmacSz > 0) {
                    // read hmac
                    this.hmac = new byte[this.hmacSz];
                    frame.readBytes(this.hmac);
                    frameSz -= this.hmacSz;
                }
                break;
            case QNLConstants.RESP_GET_ALLOC_KP_BLOCK:
                uuidLen = frame.readShort();
                frameSz -= Short.BYTES;
                id = new byte[uuidLen];
                frame.readBytes(id);
                this.uuid = new String(id);
                frameSz -= uuidLen;

                this.payLoadSz = frame.readInt();
                frameSz -= Integer.BYTES; // payloadSz
                System.out.println("QNLResponse-decode:" + this + ",payLoadSz:" + this.payLoadSz);
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
            payLoadMode = !(frameSz== 0);
        }
        return verifyHMAC(frame, savedFrameSz);
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

    public void calculateHMAC(ByteBuf frame, int frameSize) {
        if (frameSize == 0)
            return;
        byte[] f = new byte[frameSize];
        // reset the hMacSz to 0
        frame.getBytes(0, f, 0, frameSize);

        try {
            Mac sha256Mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec sks = new SecretKeySpec(this.hmacKey, "HmacSHA256");
            sha256Mac.init(sks);
            this.hmac = sha256Mac.doFinal(f);
            this.hmacSz = this.hmac.length;
            System.out.println("RESP-calculateHMAC maclength:" + this.hmacSz);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException in calculateHMAC:" + e);
        } catch (InvalidKeyException e) {
            System.out.println("InvalidKeyException in calculateHMAC:" + e);
        }
    }

    public boolean verifyHMAC(ByteBuf frame, int frameSize) {
        System.out.println("RESP-verifyHMAC frameSize:" + frameSize + ",hmacSz:" + this.hmacSz);
        if (this.hmacSz == 0)
            return false;
        frame.setInt(Integer.BYTES, 0); // reset hmacSz to 0 before calculateMac
        byte[] f = new byte[frameSize];
        frame.getBytes(0, f, 0, frameSize);
        System.out.println("DATA\n");
        for (int i = 0; i < f.length; i++) {
            System.out.print(f[i] + " ");
        }
        System.out.println("\n");
        System.out.println("MACKEY\n");
        for (int i = 0; i < this.hmacKey.length; i++) {
            System.out.print(this.hmacKey[i] + " ");
        }
        System.out.println("\n");
        // calculate HMAC

        try {
            Mac sha256Mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec sks = new SecretKeySpec(this.hmacKey, "HmacSHA256");
            sha256Mac.init(sks);
            byte[] calculatedMac = sha256Mac.doFinal(f);
            // compare the calculatedMac and Mac in msg itsef
            if (this.hmac.length != calculatedMac.length) {
                System.out.println("Mac length does not match, length in msg: "
                    + this.hmac.length + ", calculated length:" + calculatedMac.length);
                return false;
            }
            System.out.println("verifyHMAC maclength:" + this.hmac.length);
            for (int i = 0; i < this.hmac.length; i++) {
                System.out.println(i + "/" + this.hmac[i] + "/" + calculatedMac[i]);
                if (this.hmac[i] != calculatedMac[i])
                    return false;
            }
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException in verifyHMAC:" + e);
            return false;
        } catch (InvalidKeyException e) {
            System.out.println("InvalidKeyException in verifyHMAC:" + e);
            return false;
        }
        return true;
    }

    static public void test() {
        QNLResponse postAllocKPBlock = new QNLResponse(1024);
        postAllocKPBlock.setSiteIds("A", "B");
        postAllocKPBlock.setOpId(QNLConstants.RESP_POST_ALLOC_KP_BLOCK);
        String uniqueID = UUID.randomUUID().toString();
        postAllocKPBlock.setUUID(uniqueID);
        postAllocKPBlock.setKeyBlockIndex(8);
        ByteBuf bb1 = Unpooled.buffer(1024 + 128);
        System.out.println("ENCODE:\n" + postAllocKPBlock);
        postAllocKPBlock.encode(bb1);

        QNLResponse postAllocKPBlock2 = new QNLResponse(1024);
        boolean r = postAllocKPBlock2.decode(bb1);
        assert(r);
        System.out.println("DEOCDE:\n" + postAllocKPBlock2);
        System.out.println("\n");
        assert(postAllocKPBlock.getSrcSiteId().equalsIgnoreCase(postAllocKPBlock2.getSrcSiteId()));
        assert(postAllocKPBlock.getSrcSiteId().equalsIgnoreCase(postAllocKPBlock2.getSrcSiteId()));
        assert(postAllocKPBlock.getOpId() == postAllocKPBlock2.getOpId());
        assert(postAllocKPBlock.getUUID().equalsIgnoreCase(postAllocKPBlock2.getUUID()));
        assert(postAllocKPBlock.getKeyBlockIndex() == postAllocKPBlock2.getKeyBlockIndex());

        // getKPBLockIndex
        QNLResponse getKPBlockIndex = new QNLResponse(1024);
        getKPBlockIndex.setSiteIds("A", "B");
        getKPBlockIndex.setOpId(QNLConstants.RESP_GET_KP_BLOCK_INDEX);
        uniqueID = UUID.randomUUID().toString();
        getKPBlockIndex.setUUID(uniqueID);
        getKPBlockIndex.setKeyBlockIndex(8);
        ByteBuf bb2 = Unpooled.buffer(1024 + 128);
        System.out.println("ENCODE:\n" + getKPBlockIndex);
        getKPBlockIndex.encode(bb2);

        QNLResponse getKPBlockIndex2 = new QNLResponse(1024);
        r = getKPBlockIndex2.decode(bb2);
        assert(r);
        System.out.println("DEOCDE:\n" + getKPBlockIndex2);
        System.out.println("\n");
        assert(getKPBlockIndex.getSrcSiteId().equalsIgnoreCase(getKPBlockIndex2.getSrcSiteId()));
        assert(getKPBlockIndex.getSrcSiteId().equalsIgnoreCase(getKPBlockIndex2.getSrcSiteId()));
        assert(getKPBlockIndex.getOpId() == getKPBlockIndex2.getOpId());
        assert(getKPBlockIndex.getUUID().equalsIgnoreCase(getKPBlockIndex2.getUUID()));
        assert(getKPBlockIndex.getKeyBlockIndex() == getKPBlockIndex2.getKeyBlockIndex());

        // getAllocKPBlock
        QNLResponse getAllocKPBlock = new QNLResponse(1024);
        getAllocKPBlock.setSiteIds("A", "B");
        getAllocKPBlock.setOpId(QNLConstants.RESP_GET_ALLOC_KP_BLOCK);
        uniqueID = UUID.randomUUID().toString();
        getAllocKPBlock.setUUID(uniqueID);
        byte[] binDest = null;
        binDest = new byte[64];
        getAllocKPBlock.setPayLoad(binDest);
        ByteBuf bb3 = Unpooled.buffer(1024 + 128);
        System.out.println("ENCODE:\n" + getAllocKPBlock);
        getAllocKPBlock.encode(bb3);

        QNLResponse getAllocKPBlock2 = new QNLResponse(1024);
        r = getAllocKPBlock2.decode(bb3);
        assert(r);
        System.out.println("DEOCDE:\n" + getAllocKPBlock2);
        System.out.println("\n");
        assert(getAllocKPBlock.getSrcSiteId().equalsIgnoreCase(getAllocKPBlock2.getSrcSiteId()));
        assert(getAllocKPBlock.getSrcSiteId().equalsIgnoreCase(getAllocKPBlock2.getSrcSiteId()));
        assert(getAllocKPBlock.getOpId() == getAllocKPBlock2.getOpId());
        assert(getAllocKPBlock.getUUID().equalsIgnoreCase(getAllocKPBlock2.getUUID()));
    }
}
