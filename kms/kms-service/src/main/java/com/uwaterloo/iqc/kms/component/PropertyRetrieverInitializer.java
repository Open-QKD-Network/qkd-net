package com.uwaterloo.iqc.kms.component;

import com.uwaterloo.qkd.qnl.utils.RequestEncoder;
import com.uwaterloo.qkd.qnl.utils.ResponseDecoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class PropertyRetrieverInitializer extends ChannelInitializer<SocketChannel> {
	
	private String ip;
	private int ipSz;
	private PropertyRetrieverHandler prh;
	
	public PropertyRetrieverInitializer(String ip, int ipSize) {
		this.ip = ip;
		this.ipSz = ipSize;
		prh = new PropertyRetrieverHandler(ip, ipSize);
	}
	
    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new RequestEncoder());
        p.addLast(new ResponseDecoder(ipSz));
        p.addLast("readTimeoutHandler", new ReadTimeoutHandler(40));
        p.addLast(prh);
    }
    
    public String getPeerSiteID() {
    	return prh.getPeerSiteID();
    }  
}
