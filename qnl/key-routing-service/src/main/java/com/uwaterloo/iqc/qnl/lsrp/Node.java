package com.uwaterloo.iqc.qnl.lsrp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Node {
  private String name;
  private String address;
  private int port;

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
}
