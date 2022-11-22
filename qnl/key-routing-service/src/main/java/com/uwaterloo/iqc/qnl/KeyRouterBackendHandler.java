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
        if (respQNL.decode(frame)) {
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
        QLLReader qllRdr;

        LOGGER.info("KeyRouterBackend/processResp:" + this + ",res:" + qResp);
        switch (opId) {
        case QNLConstants.RESP_POST_ALLOC_KP_BLOCK:
            QNLResponse adjResp = new QNLResponse(blockByteSz);
            // opId: RESP_POST_PEER_ALLOC_KP_BLOCK
            // C ---> B ---> A, A is localSiteId, send to B
            adjResp.setOpId(qResp.getRespOpId());
            adjResp.setSiteIds(qResp.getSrcSiteId(), qResp.getDstSiteId());
            adjResp.setKeyIdentifier(qResp.getKeyIdentifier());
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
            String keyId = qResp.getKeyIdentifier();
            try {
                qllRdr = qConfig.getQLLReader(adjSiteId);
                byte[] bin = qllRdr.read(keyId);

                resp = new QNLResponse(blockByteSz);
                resp.setOpId(QNLConstants.RESP_GET_ALLOC_KP_BLOCK);
                resp.setPayLoad(bin);
                resp.setUUID(qResp.getUUID());
                resp.setSiteIds(qResp.getSrcSiteId(), qResp.getDstSiteId());

            } catch(Exception e) {
                e.printStackTrace();
            }

            if (opId == QNLConstants.RESP_POST_KP_BLOCK_INDEX) {
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
            // Request: C ---> B ---> A
            // Response: C <--- B <--- A
            // localSite is B, adjSiteId should be next hop to C
            adjSiteId = rConfig.getAdjacentId(srcSiteId);

            LOGGER.info("RESP_POST_PEER_ALLOC_KP_BLOCK/adjSiteId:" + adjSiteId + ",localSiteId:" + localSiteId + ",srcSiteId:" + srcSiteId);
            if (adjSiteId.equalsIgnoreCase(srcSiteId)) {
                resp = new QNLResponse(blockByteSz);
                if (localSiteId.compareToIgnoreCase(srcSiteId) < 0) {
                    resp.setOpId(QNLConstants.RESP_GET_KP_BLOCK_INDEX);
                } else {
                    resp.setOpId(QNLConstants.RESP_POST_KP_BLOCK_INDEX);
                }
                resp.setSiteIds(qResp.getSrcSiteId(), qResp.getDstSiteId());
                resp.setKeyIdentifier(qResp.getKeyIdentifier());
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
