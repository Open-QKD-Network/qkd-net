package com.uwaterloo.qkd.qnl.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ResponseEncoder extends MessageToByteEncoder<QNLResponse> {

    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, QNLResponse msg,
                                     boolean direct) {
        return ctx.alloc().heapBuffer(32896);
    }

    @Override
    public void encode(ChannelHandlerContext ctx, QNLResponse resp, ByteBuf out) throws Exception {
        resp.encode(out);
    }
}
