package com.uwaterloo.iqc.qnl.lsrp;

import io.netty.buffer.ByteBuffer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

public class LSRPRequestDecoder extends ReplayingDecoder<LSRPRequest> {

  private final Charset charset = Charset.forName("UTF-8");

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

    LSRPRequest request = new LSRPRequest();
    int strLen = in.readInt();
    String  payload = in.readCharSequence(strLen, this.charset).toString();
    request.setPayload(payload);
    out.add(request);
  }
}
