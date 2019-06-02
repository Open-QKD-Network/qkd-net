package com.uwaterloo.iqc.qnl.lsrp;

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

  public LSRPIncomingClientHandler(LSRPRouter router) {
    this.router = router;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    //
  }
}
