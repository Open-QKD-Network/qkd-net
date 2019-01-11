package com.uwaterloo.iqc.qnl;

import com.uwaterloo.qkd.qnl.utils.QNLRequest;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class KeyRouterConnectHandler extends ChannelInboundHandlerAdapter {
    private final String remoteHost;
    private final int remotePort;

    private volatile Channel outboundChannel;
    private Channel inboundChannel;
    private QNLConfiguration qConfig;

    public KeyRouterConnectHandler(String remoteHost, int remotePort, QNLConfiguration qConfig) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.qConfig = qConfig;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        inboundChannel = ctx.channel();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        Bootstrap b = new Bootstrap();
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 50000);
        b.group(workerGroup)
        .channel(ctx.channel().getClass())
        .handler(new KeyClientRouterInitializer(inboundChannel, qConfig))
        .option(ChannelOption.AUTO_READ, false);
        ChannelFuture f = b.connect(remoteHost, remotePort);
        f.awaitUninterruptibly();
        outboundChannel = f.channel();
        f.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                } else {
                    inboundChannel.close();
                }
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        QNLRequest req = (QNLRequest)msg;
        short opid = req.getOpId();

        outboundChannel.writeAndFlush(req).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
 //                   ctx.channel().read();
                } else {
                    future.channel().close();
                }
            }
        });
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
    throws Exception {
        if (inboundChannel != null) {
            closeOnFlush(inboundChannel);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
