package com.uwaterloo.iqc.kms.component;

import java.net.InetSocketAddress;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

@Configuration
public class QNLPropertyReader {
	
	public String read(String ip, int port, String querySiteIP,  int ipSz) throws Exception {
		String peerSiteId = null;		
		try {
		    peerSiteId = connect(ip, port, querySiteIP, ipSz);
		} catch(Exception e) {
			throw e;
		}		
		return peerSiteId;
	}

	private String connect(String ip, int port, String querySiteIP,  int ipSz) throws Exception {
		PropertyRetrieverInitializer pi = new PropertyRetrieverInitializer(querySiteIP, ipSz);
		EventLoopGroup group = new NioEventLoopGroup();
		try {
		    Bootstrap b = new Bootstrap();
		    b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 20000);
		    b.group(group)
		    .channel(NioSocketChannel.class)
		    .remoteAddress(new InetSocketAddress(ip, port))
		    .handler(pi);
		
		    Channel ch = b.connect(ip, port).sync().channel();
		    ch.closeFuture().sync();
		} catch(Exception e) {
			throw e;
		} finally {
		    try {
		        group.shutdownGracefully().sync();
		    } catch(Exception e) {
		        e.printStackTrace();
		        throw e;
		    }
		}
		return pi.getPeerSiteID();
	}
	
	@Bean
	public QNLPropertyReader propReader() {
		return new QNLPropertyReader();
	}
}
