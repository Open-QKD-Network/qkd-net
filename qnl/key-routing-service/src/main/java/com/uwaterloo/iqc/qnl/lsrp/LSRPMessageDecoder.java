package com.uwaterloo.iqc.qnl.lsrp;

import java.util.List;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSRPMessageDecoder extends ReplayingDecoder<LSRPMessage> {
  private static Logger LOGGER = LoggerFactory.getLogger(LSRPMessageDecoder.class);

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
