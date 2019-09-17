package com.uwaterloo.iqc.qnl.lsrp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class LSRPServerRouterInitializer extends ChannelInitializer<SocketChannel> {

  private static Logger LOGGER = LoggerFactory.getLogger(LSRPServerRouterInitializer.class);

  private LSRPRouter router;

  public LSRPServerRouterInitializer(LSRPRouter router) {
    this.router = router;
  }

  @Override
  public void initChannel(SocketChannel ch) {
    LOGGER.info("Accepts connection: " + ch);
    // inbound decoder
    ch.pipeline().addLast("loghandler", new LoggingHandler(LogLevel.INFO));
    ch.pipeline().addLast(new LSRPMessageDecoder());
    ch.pipeline().addLast(new LSRPIncomingClientHandler(this.router));
    ch.pipeline().addLast(new LSRPMessageEncoder());
  }
}
