package com.uwaterloo.iqc.qnl.lsrp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

// Handle LSRPResponse
public class LSRPOutgoingClientHandler extends ChannelInboundHandlerAdapter {

  private static Logger LOGGER = LoggerFactory.getLogger(LSRPOutgoingClientHandler.class);

  public LSRPOutgoingClientHandler() {
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {

  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    // handle the response
  }
}
