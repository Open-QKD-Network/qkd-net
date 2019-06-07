package com.uwaterloo.iqc.qnl.lsrp;

import java.net.InetSocketAddress;
import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class LSRPIncomingClientHandler extends  ChannelInboundHandlerAdapter {

  private static Logger LOGGER = LoggerFactory.getLogger(LSRPIncomingClientHandler.class);

  private LSRPRouter router;

  private String remoteAddr;

  private int remotePort;

  public LSRPIncomingClientHandler(LSRPRouter router) {
    this.router = router;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    LOGGER.info("LSRPIncomingClientHandler.channelActive,channel:" + ctx.channel());
    this.remoteAddr = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
    this.remotePort = ((InetSocketAddress) ctx.channel().remoteAddress()).getPort();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    LOGGER.info("LSRPIncomingClientHandler.channelRead,channel:" +
      ctx.channel() + ",msg:" + (LSRPMessage) msg + ",from:" + remoteAddr + ":" + remotePort);
    this.router.onLSRP((LSRPMessage)msg, remoteAddr, remotePort);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, java.lang.Throwable cause) throws Exception {
    LOGGER.info("LSRPIncomingClientHandler.exceptionCaught,channel:" + ctx.channel() + ", cause:" + cause);
  }

  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    LOGGER.info("LSRPIncomingClientHandler.channelUnregistered,channel:" + ctx.channel());
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    LOGGER.info("LSRPIncomingClientHandler.channelInactive,channel:" + ctx.channel());
    // when other site closes the connection
    //this.router.onAdjacentNeighbourDisconnected(this.remoteAddr, this.remotePort);
  }
}
