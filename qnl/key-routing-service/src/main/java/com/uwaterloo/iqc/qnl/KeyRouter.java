package com.uwaterloo.iqc.qnl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import com.uwaterloo.iqc.qnl.lsrp.LSRPRouter;
import com.uwaterloo.iqc.qnl.qll.cqptoolkit.client.GrpcClient;
import com.uwaterloo.iqc.qnl.qll.cqptoolkit.server.ISiteAgentServer;
import com.uwaterloo.iqc.qnl.qll.cqptoolkit.server.KeyTransferServer;

import java.io.IOException;
import java.util.Map;
import java.util.TimerTask;
import java.util.Timer;

public class KeyRouter {
    private static Logger LOGGER = LoggerFactory.getLogger(KeyRouter.class);

    public class WaitForConnect extends TimerTask {
      public void run()
      {
        System.out.println("10 seconds have passed");
      }
    }

    public static void main(String[] args) throws Exception {
        final QNLConfiguration qConfig;
        if (args.length == 0)
          qConfig = new QNLConfiguration(null);
        else
          qConfig = new QNLConfiguration(args[0]);

        final KeyTransferServer server = new KeyTransferServer(qConfig);
        qConfig.createOTPKeys(server);
        server.start();

        final GrpcClient client = new GrpcClient();
        //client.getSiteDetails("localhost", 8000);
        //client.startNode("localhost", 8000, "localhost", 8001);

        final String localSite = qConfig.getConfig().getSiteId();

	new Thread(new Runnable() {
	    @Override
	    public void run() {
        boolean finished = false;
        boolean complete = false;
        Timer timer = new Timer();
        while(true) {
		  try {
            finished = true;
            client.getSiteDetails("172.31.20.54", 9002);
		  } catch (Exception e) {
            finished = false;
            complete = false;
            WaitForConnect w = new WaitForConnect();
            timer.schedule(w, 10000);
                }
            if(finished && !complete)
            {
              // Iterate over registered QKD links
              for (Map.Entry<String, QKDLinkConfig> cfgEntry:
                    qConfig.getQKDLinkConfigMap().entrySet()) {
                    String remoteSite = cfgEntry.getKey();
                    QKDLinkConfig cfg = cfgEntry.getValue();

                    // Start node if we are alice (our site id is lexiographically
                    // smaller)
                   if (localSite.compareTo(remoteSite) < 0) { // i.e. we are alice
                       LOGGER.info("Starting node " + localSite + " --> " + remoteSite);
                       client.startNode(cfg.localSiteAgentUrl, cfg.localQKDDeviceId,
                                    cfg.remoteSiteAgentUrl, cfg.remoteQKDDeviceId);
                   }
                }
                complete = true;
            }
        }
	   }}).start();
	
        //TODO: investigate auto-generating siteagent.json, and/or find a way to communicate requirement of having such a file

        LOGGER.info("starting site agent a");
        final ISiteAgentServer siteAgent = new ISiteAgentServer(qConfig.getSiteAgentConfig().url, qConfig.getSiteAgentConfig().port);
        try {
          siteAgent.start();
        } catch(IOException e) {
          LOGGER.error("Unable to start site agent", e);
        }
        LOGGER.info("finished starting site agent a");

        LOGGER.info("Key router started, args.length:" + args.length);

        LSRPRouter lsrpRouter = new LSRPRouter(qConfig);
        lsrpRouter.start();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new KeyServerRouterInitializer(qConfig))
            .childOption(ChannelOption.AUTO_READ, false)
            .bind(qConfig.getConfig().getPort()).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
