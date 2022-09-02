package com.uwaterloo.qkd.qnl.utils;

import java.util.Formatter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class QNLResponse {
    private int frameSz;
    private ByteBuf payBuf;
    private String srcSiteId;
    private String dstSiteId;
    private short opId;
    private short respOpId;
    private String keyIdentifier;
    private int keyBytes;
    private String uuid;
    private boolean payLoadMode =  false;

    public QNLResponse(int keyBytes) {
        frameSz = 0;
        this.keyBytes = keyBytes;
        payBuf = Unpooled.buffer(keyBytes + 128);
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

    public void setKeyIdentifier(String identifier) {
        frameSz += identifier.length() + 2;
        keyIdentifier = identifier;
    }

    public String getKeyIdentifier() {
        return keyIdentifier;
    }

    public void setSiteIds(String src, String dst) {
        frameSz += src.length() + 2;
        srcSiteId = src;

        frameSz += dst.length() + 2;
        dstSiteId = dst;
    }

    public String getSrcSiteId() {
        return srcSiteId;
    }

    public String getDstSiteId() {
        return dstSiteId;
    }

    private static String readString(ByteBuf frame) {
        short len = frame.readShort();
        byte [] src = new byte[len];
        frame.readBytes(src);
        return new String(src);
    }

    private static void writeString(ByteBuf frame, String data) {
        frame.writeShort(data.length());
        frame.writeBytes(data.getBytes());
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
        writeString(out, srcSiteId);
        writeString(out, dstSiteId);

        switch (opId) {
        case QNLConstants.RESP_POST_ALLOC_KP_BLOCK:
            writeString(out, uuid);
            writeString(out, keyIdentifier);
            out.writeShort(respOpId);
            break;
        case QNLConstants.RESP_GET_ALLOC_KP_BLOCK:
            writeString(out, uuid);
            out.writeBytes(payBuf);
            break;
        case QNLConstants.RESP_POST_PEER_ALLOC_KP_BLOCK:
        case QNLConstants.RESP_POST_KP_BLOCK_INDEX:
        case QNLConstants.RESP_GET_KP_BLOCK_INDEX:
            writeString(out, uuid);
            writeString(out, keyIdentifier);
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

        if (!this.payLoadMode) {
            frameSz = frame.readInt();
            this.opId = frame.readShort();
            frameSz -= Short.BYTES;

            this.srcSiteId = readString(frame);
            frameSz -= Short.BYTES + this.srcSiteId.length();

            this.dstSiteId = readString(frame);
            frameSz -= Short.BYTES + this.dstSiteId.length();

            switch (opId) {
                case QNLConstants.RESP_POST_ALLOC_KP_BLOCK:
                case QNLConstants.RESP_POST_KP_BLOCK_INDEX:
                case QNLConstants.RESP_POST_PEER_ALLOC_KP_BLOCK:
                case QNLConstants.RESP_GET_KP_BLOCK_INDEX:
                case QNLConstants.RESP_GET_ALLOC_KP_BLOCK:
                    this.uuid = readString(frame);
                    frameSz -= Short.BYTES + this.uuid.length();
                    if (opId != QNLConstants.RESP_GET_ALLOC_KP_BLOCK){
                        this.keyIdentifier = readString(frame);
                        frameSz -= Short.BYTES + this.keyIdentifier.length();
                    }
                    break;
                default:
                    break;
            }

            switch (opId) {
            case QNLConstants.RESP_POST_ALLOC_KP_BLOCK:
                this.respOpId = frame.readShort();
                frameSz -= Short.BYTES;
                break;
            case QNLConstants.RESP_GET_ALLOC_KP_BLOCK:
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
            fmt.format("  KeyBlockIndex: %s%n", keyIdentifier);
            fmt.format("  UUID: %s%n", uuid);
            break;
        }
        fmt.close();
        return sb.toString();
    }

}
