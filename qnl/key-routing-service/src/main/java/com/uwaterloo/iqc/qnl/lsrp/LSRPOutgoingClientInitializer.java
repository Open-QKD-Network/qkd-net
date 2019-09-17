package com.uwaterloo.iqc.qnl.lsrp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class LSRPOutgoingClientInitializer extends ChannelInitializer<SocketChannel> {

    private static Logger LOGGER = LoggerFactory.getLogger(LSRPOutgoingClientInitializer.class);

    private LSRPRouter router;

    public LSRPOutgoingClientInitializer(LSRPRouter router) {
        LOGGER.info("LSRPOutgoingClientInitializer.new: " + this);
        this.router = router;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ch.pipeline().addLast("lsrp-outgong-response-decoder", new LSRPMessageDecoder());
        ch.pipeline().addLast("loghandler", new LoggingHandler(LogLevel.INFO));
        ch.pipeline().addLast("lsrp-outgoing-response", new LSRPOutgoingClientHandler(this.router));
        ch.pipeline().addLast("lsrp-outgoing-request-encoder", new LSRPMessageEncoder());
        LOGGER.info("LSRPOutgoingClientInitializer.initChannel:" + this + ",channel:" + ch);
    }
}
