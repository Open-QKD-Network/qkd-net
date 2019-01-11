package com.uwaterloo.iqc.kms.qnl;


import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class KeyReceivingServer {

    public static KMSQNLConfig kqConfig;
    private static Logger LOGGER = LoggerFactory.getLogger(KeyReceivingServer.class);

    public static void main(String[] args) throws Exception {
        if (args.length == 0)
            loadConfig(null);
        else
            loadConfig(args[0]);

        LOGGER.info("Key receiving server started ...");
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new ServerInitializer(kqConfig))
            .childOption(ChannelOption.AUTO_READ, false)
            .bind(kqConfig.getPort()).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void loadConfig(String id) {
        String configBaseLoc;
        String configLoc;
        try {
            if (id != null) {
                configBaseLoc = System.getProperty("user.home") + "/.qkd" + id + "/";
                configLoc = configBaseLoc + "kms/qnl/config.yaml";
            } else {
                configBaseLoc = System.getProperty("user.home") + "/.qkd/";
                configLoc = configBaseLoc + "kms/qnl/config.yaml";
            }
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            kqConfig = mapper.readValue(new File(configLoc), KMSQNLConfig.class);
            kqConfig.setLoc(configBaseLoc);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }




}
