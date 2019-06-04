package com.uwaterloo.iqc.qnl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import com.uwaterloo.iqc.qnl.lsrp.LSRPRouter;

public class KeyRouter {

    private static Logger LOGGER = LoggerFactory.getLogger(KeyRouter.class);

    public static void main(String[] args) throws Exception {
        QNLConfiguration qConfig;
        if (args.length == 0)
          qConfig = new QNLConfiguration(null);
        else
          qConfig = new QNLConfiguration(args[0]);

        LOGGER.info("Key router started, args.length:" + args.length);
        LSRPRouter lsrpRouter = new LSRPRouter(qConfig);
        lsrpRouter.start();
        lsrpRouter.connectAdjacentNeighbours();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new KeyServerRouterInitializer(qConfig))
            .childOption(ChannelOption.AUTO_READ, false)
            .bind(qConfig.getConfig().getPort()).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
