package com.uwaterloo.iqc.qnl.lsrp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;

public class Node {
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
  }

  public void setAdjacent(boolean adj) {
    this.adjacent = adj;
  }

  public boolean isAdjacent() {
    return this.adjacent;
  }

  public void setConnected(boolean connected) {
    this.connected = connected;
  }

  public boolean isConnected() {
    return this.connected;
  }

  public void setChannel(Channel ch) {
    this.channel = ch;
  }

  public void sendLSRP(LSRPMessage msg) {
  }
}
