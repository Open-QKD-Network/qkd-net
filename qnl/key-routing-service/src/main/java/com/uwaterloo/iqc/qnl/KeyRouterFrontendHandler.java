package com.uwaterloo.iqc.qnl;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.uwaterloo.iqc.qnl.qll.QLLReader;
import com.uwaterloo.qkd.qnl.utils.QNLConstants;
import com.uwaterloo.qkd.qnl.utils.QNLRequest;
import com.uwaterloo.qkd.qnl.utils.QNLResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class KeyRouterFrontendHandler extends ChannelInboundHandlerAdapter {

  private static Logger LOGGER = LoggerFactory.getLogger(KeyRouterFrontendHandler.class);

  private QNLConfiguration qConfig;
  private Channel inboundChannel;
  QNLRequest qReq;

  public KeyRouterFrontendHandler(QNLConfiguration qConfig) {
    this.qConfig = qConfig;
    qReq = new QNLRequest(1024 * 32);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    inboundChannel = ctx.channel();
    LOGGER.info(this + ".channelActive, inboud channel:" + inboundChannel);
    inboundChannel.read();
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, Object msg) {
    ByteBuf frame = (ByteBuf) msg;
    if (qReq.decode(frame)) {
      processReq(ctx, qReq);
    } else {
      LOGGER.info("cannot decode payload!!!!!!!!" + frame.toString());
    }
    ctx.channel().read();
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    System.out.println("Read complete");
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {}

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
    String adjSiteId;
    QNLRequest req;
    short opId = qReq.getOpId();
    QLLReader qllRdr;
    byte[] binDest;
    int blockByteSz = cfg.getKeyBlockSz() * cfg.getKeyBytesSz();
    OTPKey otpKey;
    String uniqueID;

    LOGGER.info(
        "KeyRouterFrontend/processQNLRequest,localSiteId:" + localSiteId + ", QNLRequest:" + qReq);
    switch (opId) {
      case QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
        // Step 1: adjSiteId should be next hop on the path
        adjSiteId = rConfig.getAdjacentId(destSiteId);
        LOGGER.info("adjSiteId:" + adjSiteId);
        req = new QNLRequest(blockByteSz);
        if (localSiteId.compareToIgnoreCase(adjSiteId) < 0) {
          uniqueID = UUID.randomUUID().toString();
          qllRdr = qConfig.getQLLReader(adjSiteId);
          req.setOpId(QNLConstants.REQ_POST_KP_BLOCK_INDEX);
          req.setKeyIdentifier(qllRdr.getNextKeyId(blockByteSz));
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
        binDest = qllRdr.read(qReq.getKeyIdentifier());

        if (localSiteId.equals(destSiteId)) {
          req = new QNLRequest(blockByteSz);
          req.setOpId(QNLConstants.REQ_POST_ALLOC_KP_BLOCK);
          req.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
          req.setUUID(qReq.getUUID());
          req.setKeyIdentifier(qReq.getKeyIdentifier());
          req.setRespOpId(QNLConstants.RESP_POST_KP_BLOCK_INDEX);
          req.setPayLoad(binDest);

          LOGGER.info("REQ_POST_KP_BLOCK_INDEX/generate new QNLRequest:" + req);
          retainConnectHandler(ctx, QNLConfig.KMS);
          ctx.fireChannelActive();
          ctx.fireChannelRead(req);
        } else {
          adjSiteId = rConfig.getAdjacentId(destSiteId);
          retainConnectHandler(ctx, adjSiteId);

          req = new QNLRequest(blockByteSz);
          req.setOpId(QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK);
          req.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
          req.setKeyIdentifier(qReq.getKeyIdentifier());
          req.setUUID(qReq.getUUID());
          try {
            otpKey = qConfig.getOTPKey(adjSiteId);
            otpKey.otp(binDest);
            req.setPayLoad(binDest);
          } catch (Exception e) {
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
        binDest = new byte[blockByteSz];
        String keyId = qllRdr.readNextKey(binDest, blockByteSz);

        if (localSiteId.equals(destSiteId)) {
          req = new QNLRequest(blockByteSz);
          req.setOpId(QNLConstants.REQ_POST_ALLOC_KP_BLOCK);
          req.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
          req.setKeyIdentifier(keyId);
          req.setRespOpId(QNLConstants.RESP_GET_KP_BLOCK_INDEX);
          req.setUUID(uniqueID);
          req.setPayLoad(binDest);

          LOGGER.info("REQ_GET_KP_BLOCK_INDEX/generate new QNLRequest:" + req);
          retainConnectHandler(ctx, QNLConfig.KMS);
          ctx.fireChannelActive();
          ctx.fireChannelRead(req);
        } else {
          // For example C ---> B ---> A
          // localSiteId is intermediate site B
          // adjSiteId should be next hop on the path towards the destSiteId
          adjSiteId = rConfig.getAdjacentId(destSiteId);
          LOGGER.info("adjSiteId(destSiteId=" + destSiteId + ")=" + adjSiteId);

          req = new QNLRequest(blockByteSz);
          req.setOpId(QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK);
          req.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
          req.setKeyIdentifier(keyId);
          req.setUUID(uniqueID);
          // req.setRespOpId(QNLConstants.RESP_GET_KP_BLOCK_INDEX); // added by XL
          try {
            // OTPKey should be key between B and next hop
            // In this case next hop is A
            // qll(C->B) xor otp(B->A)
            otpKey = qConfig.getOTPKey(adjSiteId);
            otpKey.otp(binDest);
            req.setPayLoad(binDest);
          } catch (Exception e) {
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
          req.setKeyIdentifier(qReq.getKeyIdentifier());
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
          } catch (Exception e) {
            e.printStackTrace(System.out);
          }

          LOGGER.info("REQ_POST_PEER_ALLOC_KP_BLOCK/generate new QNLRequest to KMS:" + req);
          retainConnectHandler(ctx, QNLConfig.KMS);
          ctx.fireChannelActive();
          ctx.fireChannelRead(req);
        } else {
          // D ---> C ---> B ---> A, Here is B
          // still needs to pass along the Request, but payload needs to be re-OTPed
          // OTP decode the payload first
          binDest = new byte[blockByteSz];
          qReq.getPayLoad().readBytes(binDest);
          try {
            // OTPKey should be key between localSite and previous hop
            otpKey = qConfig.getOTPKey(rConfig.getAdjacentId(srcSiteId));
            otpKey.otp(binDest);
          } catch (Exception e) {
            LOGGER.error("cannot get OTP key to decode the REQ_POST_PEER_ALLOC_KP_BLOCK payload");
            e.printStackTrace(System.out);
          }

          // OTP encode the payload for next hop
          adjSiteId = rConfig.getAdjacentId(destSiteId);
          LOGGER.info(
              "REQ_POST_PEER_ALLOC_KP_BLOCK/adjSiteId(destSiteId=" + destSiteId + ")=" + adjSiteId);
          req = new QNLRequest(blockByteSz);
          req.setOpId(QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK);
          req.setSiteIds(qReq.getSrcSiteId(), qReq.getDstSiteId());
          req.setKeyIdentifier(qReq.getKeyIdentifier());
          req.setUUID(qReq.getUUID());
          try {
            otpKey = qConfig.getOTPKey(adjSiteId);
            otpKey.otp(binDest);
            req.setPayLoad(binDest);
          } catch (Exception e) {
            LOGGER.error(
                "cannot get OTP key to encode the outbound REQ_POST_PEER_ALLOC_KP_BLOCK payload");
            e.printStackTrace(System.out);
          }
          LOGGER.info(
              "REQ_POST_PEER_ALLOC_KP_BLOCK/generate new QNLRequest to Node "
                  + adjSiteId
                  + ": "
                  + req);
          retainConnectHandler(ctx, adjSiteId);
          ctx.fireChannelActive();
          ctx.fireChannelRead(req);
        }
        break;
    }
  }

  private void retainConnectHandler(ChannelHandlerContext ctx, String retained) {
    LOGGER.info("retainConnectHandler:" + retained);
    RouteConfig rCfg = qConfig.getRouteConfig();
    for (String k : rCfg.adjacent.keySet()) {
      if (!k.equals(retained)) ctx.pipeline().remove(k);
    }
    if (!retained.equals(QNLConfig.KMS)) ctx.pipeline().remove(QNLConfig.KMS);
  }
}
