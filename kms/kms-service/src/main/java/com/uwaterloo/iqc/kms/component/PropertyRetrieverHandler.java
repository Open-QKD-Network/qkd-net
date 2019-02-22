package com.uwaterloo.iqc.kms.component;

import java.util.Vector;

import com.uwaterloo.qkd.qnl.utils.QNLConstants;
import com.uwaterloo.qkd.qnl.utils.QNLRequest;
import com.uwaterloo.qkd.qnl.utils.QNLResponse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;

public class PropertyRetrieverHandler extends SimpleChannelInboundHandler<ByteBuf> {
	
	private String ip;
	private int ipSz;
	private QNLResponse qResp;
	private String peerSiteID = null;
	
	public PropertyRetrieverHandler(String ip, int ipSz) {
		this.ip = ip;
		this.ipSz = ipSz;
		qResp = new QNLResponse(ipSz);
	}
	
   @Override
    public void channelActive(ChannelHandlerContext ctx) {
        QNLRequest req = new QNLRequest(ipSz);
        req.setOpId(QNLConstants.REQ_GET_PEER_SITE_ID);
        req.setPeerIP(ip);
        ctx.channel().writeAndFlush(req).addListener(
        new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    ctx.channel().read();
                } else {
                    future.channel().close();
                }
            }
        });
    }

    private void processResp(QNLResponse resp) {
    	peerSiteID = resp.getPeerSiteID();
    }
    
    public String getPeerSiteID() {
    	return peerSiteID;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) throws Exception {
        if (cause instanceof ReadTimeoutException) {
            cause.printStackTrace();
            ctx.close();
            throw new Exception("No QLL buffers available");
        } else {
            try {
                super.exceptionCaught(ctx, cause);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cause.printStackTrace();
                ctx.close();
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf)in;
        if (qResp.decode(frame)) {
            processResp(qResp);
        }
        ctx.channel().read();
    }
}
