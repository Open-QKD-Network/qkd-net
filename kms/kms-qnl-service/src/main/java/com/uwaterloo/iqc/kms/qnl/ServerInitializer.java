package com.uwaterloo.iqc.kms.qnl;

import com.uwaterloo.qkd.qnl.utils.RequestDecoder;
import com.uwaterloo.qkd.qnl.utils.ResponseEncoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {

    private KMSQNLConfig kqCfg;

    public ServerInitializer(KMSQNLConfig cfg) {
        kqCfg = cfg;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new RequestDecoder(kqCfg.getKeyBlockSz()*kqCfg.getKeyByteSz()));
        p.addLast(new ResponseEncoder());
        p.addLast(new ServerHandler(kqCfg));
    }
}
