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
    private ByteBuf payBuf;
    private boolean payLoadMode = false;
    private String uuid;
    private String peerIP;
    private int peerIPLen;

    public QNLRequest(int kpBlockByteSz) {
        this.kpBlockBytesSz = kpBlockByteSz;
        payBuf = Unpooled.buffer(kpBlockByteSz + 128);
        frameSz = 0;
    }

    public void setOpId(short op) {
        frameSz += Short.BYTES;
        opId = op;
    }

    public short getOpId() {
        return opId;
    }
    
    public void setPeerIP(String ip) {
        peerIPLen = ip.length();
        frameSz += peerIPLen + 2;
        peerIP = ip;
    }
    
    public String getPeerIP() {
    	return peerIP;
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
            break;
        case QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
            frameSz += payBuf.readableBytes();
            break;
        case QNLConstants.REQ_POST_KP_BLOCK_INDEX:
            break;
        }
        
        switch (opId) {
        case QNLConstants.REQ_GET_PEER_SITE_ID:
            out.writeInt(frameSz);
            out.writeShort(opId);
            out.writeShort(peerIPLen);
            out.writeBytes(peerIP.getBytes(), 0, peerIPLen);
        	break;
        default:
            out.writeInt(frameSz);
            out.writeShort(opId);
            out.writeShort(srcSiteIdLen);
            out.writeBytes(srcSiteId.getBytes(), 0, srcSiteIdLen);
            out.writeShort(dstSiteIdLen);
            out.writeBytes(dstSiteId.getBytes(), 0, dstSiteIdLen);
        	break;
        }

        switch (opId) {
        case QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
        case QNLConstants.REQ_GET_KP_BLOCK_INDEX:
            break;
        case QNLConstants.REQ_POST_ALLOC_KP_BLOCK:
            out.writeShort(uuid.length());
            out.writeBytes(uuid.getBytes(), 0, uuid.length());
            out.writeLong(keyBlockIndex);
            out.writeShort(respOpId);
            out.writeBytes(payBuf);
            break;
        case QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
            out.writeShort(uuid.length());
            out.writeBytes(uuid.getBytes(), 0, uuid.length());
            out.writeLong(keyBlockIndex);
            out.writeBytes(payBuf);
            break;
        case QNLConstants.REQ_POST_KP_BLOCK_INDEX:
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
        int m =  frame.readableBytes();
        short uuidLen;
        byte [] id;

        if (!this.payLoadMode) {
            frameSz = frame.readInt();
            this.opId = frame.readShort();
            frameSz -= Short.BYTES;
            
            switch (opId) {
            case QNLConstants.REQ_GET_PEER_SITE_ID:
                this.peerIPLen = frame.readShort();
                frameSz -= Short.BYTES;
                byte [] ip = new byte[this.peerIPLen];
                frame.readBytes(ip);
                this.peerIP = new String(ip);
                frameSz -= this.peerIPLen;
            	break;
            default:
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
            	break;
            }
            
            switch (opId) {
            case QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
            case QNLConstants.REQ_GET_KP_BLOCK_INDEX:
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

                frameSz -= frame.readableBytes();
                frame.readBytes(payBuf, frame.readableBytes());
                payLoadMode = !(frameSz == 0);
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

                frameSz -= frame.readableBytes();
                frame.readBytes(payBuf, frame.readableBytes());
                payLoadMode = !(frameSz == 0);
                break;
            }
        } else {
            int k = frame.readableBytes();
            frameSz -= frame.readableBytes();
            frame.readBytes(payBuf, k);
            payLoadMode = !(frameSz == 0);
        }
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
        case QNLConstants.REQ_GET_PEER_SITE_ID:
            return "REQ_GET_PEER_SITE_ID";
        default:
            return "";
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        
        switch (opId) {
        case QNLConstants.REQ_GET_PEER_SITE_ID:
        	fmt.format("QNLRequest:%n  opId: %s%n  peerIP: %s%n",
        			opIdToString(opId), peerIP);
        	break;
        default:
        	fmt.format("QNLRequest:%n  opId: %s%n  srcSiteId: %s%n  dstSiteId: %s%n",
        			opIdToString(opId), srcSiteId, dstSiteId);
        	break;
        }
        
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
}

