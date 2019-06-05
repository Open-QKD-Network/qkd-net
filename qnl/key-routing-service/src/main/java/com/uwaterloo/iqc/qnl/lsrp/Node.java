package com.uwaterloo.iqc.qnl.lsrp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;

public class Node {
  private static Logger LOGGER = LoggerFactory.getLogger(Node.class);

  private String name;
  private String address;
  private int port;
  long floodingTimeStamp; // the timestamp of last received LSR message.
  private boolean adjacent;
  private boolean connected; // only applies for adjacent
  private Channel channel;

  public Node(String name, String address, int port) {
    this.name = name;
    this.address = address;
    this.port = port;
  }

  public String getName() {
    return this.name;
  }

  public String getAddress() {
    return this.address;
  }

  public int getPort() {
    return this.port;
  }

  public void setFloodingStamp(long stamp) {
    this.floodingTimeStamp = stamp;
    LOGGER.info("Received floodingtimestamp:" + this);
  }

  public void setAdjacent(boolean adj) {
    this.adjacent = adj;
  }

  public boolean isAdjacent() {
    return this.adjacent;
  }

  public void setConnected(boolean connected) {
    this.connected = connected;
    LOGGER.info("Connected to neighbour:" + this);
  }

  public boolean isConnected() {
    return this.connected;
  }

  public void setChannel(Channel ch) {
    this.channel = ch;
    LOGGER.info("Channel to neighbour:" + this);
  }

  public void sendLSRP(LSRPMessage msg) {
    if (this.channel == null) {
      LOGGER.error("sendLSRP without channel to neighbour:" + this);
    }
    LOGGER.info(this + ",sendLSRP:" + msg);
    this.channel.writeAndFlush(msg);
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("Node(").append(this.name).append(")");
    sb.append(",addr=").append(this.address);
    sb.append(",port=").append(this.port);
    sb.append(",adjacent=").append(this.adjacent);
    sb.append(",connected=").append(this.connected);
    sb.append(",channel=").append(this.channel);
    sb.append(",floodingtimestamp=").append(this.floodingTimeStamp);
    return sb.toString();
  }
}
