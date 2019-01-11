package com.uwaterloo.qkd.qnl.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import com.uwaterloo.qkd.qnl.utils.QNLRequest;

public class RequestEncoder extends MessageToByteEncoder<QNLRequest> {

    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, QNLRequest msg,
                                     boolean direct) {
        return ctx.alloc().heapBuffer(32896);
    }

    @Override
    public void encode(ChannelHandlerContext ctx, QNLRequest req, ByteBuf out) throws Exception {
        req.encode(out);
    }
}
