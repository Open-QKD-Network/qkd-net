package com.uwaterloo.iqc.qnl;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

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

        switch (opId) {
        case QNLConstants.REQ_GET_PEER_SITE_ID:
        	String peerIP = qReq.getPeerIP();
        	QNLResponse qResp = new QNLResponse(0);
        	qResp.setOpId(QNLConstants.RESP_GET_PEER_SITE_ID);
        	//Please see the comments under getSiteId(..) about 
        	//why this is not going to work for fetching siteids
        	//for ip address. Otherwie the framework for retrieving
        	//property from QNL by KMS works fine.
            qResp.setPeerSiteID(rConfig.getSiteId(peerIP));
            retainConnectHandler(ctx, "");
            ctx.fireChannelActive();
            inboundChannel.writeAndFlush(qResp).addListener(
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
        case QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
            adjSiteId = rConfig.getAdjacentId(destSiteId);
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
            otherSiteId = rConfig.getOtherAdjacentId(adjSiteId);
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

                ctx.fireChannelActive();
                ctx.fireChannelRead(req);
            }
            break;
        case QNLConstants.REQ_GET_KP_BLOCK_INDEX:
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

                retainConnectHandler(ctx, QNLConfig.KMS);
                ctx.fireChannelActive();
                ctx.fireChannelRead(req);
            } else {
                adjSiteId = rConfig.getAdjacentId(destSiteId);
                otherSiteId = rConfig.getOtherAdjacentId(adjSiteId);

                req = new QNLRequest(blockByteSz);
                req.setOpId(QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK);
                req.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
                req.setKeyBlockIndex(ref.get());
                req.setUUID(uniqueID);
                try {
                    otpKey = qConfig.getOTPKey(adjSiteId);
                    binDest = new Hex().decode(hex);
                    otpKey.otp(binDest);
                    req.setPayLoad(binDest);
                } catch(Exception e) {
                    e.printStackTrace(System.out);
                }
                retainConnectHandler(ctx, adjSiteId);
                ctx.fireChannelActive();
                ctx.fireChannelRead(req);
            }
            break;

        case QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
            if (localSiteId.equals(destSiteId)) {
                req = new QNLRequest(blockByteSz);
                req.setOpId(QNLConstants.REQ_POST_ALLOC_KP_BLOCK);
                req.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
                req.setUUID(qReq.getUUID());
                req.setKeyBlockIndex(qReq.getKeyBlockIndex());
                req.setRespOpId(QNLConstants.RESP_POST_PEER_ALLOC_KP_BLOCK);
                binDest = new byte[blockByteSz];
                qReq.getPayLoad().readBytes(binDest);
                adjSiteId = rConfig.getAdjacentId(srcSiteId);
                try {
                    otpKey = qConfig.getOTPKey(adjSiteId);
                    otpKey.otp(binDest);
                    req.setPayLoad(binDest);
                } catch(Exception e) {}

                retainConnectHandler(ctx, QNLConfig.KMS);
                ctx.fireChannelActive();
                ctx.fireChannelRead(req);
            } else {
                // Non adjacent allocation request
                //Create REQ_POST_PEER_ALLOC_KP_BLOCK to propagate key blocks
                otherSiteId = rConfig.getOtherAdjacentId(rConfig.getAdjacentId(destSiteId));
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
        RouteConfig rCfg = qConfig.getRouteConfig();
        for (String k : rCfg.adjacent.keySet()) {
            if (!k.equals(retained))
                ctx.pipeline().remove(k);
        }
        if (!retained.equals(QNLConfig.KMS))
            ctx.pipeline().remove(QNLConfig.KMS);
    }
}

