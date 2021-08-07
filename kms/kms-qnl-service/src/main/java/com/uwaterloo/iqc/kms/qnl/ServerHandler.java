package com.uwaterloo.iqc.kms.qnl;

import java.io.File;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;

import com.uwaterloo.qkd.qnl.utils.QNLConstants;
import com.uwaterloo.qkd.qnl.utils.QNLRequest;
import com.uwaterloo.qkd.qnl.utils.QNLResponse;
import com.uwaterloo.qkd.qnl.utils.QNLUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private QNLRequest qReq;
    private KMSQNLConfig kqCfg;
    private int blockByteSz;
    private int blockSz;
    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    public ServerHandler(KMSQNLConfig cfg) {
        kqCfg = cfg;
        blockByteSz = cfg.getKeyByteSz() * cfg.getKeyBlockSz();
        blockSz = cfg.getKeyBlockSz();
        qReq = new QNLRequest(blockByteSz);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.channel().read();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf)in;
        if (qReq.decode(frame)) {
            processReq(ctx, qReq);
        }
        ctx.channel().read();
    }

    private void processReq(final ChannelHandlerContext ctx, QNLRequest qReq) {
        QNLResponse resp;
        byte [] binDest;
        byte [] hexKeys;
        short opId = qReq.getOpId();
        String uuid, srcId, dstId;

        switch (opId) {
        case QNLConstants.REQ_POST_ALLOC_KP_BLOCK:
            resp = new QNLResponse(blockByteSz);
            resp.setOpId(QNLConstants.RESP_POST_ALLOC_KP_BLOCK);
            resp.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
            resp.setUUID(qReq.getUUID());
            resp.setKeyBlockIndex(qReq.getKeyBlockIndex());
            resp.setRespOpId(qReq.getRespOpId());
            binDest = new byte[blockByteSz];
            qReq.getPayLoad().readBytes(binDest);
            uuid = qReq.getUUID();
            srcId = qReq.getSrcSiteId();
            dstId = qReq.getDstSiteId();
            hexKeys = new Hex().encode(binDest);
            try {
                File f = new File(kqCfg.getLoc() + "/" + srcId + "/" + dstId);
                if (!f.exists())
                    FileUtils.forceMkdir(f);
                logger.info("ServerHandler.writeKeys to keypool:" + f.getAbsolutePath() + "/" + uuid + ", blockSz:" + blockSz);
                QNLUtils.writeKeys(hexKeys, f.getAbsolutePath() + "/" + uuid, blockSz);
            } catch (Exception e) {}
            ctx.channel().writeAndFlush(resp).addListener(
            new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        future.channel().close();
                    } else {
                        future.channel().close();
                    }
                }
            });
            break;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
    throws Exception {
    }

}
