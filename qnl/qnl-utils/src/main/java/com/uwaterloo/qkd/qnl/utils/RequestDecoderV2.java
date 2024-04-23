package com.uwaterloo.qkd.qnl.utils;

import java.nio.charset.Charset;
import java.util.List;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ReplayingDecoder;

public class RequestDecoderV2 extends ReplayingDecoder<Void> {

    // private static Logger LOGGER = LoggerFactory.getLogger(RequestDecoderV2.class);
    public RequestDecoderV2(int bodySz) {}

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception {
        
        // [@rahul temp]: fix the magic numbers.
        QNLRequest qnlReq = new QNLRequest(1024 * 32);
        if (qnlReq.decode(in)) {
            // succesfully decoded.
            out.add(qnlReq);
        } else {
            System.err.println("Could not decode payload! " + in.toString(Charset.forName("UTF-8")));
        }
    }
}
