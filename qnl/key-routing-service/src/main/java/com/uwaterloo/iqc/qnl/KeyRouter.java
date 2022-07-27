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
import com.uwaterloo.iqc.qnl.qll.cqptoolkit.server.ISiteAgentServer.ISiteAgentServerListener;
import com.cqp.remote.*;

import java.io.IOException;
import java.io.ObjectInputFilter.Config;
import java.util.Map;
import java.util.Timer;

public class KeyRouter implements ISiteAgentServerListener {
    private static Logger LOGGER = LoggerFactory.getLogger(KeyRouter.class);
    private static QNLConfiguration qConfig;
    private Timer timer = new Timer();

    public static void main(String[] args) throws Exception {
        if (args.length == 0)
          qConfig = new QNLConfiguration(null);
        else
          qConfig = new QNLConfiguration(args[0]);

        final KeyTransferServer server = new KeyTransferServer( qConfig);
          qConfig.createOTPKeys(server);
        server.start();

        GrpcClient client = new GrpcClient();
        //client.getSiteDetails("localhost", 8000);
        //client.startNode("localhost", 8000, "localhost", 8001);
	
        //TODO: investigate auto-generating siteagent.json, and/or find a way to communicate requirement of having such a file

        LOGGER.info("starting site agent a");
        final ISiteAgentServer siteAgent = new ISiteAgentServer(qConfig.getSiteAgentConfig().url, qConfig.getSiteAgentConfig().port);
        try {
          siteAgent.start();
        } catch(IOException e) {
          LOGGER.error("Unable to start site agent", e);
        }
        LOGGER.info("finished starting site agent a");

        siteAgent.setMySiteAgentListener(new KeyRouter());

        LOGGER.info("Key router started, args.length:" + args.length);

        LSRPRouter lsrpRouter = new LSRPRouter( qConfig);
        lsrpRouter.start();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new KeyServerRouterInitializer( qConfig))
            .childOption(ChannelOption.AUTO_READ, false)
            .bind(qConfig.getConfig().getPort()).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void onDeviceRegistered(String deviceID) {
      //do not block this function
      //this function creates a timer object and thread which checks when peer dummy driver is registered on peer site agent
      //and when the above condition is met, call startNode on alice site.

      final String localSite = qConfig.getConfig().getSiteId();
      int count = 1;

      LOGGER.info("This is the id of the local site: " + localSite);
      LOGGER.info("and this is the deviceID: " + deviceID);

      for (Map.Entry<String, QKDLinkConfig> cfgEntry:
                     qConfig.getQKDLinkConfigMap().entrySet()) {
                    String remoteSite = cfgEntry.getKey();
                    LOGGER.info("Remote site " + count + "'s id is: " + remoteSite);
                    count++;
                    QKDLinkConfig cfg = cfgEntry.getValue();

                    // Start timer thread if we are alice (our site id is lexicographically
                    // smaller)
                   if (localSite.compareTo(remoteSite) < 0) { // i.e. we are alice
                      timer.schedule(new WaitForConnect(cfg, timer), 10000); // calling the TimerTask
                   }
                }
    }
}

