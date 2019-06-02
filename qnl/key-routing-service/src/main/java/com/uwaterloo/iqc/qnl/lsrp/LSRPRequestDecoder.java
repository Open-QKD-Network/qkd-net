package com.uwaterloo.iqc.qnl.lsrp;

import io.netty.buffer.ByteBuffer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

public class LSRPMessageDecoder extends ReplayingDecoder<LSRPMessage> {

  private final Charset charset = Charset.forName("UTF-8");

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

    LSRPMessage msg = new LSRPMessage();
    int strLen = in.readInt();
    String  payload = in.readCharSequence(strLen, this.charset).toString();
    msg.setPayload(payload);
    out.add(msg);
  }
}
