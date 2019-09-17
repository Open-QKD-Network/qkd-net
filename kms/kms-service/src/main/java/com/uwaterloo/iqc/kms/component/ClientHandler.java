package com.uwaterloo.iqc.kms.component;

import java.util.Vector;

import com.uwaterloo.qkd.qnl.utils.QNLConstants;
import com.uwaterloo.qkd.qnl.utils.QNLRequest;
import com.uwaterloo.qkd.qnl.utils.QNLResponse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;

public class ClientHandler
    extends SimpleChannelInboundHandler<ByteBuf> {
    private String srcId;
    private String dstId;
    private String blockId;
    private Vector<String> keys;
    private QNLResponse qResp;
    private int blockSz;
    private int byteSz;
    private int blockByteSz;

    public ClientHandler(String src, String dst, Vector<String> keys, int blockSz, int byteSz) {
        srcId = src;
        dstId = dst;
        this.keys =  keys;
        this.blockSz = blockSz;
        this.byteSz = byteSz;
        blockByteSz = blockSz*byteSz;
        qResp = new QNLResponse(blockByteSz);

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

        QNLRequest req = new QNLRequest(blockByteSz);
        req.setOpId(QNLConstants.REQ_GET_ALLOC_KP_BLOCK);
        req.setSiteIds(srcId, dstId);
        ctx.channel().writeAndFlush(req).addListener(
        new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    ctx.channel().read();
                } else {
                    future.channel().close();
                }
            }
        });
    }

    private void processResp(QNLResponse resp) {
        blockId = resp.getUUID();
        byte[] bin = new byte[blockByteSz];
        resp.getPayLoad().readBytes(bin);
        String hex = KeyUtils.byteArray2Hex(bin);
        byte [] hexBytes = hex.getBytes();
        int len = hexBytes.length / blockSz;
        for(int k = 0; k < blockSz; ++k) {
            keys.add(new String(hexBytes, k*len, len));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) throws Exception {
        if (cause instanceof ReadTimeoutException) {
            cause.printStackTrace();
            ctx.close();
            System.out.println("ClientHandler readtimeouts");
            throw new Exception("No QLL buffers available");
        } else {
            try {
                super.exceptionCaught(ctx, cause);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.out.println("ClientHandler exception:" + cause);
                cause.printStackTrace();
                ctx.close();
            }
        }
    }

    public String getBlockId() {
        return blockId;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.close();
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf)in;
        if (qResp.decode(frame)) {
            processResp(qResp);
        }
        ctx.channel().read();

    }
}
