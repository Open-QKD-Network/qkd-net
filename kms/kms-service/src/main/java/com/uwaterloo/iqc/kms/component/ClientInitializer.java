package com.uwaterloo.iqc.kms.component;

import java.util.Vector;

import com.uwaterloo.qkd.qnl.utils.RequestEncoder;
import com.uwaterloo.qkd.qnl.utils.ResponseDecoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class ClientInitializer extends ChannelInitializer<SocketChannel> {

    private ClientHandler kmsClientHandler;
    private String srcId;
    private String dstId;
    private Vector<String> keysDst;
    private int blockSz;
    private int byteSz;
    private int blockByteSz;

    public ClientInitializer(String src, String dst, Vector<String> keysDst, int blockSz,
                             int byteSz) {
        srcId = src;
        dstId = dst;
        this.keysDst = keysDst;
        this.blockSz = blockSz;
        this.byteSz = byteSz;
        blockByteSz = blockSz * byteSz;

        kmsClientHandler = new ClientHandler(srcId, dstId, keysDst, blockSz, byteSz);
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new RequestEncoder());
        p.addLast(new ResponseDecoder(blockByteSz));
        p.addLast("readTimeoutHandler", new ReadTimeoutHandler(40));
        p.addLast(kmsClientHandler);
    }

    public String  getBlockId() {
        return kmsClientHandler.getBlockId();
    }
}
