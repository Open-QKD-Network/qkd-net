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
    private String hmacKey;

    //private static Logger LOGGER = LoggerFactory.getLogger(QNLRequest.class);

    public QNLRequest(int kpBlockByteSz) {
        this.kpBlockBytesSz = kpBlockByteSz;
        payBuf = Unpooled.buffer(kpBlockByteSz + 128);
        frameSz = 0;
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
        frameSz += srcSiteIdLen + 2;
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
        this.hmacKey = key;
    }

    public void encode(ByteBuf out) {
        switch (opId) {
        case QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
        case QNLConstants.REQ_GET_KP_BLOCK_INDEX:
            break;
        case QNLConstants.REQ_POST_ALLOC_KP_BLOCK:
            frameSz += this.payLoadSz;
            break;
        case QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
            frameSz += this.payLoadSz;
            break;
        case QNLConstants.REQ_POST_KP_BLOCK_INDEX:
            break;
        }
        frameSz += Integer.BYTES; // for frameSz itself
        frameSz += Integer.BYTES; // for hmacSz
        frameSz += Integer.BYTES; // payloadSz out.writeInt(this.payLoadSz);
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
            out.writeBytes(this.hmac);
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

    // For the case where hamcKey is known before decoding,
    // decode() function can be used directly.

    // For case where hmacKey is not known before decoding,
    // we introduce the following two methods to decode meta data first
    // then get the hmacKey based on the OpId and src/dest siteId.
    // The usage should be as below

    // 1. QNLRequest request = new QNLRequest(1024);
    // 2. int frameSz = request.decodeNonMeta(frame);
    // 3. byte[] hmacKey = getHMACKey(request);
    // 4. request.setHMACKey(hnacKey);
    // 5. request.decodeNoMetaData(frame, frameSz);

    // Decode metadata
    // frameSz, macSz, opId, srtSiteId, dstSiteId
    public int decodeMetaData(ByteBuf frame) {
        int savedFrameSz = 0;
        frameSz = frame.readInt();
        savedFrameSz = frameSz;
        frameSz -= Integer.BYTES;
        System.out.println("QNLRequest-decode:" + this + ",frameSz:" + savedFrameSz);

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

        return savedFrameSz;
    }

    public boolean decodeNonMetaData(ByteBuf frame, int savedFrameSz) {
        short uuidLen;
        byte [] id;

        switch (this.opId) {
        case QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
        case QNLConstants.REQ_GET_KP_BLOCK_INDEX:
            // Read payLoadSz
            this.payLoadSz = frame.readInt(); // it must be 0
            frameSz -= Integer.BYTES; // payLoadSz
            System.out.println("QNLRequest-decode:" + this + ",payLoadSz:" + this.payLoadSz);
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
            System.out.println("QNLRequest-decode:" + this + ",payLoadSz:" + this.payLoadSz + ", payloadMode:" + payLoadMode);
            break;
      }
      if (this.hmacSz > 0) {
          // read hmac
          this.hmac = new byte[this.hmacSz];
          frame.readBytes(this.hmac);
          frameSz -= this.hmacSz;
      }
      return verifyHMAC(frame, savedFrameSz);
    }

    public boolean decode(ByteBuf frame) {
        int m =  frame.readableBytes();
        short uuidLen;
        byte [] id;
        int savedFrameSz = 0;

        if (!this.payLoadMode) {
            frameSz = frame.readInt();
            savedFrameSz = frameSz;
            frameSz -= Integer.BYTES;
            System.out.println("QNLRequest-decode:" + this + ",frameSz:" + savedFrameSz);

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

                System.out.println("QNLRequest-decode:" + this + ",payLoadSz:" + this.payLoadSz + ", payloadMode:" + payLoadMode);
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
        return verifyHMAC(frame, savedFrameSz);
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
        if (frameSize == 0)
            return;
        byte[] f = new byte[frameSize];
        // reset the hMacSz to 0
        frame.getBytes(0, f, 0, frameSize);

        try {
            Mac sha256Mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec sks = new SecretKeySpec(this.hmacKey.getBytes(), "HmacSHA256");
            sha256Mac.init(sks);
            this.hmac = sha256Mac.doFinal(f);
            this.hmacSz = this.hmac.length;
            System.out.println("calculateHMAC maclength:" + this.hmacSz);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException in calculateHMAC:" + e);
        } catch (InvalidKeyException e) {
            System.out.println("InvalidKeyException in calculateHMAC:" + e);
        }
    }

    public boolean verifyHMAC(ByteBuf frame, int frameSize) {
        //System.out.println("verifyHMAC frameSize:" + frameSize + ",hmacSz:" + this.hmacSz);
        if (this.hmacSz == 0)
            return false;
        frame.setInt(Integer.BYTES, 0); // reset hmacSz to 0 before calculateMac
        byte[] f = new byte[frameSize];
        frame.getBytes(0, f, 0, frameSize);
        // calculate HMAC

        try {
            Mac sha256Mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec sks = new SecretKeySpec(this.hmacKey.getBytes(), "HmacSHA256");
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
      QNLRequest getAllocKPBlock = new QNLRequest(1024);
      getAllocKPBlock.setSiteIds("A", "B");
      getAllocKPBlock.setOpId(QNLConstants.REQ_GET_ALLOC_KP_BLOCK);
      ByteBuf bb1 = Unpooled.buffer(1024 + 128);
      System.out.println("ENCODE:\n" + getAllocKPBlock);
      getAllocKPBlock.encode(bb1);

      QNLRequest getAllocKPBlock2 = new QNLRequest(1024);
      boolean r = getAllocKPBlock2.decode(bb1);
      assert(r);

      System.out.println("DECODE:\n" + getAllocKPBlock2);
      System.out.println("\n");
      assert(getAllocKPBlock.getSrcSiteId().equalsIgnoreCase(getAllocKPBlock2.getSrcSiteId()));
      assert(getAllocKPBlock.getDstSiteId().equalsIgnoreCase(getAllocKPBlock2.getDstSiteId()));
      assert(getAllocKPBlock.getOpId() == getAllocKPBlock2.getOpId());

      // getKPBlockIndex
      QNLRequest getKPBlockIndex = new QNLRequest(1024);
      getKPBlockIndex.setSiteIds("A", "B");
      getKPBlockIndex.setOpId(QNLConstants.REQ_GET_KP_BLOCK_INDEX);
      ByteBuf bb2 = Unpooled.buffer(1024 + 128);
      System.out.println("ENCODE:\n" + getKPBlockIndex);
      getKPBlockIndex.encode(bb2);

      QNLRequest getKPBlockIndex2 = new QNLRequest(1024);
      r = getKPBlockIndex2.decode(bb2);
      assert(r);

      System.out.println("DECODE:\n" + getKPBlockIndex2);
      System.out.println("\n");
      assert(getKPBlockIndex.getSrcSiteId().equalsIgnoreCase(getKPBlockIndex2.getSrcSiteId()));
      assert(getKPBlockIndex.getDstSiteId().equalsIgnoreCase(getKPBlockIndex2.getDstSiteId()));
      assert(getKPBlockIndex.getOpId() == getKPBlockIndex2.getOpId());

      // postAllocKPBlock
      QNLRequest postAllocKPBlock = new QNLRequest(1024);
      postAllocKPBlock.setSiteIds("A", "B");
      postAllocKPBlock.setOpId(QNLConstants.REQ_POST_ALLOC_KP_BLOCK);
      postAllocKPBlock.setKeyBlockIndex(1);
      postAllocKPBlock.setRespOpId(QNLConstants.RESP_GET_KP_BLOCK_INDEX);
      String uniqueID = UUID.randomUUID().toString();
      postAllocKPBlock.setUUID(uniqueID);
      byte[] binDest = null;
      binDest = new byte[64];
      postAllocKPBlock.setPayLoad(binDest);
      ByteBuf bb3 = Unpooled.buffer(1024 + 128);
      System.out.println("ENCODE:\n" + postAllocKPBlock);
      postAllocKPBlock.encode(bb3);

      QNLRequest postAllocKPBlock2 = new QNLRequest(1024);
      r = postAllocKPBlock2.decode(bb3);
      assert(r);

      System.out.println("DECODE:\n" + postAllocKPBlock2);
      System.out.println("\n");
      assert(postAllocKPBlock2.getSrcSiteId().equalsIgnoreCase(postAllocKPBlock.getSrcSiteId()));
      assert(postAllocKPBlock2.getDstSiteId().equalsIgnoreCase(postAllocKPBlock.getDstSiteId()));
      assert(postAllocKPBlock2.getOpId() == postAllocKPBlock.getOpId());
      assert(postAllocKPBlock2.getKeyBlockIndex() == 1);
      assert(postAllocKPBlock2.getRespOpId() == QNLConstants.RESP_GET_KP_BLOCK_INDEX);
      assert(postAllocKPBlock2.getUUID().equalsIgnoreCase(postAllocKPBlock.getUUID()));

      // postPeerAllocKPBlock
      QNLRequest postPeerAllocKPBlock = new QNLRequest(1024);
      postPeerAllocKPBlock.setSiteIds("A", "B");
      postPeerAllocKPBlock.setOpId(QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK);
      postPeerAllocKPBlock.setKeyBlockIndex(1);
      uniqueID = UUID.randomUUID().toString();
      postPeerAllocKPBlock.setUUID(uniqueID);
      binDest = new byte[64];
      postPeerAllocKPBlock.setPayLoad(binDest);
      ByteBuf bb4 = Unpooled.buffer(1024 + 128);
      System.out.println("ENCODE:\n" + postPeerAllocKPBlock);
      postPeerAllocKPBlock.encode(bb4);

      QNLRequest postPeerAllocKPBlock2 = new QNLRequest(1024);
      r= postPeerAllocKPBlock2.decode(bb4);
      assert(r);

      System.out.println("DECODE:\n" + postPeerAllocKPBlock2);
      System.out.println("\n");
      assert(postPeerAllocKPBlock2.getSrcSiteId().equalsIgnoreCase(postPeerAllocKPBlock.getSrcSiteId()));
      assert(postPeerAllocKPBlock2.getDstSiteId().equalsIgnoreCase(postPeerAllocKPBlock.getDstSiteId()));
      assert(postPeerAllocKPBlock2.getOpId() == postPeerAllocKPBlock.getOpId());
      assert(postPeerAllocKPBlock2.getKeyBlockIndex() == 1);
      assert(postPeerAllocKPBlock2.getUUID().equalsIgnoreCase(postPeerAllocKPBlock.getUUID()));

      System.out.println("Test passed\n");
    }

    static public void test2() {
      QNLRequest getAllocKPBlock = new QNLRequest(1024);
      getAllocKPBlock.setHMACKey("qawsedrftgyhujikolp");
      getAllocKPBlock.setSiteIds("A", "B");
      getAllocKPBlock.setOpId(QNLConstants.REQ_GET_ALLOC_KP_BLOCK);
      ByteBuf bb1 = Unpooled.buffer(1024 + 128);
      System.out.println("ENCODE:\n" + getAllocKPBlock);
      getAllocKPBlock.encode(bb1);

      QNLRequest getAllocKPBlock2 = new QNLRequest(1024);
      int fz = getAllocKPBlock2.decodeMetaData(bb1);
      getAllocKPBlock2.setHMACKey("qawsedrftgyhujikolp");
      boolean r = getAllocKPBlock2.decodeNonMetaData(bb1, fz);
      assert(r);

      System.out.println("DECODE:\n" + getAllocKPBlock2);
      System.out.println("\n");
      assert(getAllocKPBlock.getSrcSiteId().equalsIgnoreCase(getAllocKPBlock2.getSrcSiteId()));
      assert(getAllocKPBlock.getDstSiteId().equalsIgnoreCase(getAllocKPBlock2.getDstSiteId()));
      assert(getAllocKPBlock.getOpId() == getAllocKPBlock2.getOpId());

      // getKPBlockIndex
      QNLRequest getKPBlockIndex = new QNLRequest(1024);
      getKPBlockIndex.setHMACKey("qawsedrftgyhujikolp");
      getKPBlockIndex.setSiteIds("A", "B");
      getKPBlockIndex.setOpId(QNLConstants.REQ_GET_KP_BLOCK_INDEX);
      ByteBuf bb2 = Unpooled.buffer(1024 + 128);
      System.out.println("ENCODE:\n" + getKPBlockIndex);
      getKPBlockIndex.encode(bb2);

      QNLRequest getKPBlockIndex2 = new QNLRequest(1024);
      fz = getKPBlockIndex2.decodeMetaData(bb2);
      getKPBlockIndex2.setHMACKey("qawsedrftgyhujikolp");
      r = getKPBlockIndex2.decodeNonMetaData(bb2, fz);
      assert(r);

      System.out.println("DECODE:\n" + getKPBlockIndex2);
      System.out.println("\n");
      assert(getKPBlockIndex.getSrcSiteId().equalsIgnoreCase(getKPBlockIndex2.getSrcSiteId()));
      assert(getKPBlockIndex.getDstSiteId().equalsIgnoreCase(getKPBlockIndex2.getDstSiteId()));
      assert(getKPBlockIndex.getOpId() == getKPBlockIndex2.getOpId());

      // postAllocKPBlock
      QNLRequest postAllocKPBlock = new QNLRequest(1024);
      postAllocKPBlock.setHMACKey("qawsedrftgyhujikolp");
      postAllocKPBlock.setSiteIds("A", "B");
      postAllocKPBlock.setOpId(QNLConstants.REQ_POST_ALLOC_KP_BLOCK);
      postAllocKPBlock.setKeyBlockIndex(1);
      postAllocKPBlock.setRespOpId(QNLConstants.RESP_GET_KP_BLOCK_INDEX);
      String uniqueID = UUID.randomUUID().toString();
      postAllocKPBlock.setUUID(uniqueID);
      byte[] binDest = null;
      binDest = new byte[64];
      postAllocKPBlock.setPayLoad(binDest);
      ByteBuf bb3 = Unpooled.buffer(1024 + 128);
      System.out.println("ENCODE:\n" + postAllocKPBlock);
      postAllocKPBlock.encode(bb3);

      QNLRequest postAllocKPBlock2 = new QNLRequest(1024);
      fz = postAllocKPBlock2.decodeMetaData(bb3);
      postAllocKPBlock2.setHMACKey("qawsedrftgyhujikolp");
      r = postAllocKPBlock2.decodeNonMetaData(bb3, fz);
      assert(r);

      System.out.println("DECODE:\n" + postAllocKPBlock2);
      System.out.println("\n");
      assert(postAllocKPBlock2.getSrcSiteId().equalsIgnoreCase(postAllocKPBlock.getSrcSiteId()));
      assert(postAllocKPBlock2.getDstSiteId().equalsIgnoreCase(postAllocKPBlock.getDstSiteId()));
      assert(postAllocKPBlock2.getOpId() == postAllocKPBlock.getOpId());
      assert(postAllocKPBlock2.getKeyBlockIndex() == 1);
      assert(postAllocKPBlock2.getRespOpId() == QNLConstants.RESP_GET_KP_BLOCK_INDEX);
      assert(postAllocKPBlock2.getUUID().equalsIgnoreCase(postAllocKPBlock.getUUID()));

      // postPeerAllocKPBlock
      QNLRequest postPeerAllocKPBlock = new QNLRequest(1024);
      postPeerAllocKPBlock.setHMACKey("qawsedrftgyhujikolp");
      postPeerAllocKPBlock.setSiteIds("A", "B");
      postPeerAllocKPBlock.setOpId(QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK);
      postPeerAllocKPBlock.setKeyBlockIndex(1);
      uniqueID = UUID.randomUUID().toString();
      postPeerAllocKPBlock.setUUID(uniqueID);
      binDest = new byte[64];
      postPeerAllocKPBlock.setPayLoad(binDest);
      ByteBuf bb4 = Unpooled.buffer(1024 + 128);
      System.out.println("ENCODE:\n" + postPeerAllocKPBlock);
      postPeerAllocKPBlock.encode(bb4);

      QNLRequest postPeerAllocKPBlock2 = new QNLRequest(1024);
      fz = postPeerAllocKPBlock2.decodeMetaData(bb4);
      postPeerAllocKPBlock2.setHMACKey("qawsedrftgyhujikolp");
      r= postPeerAllocKPBlock2.decodeNonMetaData(bb4, fz);
      assert(r);

      System.out.println("DECODE:\n" + postPeerAllocKPBlock2);
      System.out.println("\n");
      assert(postPeerAllocKPBlock2.getSrcSiteId().equalsIgnoreCase(postPeerAllocKPBlock.getSrcSiteId()));
      assert(postPeerAllocKPBlock2.getDstSiteId().equalsIgnoreCase(postPeerAllocKPBlock.getDstSiteId()));
      assert(postPeerAllocKPBlock2.getOpId() == postPeerAllocKPBlock.getOpId());
      assert(postPeerAllocKPBlock2.getKeyBlockIndex() == 1);
      assert(postPeerAllocKPBlock2.getUUID().equalsIgnoreCase(postPeerAllocKPBlock.getUUID()));

      System.out.println("Test passed\n");
    }
}
