package com.uwaterloo.iqc.qnl.lsrp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import com.uwaterloo.iqc.qnl.QNLConfiguration;

public class LSRPRouter {

    private static Logger LOGGER = LoggerFactory.getLogger(LSRPRouter.class);

    private QNLConfiguration qConfig;

    public LSRPRouter(QNLConfiguration qnlConfiguration) {
      this.qConfig = qConfig;
    }

    public void start()  throws Exception {
      LOGGER.info("LSRPRouter starts");
      EventLoopGroup bossGroup = new NioEventLoopGroup(1);
      EventLoopGroup workerGroup = new NioEventLoopGroup();
      try {
          // harcoded port 9395 for now
          ServerBootstrap b = new ServerBootstrap();
          b.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .handler(new LoggingHandler(LogLevel.INFO))
          .childHandler(new LSRPServerRouterInitializer(this))
          .childOption(ChannelOption.AUTO_READ, false)
          .bind(9395).sync().channel().closeFuture().sync();
      } finally {
          bossGroup.shutdownGracefully();
          workerGroup.shutdownGracefully();
      }
    }

    private void connectNeighbour(final Node neighbour) {
      try {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new LSRPOutgoingClientInitializer());

        ChannelFuture f = b.connect(neighbour.getAddress(), neighbour.getPort()).sync();
        //f.awaitUninterruptibly();
        f.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    LOGGER.info("LSRPOutgoingClient succeeds to connect to neighbour:" + neighbour.getName() + "/" + neighbour.getAddress());
                    // adds to hasht able?
                } else {
                    LOGGER.info("LSRPOutgoingClient fails to connect to neighbour:" + neighbour.getName() + "/" + neighbour.getAddress());
                }
            }
        });
      } catch (Exception e) {
        LOGGER.info("LSRPRouter.connectNeighbour exception:" + e);
      }
    }
}
