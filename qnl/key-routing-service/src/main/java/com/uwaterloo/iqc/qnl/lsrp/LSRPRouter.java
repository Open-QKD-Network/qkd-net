package com.uwaterloo.iqc.qnl.lsrp;

import java.util.List;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import com.uwaterloo.iqc.qnl.QNLConfiguration;
import com.uwaterloo.iqc.qnl.RouteConfig;
import com.uwaterloo.iqc.qnl.QNLConfig;

public class LSRPRouter {

    private static Logger LOGGER = LoggerFactory.getLogger(LSRPRouter.class);

    private QNLConfiguration qConfig;

    private List<Node> adjacentNeighbours = new LinkedList<Node>();

    private Timer connectTimer = new Timer("LSRPRouterConnectTimer");

    private long floodingTimeStamp;

    private String mySiteId;

    public LSRPRouter(QNLConfiguration qnlConfiguration) {
      this.qConfig = qnlConfiguration;
      RouteConfig routeCfg = this.qConfig.getRouteConfig();
      QNLConfig qnlConfig = qConfig.getConfig();
      this.mySiteId = qnlConfig.getSiteId();

      for (String k : routeCfg.adjacent.keySet()) {
          String [] ipPort = routeCfg.adjacent.get(k).split(":");
          int port = 9395;
          if (ipPort.length == 2)
              port = Integer.valueOf(ipPort[1]);
          LOGGER.info("add adjacent neighbour: " + k + ",address:" + ipPort[0] + ",port:" + port);
          Node node = new Node(k, ipPort[0], port);
          node.setAdjacent(true);
          this.adjacentNeighbours.add(node);
      }
    }

    public void start() {
        new Thread() {
            public void run() {
              try {
                startListening();
              } catch (Exception e) {
                LOGGER.info("Fails to start LSRPRouter: " + e);
              }
            }
        }.start();
    }

    public void connectAdjacentNeighbours() {
      for (int index = 0; index < this.adjacentNeighbours.size(); index++) {
        connectNeighbourInTimer(this.adjacentNeighbours.get(index));
      }
    }

    public void connectNeighbourInTimer(Node neighbour) {
        ConnectTimerTask task = new ConnectTimerTask(neighbour);
        this.connectTimer.schedule(task, 30 * 1000l);
    }

    private void startListening()  throws Exception {
      LOGGER.info("LSRPRouter starts listening...");
      EventLoopGroup bossGroup = new NioEventLoopGroup(1);
      EventLoopGroup workerGroup = new NioEventLoopGroup();
      try {
          // harcoded port 9395 for now
          ServerBootstrap b = new ServerBootstrap();
          b.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .handler(new LoggingHandler(LogLevel.INFO))
          .childHandler(new LSRPServerRouterInitializer(this))
          .childOption(ChannelOption.AUTO_READ, false)
          .bind(9395).sync().channel().closeFuture().sync();
      } finally {
          bossGroup.shutdownGracefully();
          workerGroup.shutdownGracefully();
      }
    }

    private void connectNeighbour(final Node neighbour) {
      try {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new LSRPOutgoingClientInitializer());

        LOGGER.info("LSRPRouter tries to connect to neighbour:" +
            neighbour.getName() + ", address:" + neighbour.getAddress() +
            ", port:" + neighbour.getPort());
        ChannelFuture f = b.connect(neighbour.getAddress(), neighbour.getPort()).sync();
        //f.awaitUninterruptibly();
        f.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    LOGGER.info("LSRPRouter succeeds to connect to neighbour:" + neighbour.getName() + "/" + neighbour.getAddress());
                    neighbour.setConnected(true);
                    neighbour.setChannel(future.channel());
                    startFlooding();
                } else {
                    LOGGER.info("LSRPRouter fails to connect to neighbour:" + neighbour.getName() + "/" + neighbour.getAddress());
                }
            }
        });
      } catch (Exception e) {
        LOGGER.info("LSRPRouter.connectNeighbour exception:" + e);
        connectNeighbourInTimer(neighbour);
      }
    }

    private void startFlooding() {
        LSRPMessage msg = new LSRPMessage(true, "LINKSTATE", this.mySiteId);
        this.floodingTimeStamp = msg.getTimeStamp();
        for (int index = 0; index < this.adjacentNeighbours.size(); index++) {
            Node neighbour = this.adjacentNeighbours.get(index);
            if (neighbour.isConnected()) {
              msg.addNeighbour(neighbour);
            }
        }
        for (int index = 0; index < this.adjacentNeighbours.size(); index++) {
            Node neighbour = this.adjacentNeighbours.get(index);
            if (neighbour.isConnected()) {
              neighbour.sendLSRP(msg);
            }
        }
    }

    class ConnectTimerTask extends TimerTask {
      private Node neighbour;

      public ConnectTimerTask(Node neighbour) {
        this.neighbour = neighbour;
      }

      public void run() {
        new Thread() {
            public void run() {
              connectNeighbour(neighbour);
            }
        }.start();
      }
    }
}