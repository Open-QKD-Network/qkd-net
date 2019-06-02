package com.uwaterloo.iqc.qnl;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.apache.commons.codec.binary.Hex;

import com.uwaterloo.iqc.qnl.qll.QLLReader;
import com.uwaterloo.qkd.qnl.utils.QNLConstants;
import com.uwaterloo.qkd.qnl.utils.QNLRequest;
import com.uwaterloo.qkd.qnl.utils.QNLResponse;
import com.uwaterloo.qkd.qnl.utils.QNLUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class KeyRouterFrontendHandler extends  ChannelInboundHandlerAdapter {

    static private Logger LOGGER = LoggerFactory.getLogger(KeyRouterFrontendHandler.class);

    private QNLConfiguration qConfig;
    private Channel inboundChannel;
    QNLRequest qReq;

    public KeyRouterFrontendHandler(QNLConfiguration qConfig) {
        this.qConfig = qConfig;
        qReq = new QNLRequest(1024*32);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        inboundChannel = ctx.channel();
        LOGGER.info(this + ".channelActive, inboud channel:" + inboundChannel);
        inboundChannel.read();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        ByteBuf frame = (ByteBuf)msg;
        if (qReq.decode(frame)) {
            processReq(ctx, qReq);
        }
        ctx.channel().read();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
    throws Exception {
        System.out.println("Read complete");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

    }

    /* * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void processReq(final ChannelHandlerContext ctx, QNLRequest qReq) {
        QNLConfig cfg = qConfig.getConfig();
        RouteConfig rConfig = qConfig.getRouteConfig();
        String destSiteId = qReq.getDstSiteId();
        String srcSiteId = qReq.getSrcSiteId();
        String localSiteId = cfg.getSiteId();
        String otherSiteId;
        String adjSiteId;
        QNLRequest req;
        QNLResponse resp;
        short opId = qReq.getOpId();
        QLLReader qllRdr;
        AtomicLong ref;
        long index;
        byte[] hex;
        byte[] binDest = null;
        int blockByteSz = cfg.getKeyBlockSz()*cfg.getKeyBytesSz();
        OTPKey otpKey;
        String uniqueID;

        LOGGER.info("KeyRouterFrontend/processQNLRequest,localSiteId:" + localSiteId + ", QNLRequest:" + qReq);
        switch (opId) {
        case QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
            // Step 1: it should be rConfig.getRoutes(srcSiteId, destSiteId)
            // and puts routes into QNLRequest()
            // adjSiteId should be next hop on the path
            adjSiteId = rConfig.getAdjacentId(destSiteId);
            LOGGER.info("adjSiteId:" + adjSiteId);
            req = new QNLRequest(blockByteSz);
            if (localSiteId.compareToIgnoreCase(adjSiteId) < 0) {
                uniqueID = UUID.randomUUID().toString();
                qllRdr = qConfig.getQLLReader(adjSiteId);
                ref = new AtomicLong(0);
                qllRdr.getNextBlockIndex(cfg.getKeyBlockSz(), ref);
                req.setOpId(QNLConstants.REQ_POST_KP_BLOCK_INDEX);
                req.setKeyBlockIndex(ref.get());
                req.setUUID(uniqueID);
            } else {
                req.setOpId(QNLConstants.REQ_GET_KP_BLOCK_INDEX);
            }
            req.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
            LOGGER.info("REQ_GET_ALLOC_KP_BLOCK/generate new QNLRequest:" + req);
            retainConnectHandler(ctx, adjSiteId);

            ctx.fireChannelActive();
            ctx.fireChannelRead(req);
            break;
        case QNLConstants.REQ_POST_KP_BLOCK_INDEX:
            qllRdr = qConfig.getQLLReader(srcSiteId);
            index = qReq.getKeyBlockIndex();
            hex =  new byte[blockByteSz * 2];
            qllRdr.read(hex, cfg.getKeyBlockSz(), index);

            try {
                binDest = new Hex().decode(hex);
            } catch(Exception e) {}

            if (localSiteId.equals(destSiteId)) {
                req = new QNLRequest(blockByteSz);
                req.setOpId(QNLConstants.REQ_POST_ALLOC_KP_BLOCK);
                req.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
                req.setUUID(qReq.getUUID());
                req.setKeyBlockIndex(qReq.getKeyBlockIndex());
                req.setRespOpId(QNLConstants.RESP_POST_KP_BLOCK_INDEX);
                req.setPayLoad(binDest);

                LOGGER.info("REQ_POST_KP_BLOCK_INDEX/generate new QNLRequest:" + req);
                retainConnectHandler(ctx, QNLConfig.KMS);
                ctx.fireChannelActive();
                ctx.fireChannelRead(req);
            } else {
                adjSiteId = rConfig.getAdjacentId(destSiteId);
                otherSiteId = rConfig.getOtherAdjacentId(adjSiteId);
                retainConnectHandler(ctx, adjSiteId);

                req = new QNLRequest(blockByteSz);
                req.setOpId(QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK);
                req.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
                req.setKeyBlockIndex(qReq.getKeyBlockIndex());
                req.setUUID(qReq.getUUID());
                try {
                    otpKey = qConfig.getOTPKey(adjSiteId);
                    otpKey.otp(binDest);
                    req.setPayLoad(binDest);
                } catch(Exception e) {
                    e.printStackTrace(System.out);
                }

                LOGGER.info("REQ_POST_KP_BLOCK_INDEX/generate new QNLRequest:" + req);
                ctx.fireChannelActive();
                ctx.fireChannelRead(req);
            }
            break;
        case QNLConstants.REQ_GET_KP_BLOCK_INDEX:
            // Step 2:
            uniqueID = UUID.randomUUID().toString();
            qllRdr = qConfig.getQLLReader(srcSiteId);
            ref = new AtomicLong(0);
            hex =  new byte[cfg.getKeyBlockSz()*cfg.getKeyBytesSz()*2];
            qllRdr.read(hex, cfg.getKeyBlockSz(), ref);

            if (localSiteId.equals(destSiteId)) {
                req = new QNLRequest(blockByteSz);
                req.setOpId(QNLConstants.REQ_POST_ALLOC_KP_BLOCK);
                req.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
                req.setKeyBlockIndex(ref.get());
                req.setRespOpId(QNLConstants.RESP_GET_KP_BLOCK_INDEX);
                req.setUUID(uniqueID);
                try {
                    binDest = new Hex().decode(hex);
                } catch(Exception e) {}
                req.setPayLoad(binDest);

                LOGGER.info("REQ_GET_KP_BLOCK_INDEX/generate new QNLRequest:" + req);
                retainConnectHandler(ctx, QNLConfig.KMS);
                ctx.fireChannelActive();
                ctx.fireChannelRead(req);
            } else {
                // For example C ---> B ---> A
                // localSiteId is intermediate site B
                // adjSiteId should be next hop on the path
                adjSiteId = rConfig.getAdjacentId(destSiteId);
                LOGGER.info("adjSiteId(destSiteId=" + destSiteId + ")=" + adjSiteId);

                req = new QNLRequest(blockByteSz);
                req.setOpId(QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK);
                req.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
                req.setKeyBlockIndex(ref.get());
                req.setUUID(uniqueID);
                try {
                    // OTPKey should be key between B and next hop
                    // In this case next hop is A
                    // qll(C->B) xor otp(B->A)
                    otpKey = qConfig.getOTPKey(adjSiteId);
                    binDest = new Hex().decode(hex);
                    otpKey.otp(binDest);
                    req.setPayLoad(binDest);
                } catch(Exception e) {
                    e.printStackTrace(System.out);
                }
                LOGGER.info("REQ_GET_KP_BLOCK_INDEX/generate new QNLRequest:" + req);
                retainConnectHandler(ctx, adjSiteId);
                ctx.fireChannelActive();
                ctx.fireChannelRead(req);
            }
            break;

        case QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
            if (localSiteId.equals(destSiteId)) {
                // C ---> B ---> A, Here is A
                // adjSiteId should be previous hop B
                req = new QNLRequest(blockByteSz);
                req.setOpId(QNLConstants.REQ_POST_ALLOC_KP_BLOCK);
                req.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
                req.setUUID(qReq.getUUID());
                req.setKeyBlockIndex(qReq.getKeyBlockIndex());
                req.setRespOpId(QNLConstants.RESP_POST_PEER_ALLOC_KP_BLOCK);
                binDest = new byte[blockByteSz];
                qReq.getPayLoad().readBytes(binDest);
                adjSiteId = rConfig.getAdjacentId(srcSiteId);
                LOGGER.info("adjSiteId(srcSiteId=" + srcSiteId + ")=" + adjSiteId);
                try {
                    // qll(C->B) xor otp(B->A) xor otp(A->B) = qll(C->B)
                    otpKey = qConfig.getOTPKey(adjSiteId);
                    otpKey.otp(binDest);
                    req.setPayLoad(binDest);
                } catch(Exception e) {}

                LOGGER.info("REQ_POST_PEER_ALLOC_KP_BLOCK/generate new QNLRequest:" + req);
                retainConnectHandler(ctx, QNLConfig.KMS);
                ctx.fireChannelActive();
                ctx.fireChannelRead(req);
            } else {
                // It should be next hop if there is one for example
                // C ---> B ---> A ---> D, localSiteId is A
                // Non adjacent allocation request
                //Create REQ_POST_PEER_ALLOC_KP_BLOCK to propagate key blocks
                otherSiteId = rConfig.getOtherAdjacentId(rConfig.getAdjacentId(destSiteId));
                LOGGER.info("REQ_POST_PEER_ALLOC_KP_BLOCK/otherSiteId:" + otherSiteId);
                if (otherSiteId != null)
                    ctx.pipeline().remove(otherSiteId);
                //NEEDS FIXING
//        		req = new QNLRequest((short)132, 0);
//        		req.setOpId(QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK);
//                req.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
                ctx.fireChannelActive();
                ctx.fireChannelRead(qReq);       //just propagate
            }
            break;
        }
    }

    private void retainConnectHandler(ChannelHandlerContext ctx, String retained) {
        LOGGER.info("retainConnectHandler:" + retained);
        RouteConfig rCfg = qConfig.getRouteConfig();
        for (String k : rCfg.adjacent.keySet()) {
            if (!k.equals(retained))
                ctx.pipeline().remove(k);
        }
        if (!retained.equals(QNLConfig.KMS))
            ctx.pipeline().remove(QNLConfig.KMS);
    }
}
