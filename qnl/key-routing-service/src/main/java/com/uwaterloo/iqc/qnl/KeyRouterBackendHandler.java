package com.uwaterloo.iqc.qnl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.codec.binary.Hex;

import com.uwaterloo.iqc.qnl.qll.QLLReader;
import com.uwaterloo.qkd.qnl.utils.QNLConstants;
import com.uwaterloo.qkd.qnl.utils.QNLResponse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class KeyRouterBackendHandler extends ChannelInboundHandlerAdapter {

    private static Logger LOGGER = LoggerFactory.getLogger(KeyRouterBackendHandler.class);

    private final Channel inboundChannel;
    private QNLConfiguration qConfig;
    private QNLResponse respQNL;
    private int blockByteSz;
    private QNLConfig cfg;
    public KeyRouterBackendHandler(Channel inboundChannel, QNLConfiguration qConfig) {
        this.inboundChannel = inboundChannel;
        this.qConfig = qConfig;
        cfg = qConfig.getConfig();
        blockByteSz = cfg.getKeyBlockSz()*cfg.getKeyBytesSz();
        respQNL = new QNLResponse(blockByteSz);
        LOGGER.info("KeyRouterBackendHandler.new:" + this + ",inboundChannel:" + inboundChannel);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.channel().read();
     //   ctx.write(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        ByteBuf frame = (ByteBuf)msg;
        boolean r = false;
        int frameSz = 0;
        OTPKey otpKey;
        short opId;
        RouteConfig rConfig = qConfig.getRouteConfig();

        frameSz = respQNL.decodeMetaData(frame);
        opId = respQNL.getOpId();
        if (opId == QNLConstants.RESP_GET_KP_BLOCK_INDEX) {
            String destSiteId = respQNL.getDstSiteId();
            String srcSiteId = respQNL.getSrcSiteId();
            String adjacentId = rConfig.getAdjacentId(destSiteId);
            LOGGER.info("destId/srcId/adjId:" + destSiteId + "/" + srcSiteId + "/" + adjacentId);
            otpKey = qConfig.getOTPKey(adjacentId);
            LOGGER.info("receiving RESP_GET_KP_BLOCK_INDEX, setHMACKey with otpkey:" + adjacentId);
            respQNL.setHMACKey(otpKey.getKey());
        } else if (opId == QNLConstants.RESP_POST_PEER_ALLOC_KP_BLOCK) {
            // C: srcSiteId, A: destSiteId
            // Request  C ---> B ---> A
            // Respones A ---> B ---> C
            // localSite is B, A sends RESP_POST_PEER_ALLOC_KP_BLOCK to B
            String destSiteId = respQNL.getDstSiteId();
            String srcSiteId = respQNL.getSrcSiteId();
            String adjacentId = rConfig.getAdjacentId(destSiteId);
            LOGGER.info("destId/srcId/adjId:" + destSiteId + "/" + srcSiteId + "/" + adjacentId);
            otpKey = qConfig.getOTPKey(adjacentId);
            LOGGER.info("receiving RESP_POST_PEER_ALLOC_KP_BLOCK, setHMACKey with otpkey:" + adjacentId);
            respQNL.setHMACKey(otpKey.getKey());
        }
        r = respQNL.decodeNonMetaData(frame, frameSz);
        if (r) {
            processResp(ctx, respQNL);
        }
        ctx.channel().read();
    }

    private void processResp(final ChannelHandlerContext ctx, QNLResponse qResp) {
        short opId = qResp.getOpId();
        RouteConfig rConfig = qConfig.getRouteConfig();
        String adjSiteId;
        String localSiteId = qConfig.getConfig().getSiteId();
        String destSiteId = qResp.getDstSiteId();
        String srcSiteId = qResp.getSrcSiteId();
        QNLResponse resp = null;
        long index;
        QLLReader qllRdr;
        byte [] bin =  null;
        byte [] hex = null;

        LOGGER.info("KeyRouterBackend/processResp:" + this + ",res:" + qResp);
        switch (opId) {
        case QNLConstants.RESP_POST_ALLOC_KP_BLOCK:
            QNLResponse adjResp = new QNLResponse(blockByteSz);
            // opId: RESP_POST_PEER_ALLOC_KP_BLOCK
            // C: srcSiteId, A: destSiteId
            // Request:  C ---> B ---> A
            // Response: A ---> B ---> C
            // localsiteId is A. sends RESP_POST_PEER_ALLOC_KP_BLOCK to B
            adjResp.setOpId(qResp.getRespOpId());
            if (adjResp.getOpId() == QNLConstants.RESP_GET_KP_BLOCK_INDEX) {
                LOGGER.info("sending RESP_GET_KP_BLOCK_INDEX, setHMACKey with otpkey:" + srcSiteId);
                OTPKey otpKey = null;
                otpKey = qConfig.getOTPKey(srcSiteId);
                adjResp.setHMACKey(otpKey.getKey());
            } else if (adjResp.getOpId() == QNLConstants.RESP_POST_PEER_ALLOC_KP_BLOCK) {
                adjSiteId = rConfig.getAdjacentId(srcSiteId);
                LOGGER.info("sending RESP_POST_PEER_ALLOC_KP_BLOCK with otpkey:" + adjSiteId);
                OTPKey otpKey = qConfig.getOTPKey(adjSiteId);
                adjResp.setHMACKey(otpKey.getKey());
            }

            adjResp.setSiteIds(qResp.getSrcSiteId(), qResp.getDstSiteId());
            adjResp.setKeyBlockIndex(qResp.getKeyBlockIndex());
            adjResp.setUUID(qResp.getUUID());

            LOGGER.info("RESP_POST_ALLOC_KP_BLOCK/writeResp:" + adjResp);
            inboundChannel.writeAndFlush(adjResp).addListener(
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

        case QNLConstants.RESP_POST_KP_BLOCK_INDEX:
        case QNLConstants.RESP_GET_KP_BLOCK_INDEX:
            // RESP_GET_KP_BLOCK_INDEX, send RESP_GET_ALLOC_KP_BLOCK back to KMS service
            // Request C ---> B ---> A
            // Response C <---B <--- A, localSiteId is C
            // so adjSiteId is B
            adjSiteId = rConfig.getAdjacentId(destSiteId);
            LOGGER.info("adjSiteId:" + adjSiteId);
            index = qResp.getKeyBlockIndex();
            try {
                qllRdr = qConfig.getQLLReader(adjSiteId);
                hex =  new byte[blockByteSz*2];
                qllRdr.read(hex, cfg.getKeyBlockSz(), index);
                bin = new Hex().decode(hex);

                resp = new QNLResponse(blockByteSz);
                resp.setOpId(QNLConstants.RESP_GET_ALLOC_KP_BLOCK);
                resp.setPayLoad(bin);
                resp.setUUID(qResp.getUUID());
                resp.setSiteIds(qResp.getSrcSiteId(), qResp.getDstSiteId());

            } catch(Exception e) {
                e.printStackTrace();
            }

            if (opId == QNLConstants.REQ_POST_KP_BLOCK_INDEX) {
                LOGGER.info("RESP_POST_KP_BLOCK_INDEX/writeResp to inboundChannel:" + inboundChannel + ", resp:"  + resp);
            } else {
                LOGGER.info("RESP_GET_KP_BLOCK_INDEX/writeResp to inbound channel:" + inboundChannel + ", resp:" + resp);
            }
            inboundChannel.writeAndFlush(resp).addListener(
            new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        //Once response sent to client close the connection
                        future.channel().close();
                    } else {
                        future.channel().close();
                    }
                }
            });
            break;

        case QNLConstants.RESP_POST_PEER_ALLOC_KP_BLOCK:
            // C: srcSiteId, A: destSiteId
            // Request:  C ---> B ---> A
            // Response: A ---> B ---> C
            // localSite is B, adjSiteId should be next hop to C
            adjSiteId = rConfig.getAdjacentId(srcSiteId);

            LOGGER.info("RESP_POST_PEER_ALLOC_KP_BLOCK/adjSiteId:" + adjSiteId + ",localSiteId:" + localSiteId + ",srcSiteId:" + srcSiteId);
            if (adjSiteId.equalsIgnoreCase(srcSiteId)) {
                resp = new QNLResponse(blockByteSz);
                if (localSiteId.compareToIgnoreCase(srcSiteId) < 0) {
                    resp.setOpId(QNLConstants.RESP_GET_KP_BLOCK_INDEX);
                    LOGGER.info("sending RESP_GET_KP_BLOCK_INDEX with otpkey:" + adjSiteId);
                    OTPKey otpKey = qConfig.getOTPKey(adjSiteId);
                    resp.setHMACKey(otpKey.getKey());
                } else {
                    resp.setOpId(QNLConstants.RESP_POST_KP_BLOCK_INDEX);
                }
                resp.setSiteIds(qResp.getSrcSiteId(), qResp.getDstSiteId());
                resp.setKeyBlockIndex(qResp.getKeyBlockIndex());
                resp.setUUID(qResp.getUUID());
            } else {
                //propagate the peer alloc block response
                resp = qResp;
            }

            LOGGER.info("RESP_POST_PEER_ALLOC_KP_BLOCK/writeResp:" + resp);
            inboundChannel.writeAndFlush(resp).addListener(
            new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                    	future.channel().close();
                        //ctx.channel().read();
                    } else {
                        future.channel().close();
                    }
                }
            });
            break;
        };
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        KeyRouterFrontendHandler.closeOnFlush(inboundChannel);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        KeyRouterFrontendHandler.closeOnFlush(ctx.channel());
    }
}
