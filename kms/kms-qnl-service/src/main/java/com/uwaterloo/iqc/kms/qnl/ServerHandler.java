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
/**
 * Responsible for receiving a POST request of type `ALLOC_KP_BLOCK` 
 * expects: req to contain -> srcSiteID, destSiteID, UUID, payload=actual key
 * effects:
 * - finds poolLoc using using the kqCfg. Example poolLoc: ~/.qkd/kms/pools
 * - writes the <actual key> to <poolLoc> / <srcSiteID> / <destSiteID> / <UUID> (note: name of the file = <UUID>)
 * - sends a QNL Response and closes the channel:
 *  > opID = RESP_POST_ALLOC_KP_BLOCK
 *  > siteIDs, UUID, keyBlockIndex, RespOpId are all copied from the request.
 */
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

    /** [@rahul temp] Called when a new connection to the server is established. */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.channel().read();
    }

    /** [@rahul temp]: Called when a message is received from the server */
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
            } catch (Exception e) {
                logger.error("[rahul debug] Exception occurrecd in REQ_POST_ALLOC_KP_BLOCK when trying to write key to files");
            }
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


    // [@rahul doubt]: should we be overriding exception caught here?
}