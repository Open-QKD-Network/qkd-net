package com.uwaterloo.qkd.qnl.utils;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class ResponseDecoder extends ByteToMessageDecoder {

	public ResponseDecoder(int bodySz) {
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception {
        out.add(in.readBytes(in.readableBytes()));
    }

    @Override
    public void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception {
    }
}
