package com.uwaterloo.iqc.qnl.lsrp;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.Channel;
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

    private static int LINK_DETECTION_TIMER_VALUE = 60;

    private QNLConfiguration qConfig;

    private Map<String, Node> adjacentNeighbours = new HashMap<String, Node>();

    private Map<String, Node> allNodes = new HashMap<String, Node>();

    private long floodingTimeStamp;

    private String mySiteId;

    private String myIPv4Address;

    private EventLoopGroup sharedEventLoopGroup;

    private DijkstraRunnable testRunnable;

    public LSRPRouter(QNLConfiguration qnlConfiguration) {
      this.qConfig = qnlConfiguration;
      RouteConfig routeCfg = this.qConfig.getRouteConfig();
      routeCfg.setLSRPRouter(this);
      QNLConfig qnlConfig = qConfig.getConfig();
      this.mySiteId = qnlConfig.getSiteId();
      this.myIPv4Address = LSRPRouter.getLocalIPv4Address();

      // Add myself to allNodes
      Node self = new Node(this.mySiteId, this.myIPv4Address, 9395);
      LOGGER.info("Add node:" + this.mySiteId);
      this.allNodes.put(this.mySiteId, self);

      for (String k : routeCfg.adjacent.keySet()) {
          String [] ipPort = routeCfg.adjacent.get(k).split(":");
          int port = 9395;
          if (ipPort.length == 2)
              port = Integer.valueOf(ipPort[1]);
          LOGGER.info("Add neighbour:" + k + ",address:" + ipPort[0] + ",port:" + port);
          Node node = new Node(k, ipPort[0], port);
          node.setAdjacent(true);
          this.adjacentNeighbours.put(k, node);
          LOGGER.info("Add node:" + k);
          this.allNodes.put(k, node);
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

    public String getNextHop(String destination) {
      Node destNode = this.allNodes.get(destination);
      if (destNode == null) {
        LOGGER.info("Can not get destination node:" + destination);
        return null;
      }
      this.resetShortestPath();
      LSRPRouter router = LSRPRouter.calculateShortestPathFromSource(
        this, this.allNodes.get(this.mySiteId));
      // The first one is always myself
      if (destNode.getShortestPath().size() == 1) {
        // myself and destination are adjacent
        return destination;
      } else if (destNode.getShortestPath().size() > 1) {
        return destNode.getShortestPath().get(1).getName();
      }
      LOGGER.info("Fails to get shortest path from " + this.mySiteId + " to " + destination);
      return null;
    }

    public void testShortestPath() {
      this.testRunnable = new DijkstraRunnable(this, this.allNodes.get(this.mySiteId));
      this.sharedEventLoopGroup.schedule(this.testRunnable, 45, TimeUnit.SECONDS);
      //this.sharedEventLoopGroup.scheduleWithFixedDelay(this.testRunnable,
      //    120, 60, TimeUnit.SECONDS);
    }

    public void printShortestPath() {
      for (Map.Entry<String, Node> entry : this.allNodes.entrySet()) {
        Node destination = entry.getValue();
        if (this.mySiteId.equalsIgnoreCase(destination.getName()))
          continue;
        if (destination.getShortestPath().size() == 1) {
          LOGGER.info("Shortest path from " + this.mySiteId + " to " +
            destination.getName() + " is " + destination.getName() +
            " distance " + destination.getDistance());
        } else if (destination.getShortestPath().size() > 1) {
          LOGGER.info("Shortest path from " + this.mySiteId + " to " +
            destination.getName() + " is " + destination.getShortestPath().get(1).getName() +
            " distance " + destination.getDistance());
        }
      }
    }

    public void connectAdjacentNeighbours() {
      for (Map.Entry<String, Node> entry : this.adjacentNeighbours.entrySet()) {
        connectNeighbourInEventLoop(entry.getValue());
      }
    }

    public void connectNeighbourInEventLoop(Node neighbour) {
      ConnectRunnable task = new ConnectRunnable(neighbour);
      this.sharedEventLoopGroup.schedule(task, 30, TimeUnit.SECONDS);
    }

    public void onLSRP(LSRPMessage msg, String remoteAddr, int remotePort) {
      LOGGER.info("onLSRP-" + msg.getOriginator() + "-" + msg.getTimeStamp() + " from:" + remoteAddr);
      Node o = null;
      boolean update = false;
      Node nn = this.adjacentNeighbours.get(msg.getOriginator());
      if (msg.getOriginator().equalsIgnoreCase(this.mySiteId)) {
        // get my own LSRP if there is a loop
        return;
      }
      if (nn != null) {
        if (msg.getTimeStamp() > nn.getFloodingTimeStamp()) {
          // new LSRP of existing node
          nn.setFloodingTimeStamp(msg.getTimeStamp());
          update = true;
        }
        o = nn;
      }
      Node an = this.allNodes.get(msg.getOriginator());
      if (an != null) {
        if (msg.getTimeStamp() > an.getFloodingTimeStamp()) {
          // new LSRP of existing node
          an.setFloodingTimeStamp(msg.getTimeStamp());
          update = true;
        }
        o = an;
      }

      if (o == null) {
        //insert an new node into allNodes
        o = new Node(msg.getOriginator(), msg.getAddress(), 9395);
        o.setFloodingTimeStamp(msg.getTimeStamp());
        LOGGER.info("Add node/originator:" + o.getName());
        this.allNodes.put(msg.getOriginator(), o);
        update = true;
      }

      if (update) {
        Map<Node, Integer> existingAdjacentNodes = o.getAdjacentNodes();
        LinkedList<Neighbour> oneighbours = msg.getNeighbours();

        // insert as necessary
        for (int oindex = 0; oindex < oneighbours.size(); oindex++) {
          Neighbour oneighbour = oneighbours.get(oindex);
          if (this.allNodes.get(oneighbour.name) == null) {
            Node node = new Node(oneighbour.name, oneighbour.addr, 9395);
            LOGGER.info("Add node:" + oneighbour.name);
            this.allNodes.put(node.getName(), node);
          }
          if (remoteAddr.equalsIgnoreCase(msg.getAddress()) &&
              // The above says that IP source of LSRP message is same as address if LSRP message
              this.mySiteId.equalsIgnoreCase(oneighbour.name) &&
              (this.adjacentNeighbours.get(o.getName()) == null)) {
            LOGGER.info("ReAdd neighbour:" + o.getName());
            this.adjacentNeighbours.put(o.getName(), o);
            if (!o.isConnected()) {
              // Make outgoing connection to neighbour, after connection is made, reflooding
              connectNeighbourInEventLoop(o);
            }
          }
        }

        // remove as necessary
        List<Node> toBeRemoved = new ArrayList<Node>();
        for (Map.Entry<Node, Integer> entry : existingAdjacentNodes.entrySet()) {
          Node existingNode = entry.getKey();
          boolean found = false;

          for (int oindex = 0; oindex < oneighbours.size(); oindex++) {
            Neighbour oneighbour = oneighbours.get(oindex);
            if (existingNode.getName().equalsIgnoreCase(oneighbour.name)) {
              found = true;
              break;
            }
          }
          if (!found) {
            LOGGER.info("Node " + o.getName() + " lose neighbour:" + existingNode.getName());
            LOGGER.info("Delete link " + o.getName() + "-" + existingNode.getName());
            toBeRemoved.add(existingNode);
          }
        } // for (map)

        if (toBeRemoved.size() > 0) {
          for (Node node : toBeRemoved) {
            o.removeDestination(node);
            node.removeDestination(o);
            if (node.neighbours() == 0) {
              // remove the exisitingNode
              LOGGER.info("Delete node:" + node.getName());
              this.allNodes.remove(node.getName());
            }
          }
        }

        // Update the node's neighbours
        o.resetAdjacentNodes();
        for (int oindex = 0; oindex < oneighbours.size(); oindex++) {
          Neighbour oneighbour = oneighbours.get(oindex);
          Node node = this.allNodes.get(oneighbour.name);
          if (node != null) {
            o.addDestination(node, oneighbour.weight);
            node.addDestination(o, oneighbour.weight);
            LOGGER.info("Add link " + o + "---->" + node + ",weight:" + oneighbour.weight);
          }
        }
      } // update

      // write toplogy to file
      writeNetworkToFile();
      // forward the msg to all neighbours except the one recevied from
      for (Map.Entry<String, Node> entry : this.adjacentNeighbours.entrySet()) {
        Node n = entry.getValue();
        if (remoteAddr.equalsIgnoreCase(n.getAddress()))
          continue;
        else if (n.getName().equalsIgnoreCase(msg.getOriginator()))
          // donot forward to the originator
          continue;
        else if (n.isConnected())
          n.sendLSRP(msg);
      }
    }

    // outgoing connection to neighbour
    public void onAdjacentNeighbourConnected(String remoteAddr, int remotePort, Channel ch) {
      LOGGER.info("LSRPRouter succeeds to connect to neighbour:" + remoteAddr + "/" + remotePort);
      for (Map.Entry<String, Node> entry : this.adjacentNeighbours.entrySet()) {
        Node n = entry.getValue();
        if (remoteAddr.equalsIgnoreCase(n.getAddress())) {
          n.setConnected(true);
          n.setChannel(ch);
          startFlooding();
          // start monitoring the link between myself and node
          LinkDetectionRunnable detection = new LinkDetectionRunnable(this, n.getName());
          this.sharedEventLoopGroup.schedule(detection, LINK_DETECTION_TIMER_VALUE, TimeUnit.SECONDS);
          break;
        }
      }
    }

    public void onAdjacentNeighbourDisconnected(String remoteAddr, int remotePort) {
      // remove the neighbour and flooding again
      LOGGER.info("LSRPRouter disconnect to neighbour:" + remoteAddr + "/" + remotePort);
      String siteId = null;
      boolean flooding = false;
      for (Map.Entry<String, Node> entry : this.adjacentNeighbours.entrySet()) {
        Node n = entry.getValue();
        if (remoteAddr.equalsIgnoreCase(n.getAddress())) {
          siteId = n.getName();
          flooding = true;
          break;
        }
      }
      if (siteId != null) {
        LOGGER.info("Delete neighbour:" + siteId);
        this.adjacentNeighbours.remove(siteId);
        LOGGER.info("Delete node:" + siteId);
        this.allNodes.remove(siteId);
        LOGGER.info("Delete link:" + this.mySiteId + "-" + siteId);
        Node myself = this.allNodes.get(this.mySiteId);
        Node neighbour = this.allNodes.get(siteId);
        if (myself != null) {
          myself.removeDestination(neighbour);
        }
        if (neighbour != null) {
          neighbour.resetAdjacentNodes();
        }
      }
      if (flooding) {
        startFlooding();
      }
    }

    public void checkLink(String neighbour) {
      // Check the link between myself and neighbour
      Node n = this.allNodes.get(neighbour);
      Node myself = this.allNodes.get(this.mySiteId);
      if (n == null) {
        return;
      }
      String keyLoc = this.qConfig.getConfig().getQNLSiteKeyLoc(neighbour);
      File f = new File(keyLoc);
      LOGGER.info("Link-check to " + neighbour + ", file " + keyLoc);
      if (f.exists()) {
        if (!n.isConnected()) {
          LOGGER.info("Link-restored between " + this.mySiteId + "-" + neighbour);
          // link is restored
          n.setDistance(1);
          n.setConnected(true);
          n.addDestination(myself, 1);
          myself.addDestination(n, 1);
          startFlooding();
        }
      } else {
        if (n.isConnected()) {
          LOGGER.info("Link-broken between " + this.mySiteId + "-" + neighbour);
          n.setDistance(Integer.MAX_VALUE);
          n.setConnected(false);
          n.removeDestination(myself);
          myself.removeDestination(n);
          startFlooding();
        }
      }
      // start monitoring the link between myself and neighbour
      LinkDetectionRunnable detection = new LinkDetectionRunnable(this, neighbour);
      this.sharedEventLoopGroup.schedule(detection, LINK_DETECTION_TIMER_VALUE, TimeUnit.SECONDS);
      // write toplogy to file
      writeNetworkToFile();
    }

    private void startListening()  throws Exception {
      LOGGER.info("LSRPRouter starts listening...");
      EventLoopGroup bossGroup = new NioEventLoopGroup(1);
      this.sharedEventLoopGroup = new NioEventLoopGroup(1);
      try {
        // harcoded port 9395 for now
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, sharedEventLoopGroup)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new LSRPServerRouterInitializer(this));
        ChannelFuture f = b.bind(9395);
        f.addListener(new ChannelFutureListener() {
          public void operationComplete(ChannelFuture future) {
            if (future.isSuccess()) {
              LOGGER.info("LSRPRouter succeeds to bind to 9395");
              connectAdjacentNeighbours();
            } else {
              LOGGER.info("LSRPRouter fails to bind 9395:" + future.cause());
            }
          }
        });
        f.channel().closeFuture().sync();
      } finally {
        bossGroup.shutdownGracefully();
        this.sharedEventLoopGroup.shutdownGracefully();
      }
    }

    private void connectNeighbour(final Node neighbour) {
      try {
        Bootstrap b = new Bootstrap();
        b.group(this.sharedEventLoopGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new LSRPOutgoingClientInitializer(this));

        LOGGER.info("LSRPRouter tries to connect to neighbour:" +
            neighbour.getName() + ", address:" + neighbour.getAddress() +
            ", port:" + neighbour.getPort());
        ChannelFuture f = b.connect(neighbour.getAddress(), neighbour.getPort());
        f.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    LOGGER.info("LSRPRouter succeeds to connect to " + neighbour.getAddress());
                } else {
                    LOGGER.info("LSRPRouter fails to connect to " + neighbour.getAddress() + ", cause:" + future.cause());
                    connectNeighbourInEventLoop(neighbour);
                }
            }
        });
      } catch (Exception e) {
        LOGGER.info("LSRPRouter.connectNeighbour exception:" + e);
        connectNeighbourInEventLoop(neighbour);
      }
    }

    private void startFlooding() {
      if (this.testRunnable == null) {
        testShortestPath();
      }
      LSRPMessage msg = new LSRPMessage(true, "LINKSTATE", this.mySiteId);
      this.floodingTimeStamp = msg.getTimeStamp();
      for (Map.Entry<String, Node> entry : this.adjacentNeighbours.entrySet()) {
        Node neighbour = entry.getValue();
        if (neighbour.isConnected()) {
          msg.addNeighbour(new Neighbour(neighbour.getName(), neighbour.getAddress(), 9395));
        }
      }
      for (Map.Entry<String, Node> entry : this.adjacentNeighbours.entrySet()) {
        Node neighbour = entry.getValue();
        if (neighbour.isConnected()) {
          neighbour.sendLSRP(msg);
        }
      }
      LOGGER.info("sendLSRP-" + this.floodingTimeStamp);
    }

    class ConnectRunnable implements Runnable {
      private Node neighbour;

      public ConnectRunnable(Node neighbour) {
        this.neighbour = neighbour;
      }

      @Override
      public void run() {
        connectNeighbour(neighbour);
      }
    }

    class DijkstraRunnable implements Runnable {
      private LSRPRouter router;
      private Node source;

      public DijkstraRunnable(LSRPRouter router, Node source) {
        this.router = router;
        this.source = source;
      }

      @Override
      public void run() {
        this.router.resetShortestPath();
        LSRPRouter.calculateShortestPathFromSource(this.router, this.source);
      }
    }

    class LinkDetectionRunnable implements Runnable {
      private String neighbour;
      private LSRPRouter router;

      public LinkDetectionRunnable(LSRPRouter router, String neighbour) {
        this.router = router;
        this.neighbour = neighbour;
      }

      @Override
      public void run() {
        this.router.checkLink(neighbour);
      }
    }

    private void resetShortestPath() {
      for (Map.Entry<String, Node> entry : this.allNodes.entrySet()) {
        resetShortestPath(entry.getValue());
      }
    }

    private void resetShortestPath(Node n) {
      if (n == null)
        return;
      n.resetShortestPath();
    }

    // Dijkstra Algorithm related
    // calculate shortest distance
    static LSRPRouter calculateShortestPathFromSource(LSRPRouter router, Node source) {
      LOGGER.info("++++CalculateShortestPath from " + source);
      source.setDistance(0);

      Set<Node> settledNodes = new HashSet<Node>();
      Set<Node> unsettledNodes = new HashSet<Node>();
      unsettledNodes.add(source);

      while (unsettledNodes.size() != 0) {
        Node currentNode = getLowestDistanceNode(unsettledNodes);
        LOGGER.info("===Evaluating node:" + currentNode + ", unsettled nodes:" + unsettledNodes);
        unsettledNodes.remove(currentNode);
        for (Entry<Node, Integer> adjacencyPair : currentNode.getAdjacentNodes().entrySet()) {
          Node adjacentNode = adjacencyPair.getKey();
          Integer edgeWeigh = adjacencyPair.getValue();

          if (!settledNodes.contains(adjacentNode)) {
            calculateMinimumDistance(adjacentNode, edgeWeigh, currentNode);
            LOGGER.info("   Adding unsettledNode:" + adjacentNode);
            unsettledNodes.add(adjacentNode);
          }
        }
        LOGGER.info("===Adding settled node:" + currentNode);
        settledNodes.add(currentNode);
      }
      LOGGER.info("----CalculateShortestPath from " + source);
      router.printShortestPath();
      return router;
    }

    // From mySiteId to sourceNode's adjacentNode via sourceNode
    // edgeWeight is the weight from sourceNode to sourceNode's adjacentNode
    private static void calculateMinimumDistance(Node adjacentNode, Integer edgeWeigh, Node sourceNode) {
        Integer sourceDistance = sourceNode.getDistance();
        if (sourceDistance + edgeWeigh < adjacentNode.getDistance()) {
            adjacentNode.setDistance(sourceDistance + edgeWeigh);
            LinkedList<Node> shortestPath = new LinkedList<Node>(sourceNode.getShortestPath());
            shortestPath.add(sourceNode);
            adjacentNode.setShortestPath(shortestPath);
            LOGGER.info("    SetShortestPath to " + adjacentNode.getName() +
              " via " + sourceNode + " weight " + edgeWeigh + " is: " +
              shortestPath);
        }
    }

    // Get the node with shortest distance from unsettled nodes
    private static Node getLowestDistanceNode(Set<Node> unsettledNodes) {
        Node lowestDistanceNode = null;
        int lowestDistance = Integer.MAX_VALUE;
        for (Node node : unsettledNodes) {
            int nodeDistance = node.getDistance();
            if (nodeDistance < lowestDistance) {
                lowestDistance = nodeDistance;
                lowestDistanceNode = node;
            }
        }
        return lowestDistanceNode;
    }

    public static String getLocalIPv4Address() {
      String address = null;
      try {
        Enumeration en = NetworkInterface.getNetworkInterfaces();
        while(en.hasMoreElements()) {
          NetworkInterface ni=(NetworkInterface) en.nextElement();
          if (ni.isLoopback())
            continue;
          Enumeration ee = ni.getInetAddresses();
          while (ee.hasMoreElements()) {
            InetAddress ia= (InetAddress) ee.nextElement();
            if (ia instanceof Inet4Address)
              address = ia.getHostAddress();
          }
        }
      } catch (Exception e) {
      }
      return address;
    }

    private void writeNetworkToFile() {
      StringBuilder sb = new StringBuilder();
      sb.append("========Nodes/Links========\n");
      for (Map.Entry<String, Node> entry : this.allNodes.entrySet()) {
        Node node = entry.getValue();
        sb.append(node.getName()).append("\n");
        Map<Node, Integer> adjacentNodes = node.getAdjacentNodes();
        for (Map.Entry<Node, Integer> entry2 : adjacentNodes.entrySet()) {
          Node neighbour = entry2.getKey();
          int distance = entry2.getValue();
          if (distance < Integer.MAX_VALUE) {
            sb.append("    ").append(node.getName()).append(" <----> ").append(neighbour.getName()).append(" = ").append(distance).append("\n");
          }
        }
      }
      sb.append("========Nodes/Links========\n");
      try {
        String lsrp = System.getProperty("user.home") + "/.qkd/lsrp.log";
        java.io.FileWriter fw = new java.io.FileWriter(lsrp);
        fw.write(sb.toString());
        fw.close();
      } catch (Exception e) {
        LOGGER.info("Write routes to file exception:" + e);
      }
    }
}
