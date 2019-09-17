package com.uwaterloo.iqc.qnl.lsrp;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class LSRPMessageEncoder extends MessageToByteEncoder<LSRPMessage> {
  private final Charset charset = Charset.forName("UTF-8");

  @Override
  protected void encode(ChannelHandlerContext ctx,
    LSRPMessage msg, ByteBuf out) throws Exception {

      String lsrp = msg.toString();
      out.writeInt(lsrp.length());
      out.writeCharSequence(lsrp, this.charset);
  }
}
