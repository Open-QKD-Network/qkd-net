package com.uwaterloo.iqc.qnl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.uwaterloo.qkd.qnl.utils.RequestDecoderV2;
import com.uwaterloo.qkd.qnl.utils.ResponseEncoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class KeyServerRouterInitializer extends ChannelInitializer<SocketChannel> {

    private static Logger LOGGER = LoggerFactory.getLogger(KeyServerRouterInitializer.class);

    private QNLConfiguration qConfig;

    public KeyServerRouterInitializer(QNLConfiguration qConfig) {
        this.qConfig = qConfig;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        QNLConfig cfg = qConfig.getConfig();
        int bodySz = cfg.getKeyBytesSz() * cfg.getKeyBlockSz();
        ch.pipeline().addLast(new RequestDecoderV2(bodySz));
        ch.pipeline().addLast("loghandler", new LoggingHandler(LogLevel.INFO));
        ch.pipeline().addLast("frontendhandler", new KeyRouterFrontendHandler(qConfig));
        
        // [@rahul temp]: map of "siteid": "ip:port"
        RouteConfig routeCfg = qConfig.getRouteConfig(); 

        // [@rahul temp]: port 9292.
        for (String k : routeCfg.adjacent.keySet()) {
            String [] ipPort = routeCfg.adjacent.get(k).split(":");
            int port = qConfig.getConfig().getPort(); // port of Key Routing Service
            if (ipPort.length == 2)
                port = Integer.valueOf(ipPort[1]);
            LOGGER.info("add " + k + ", KeyRouterConnectHandler to " + ipPort[0] + ":" + port);
            ch.pipeline().addLast(k, new KeyRouterConnectHandler(ipPort[0], port, qConfig));
        }
        
        // connect to KMS-QNL service.
        ch.pipeline().addLast("kms", new KeyRouterConnectHandler( cfg.getKmsIP(),
                              cfg.getKmsPort(), qConfig));
        ch.pipeline().addLast(new ResponseEncoder());

    }
}
