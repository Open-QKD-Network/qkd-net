package com.uwaterloo.iqc.qnl.lsrp;

import java.net.InetSocketAddress;
import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

// Handle LSRPResponse
public class LSRPOutgoingClientHandler extends ChannelInboundHandlerAdapter {

  private static Logger LOGGER = LoggerFactory.getLogger(LSRPOutgoingClientHandler.class);

  private LSRPRouter router;

  private String remoteAddr;

  private int remotePort;

  public LSRPOutgoingClientHandler(LSRPRouter router) {
    this.router = router;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    LOGGER.info("LSRPOutgoingClientHandler.channelActive, channel:" + ctx.channel());
    this.remoteAddr = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
    this.remotePort = ((InetSocketAddress) ctx.channel().remoteAddress()).getPort();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    LOGGER.info("LSRPOutgoingClientHandler.channelRead,channel:" + ctx.channel() + ",msg:" + (LSRPMessage) msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, java.lang.Throwable cause) throws Exception {
    LOGGER.info("LSRPOutgoingClientHandler.exceptionCaught,channel:" + ctx.channel() + ",cause:" + cause);
  }

  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    LOGGER.info("LSRPOutgoingClientHandler.channelUnregistered,channel:" + ctx.channel());
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    LOGGER.info("LSRPOutgoingClientHandler.channelInactive,channel:" + ctx.channel());
    //this.router.onAdjacentNeighbourDisconnected(this.remoteAddr, this.remotePort);
  }
}
