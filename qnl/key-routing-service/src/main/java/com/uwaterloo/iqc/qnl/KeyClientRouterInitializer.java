package com.uwaterloo.iqc.qnl;

import com.uwaterloo.qkd.qnl.utils.RequestEncoder;
import com.uwaterloo.qkd.qnl.utils.ResponseDecoder;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class KeyClientRouterInitializer extends ChannelInitializer<SocketChannel> {

    private QNLConfiguration qConfig;
    private Channel inboundChannel;

    public KeyClientRouterInitializer(Channel inbound, QNLConfiguration qConfig) {
        this.qConfig = qConfig;
        this.inboundChannel = inbound;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        QNLConfig cfg = qConfig.getConfig();
        int bodySz = cfg.getKeyBytesSz() * cfg.getKeyBlockSz();
        ch.pipeline().addLast(new ResponseDecoder(bodySz));
        ch.pipeline().addLast("loghandler", new LoggingHandler(LogLevel.INFO));
        ch.pipeline().addLast("backend", new KeyRouterBackendHandler(inboundChannel, qConfig));
        ch.pipeline().addLast(new RequestEncoder());
    }
}
