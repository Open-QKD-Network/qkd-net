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

    public static void main(String[] args) throws Exception {
        if (args.length == 0)
          ConfigArgs.qConfig = new QNLConfiguration(null);
        else
          ConfigArgs.qConfig = new QNLConfiguration(args[0]);

        final KeyTransferServer server = new KeyTransferServer(ConfigArgs.qConfig);
          ConfigArgs.qConfig.createOTPKeys(server);
        server.start();

        ConfigArgs.client = new GrpcClient();
        //client.getSiteDetails("localhost", 8000);
        //client.startNode("localhost", 8000, "localhost", 8001);
	
        //TODO: investigate auto-generating siteagent.json, and/or find a way to communicate requirement of having such a file

        LOGGER.info("starting site agent a");
        final ISiteAgentServer siteAgent = new ISiteAgentServer(ConfigArgs.qConfig.getSiteAgentConfig().url, ConfigArgs.qConfig.getSiteAgentConfig().port);
        try {
          siteAgent.start();
        } catch(IOException e) {
          LOGGER.error("Unable to start site agent", e);
        }
        LOGGER.info("finished starting site agent a");

        siteAgent.setMySiteAgentListener(new KeyRouter());

        LOGGER.info("Key router started, args.length:" + args.length);

        LSRPRouter lsrpRouter = new LSRPRouter(ConfigArgs.qConfig);
        lsrpRouter.start();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new KeyServerRouterInitializer(ConfigArgs.qConfig))
            .childOption(ChannelOption.AUTO_READ, false)
            .bind(ConfigArgs.qConfig.getConfig().getPort()).sync().channel().closeFuture().sync();
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

      Timer timer = new Timer();
      ConfigArgs.registered = false;

      final String localSite = ConfigArgs.qConfig.getConfig().getSiteId();

      for (Map.Entry<String, QKDLinkConfig> cfgEntry:
                    ConfigArgs.qConfig.getQKDLinkConfigMap().entrySet()) {
                    String remoteSite = cfgEntry.getKey();
                    QKDLinkConfig cfg = cfgEntry.getValue();

                    // Start timer thread if we are alice (our site id is lexicographically
                    // smaller)
                   if (localSite.compareTo(remoteSite) < 0) { // i.e. we are alice
                       while(!ConfigArgs.registered) {
                          timer.schedule(new WaitForConnect(cfg), 10000); // calling the TimerTask
                       }
                   }
                }
    }
}

