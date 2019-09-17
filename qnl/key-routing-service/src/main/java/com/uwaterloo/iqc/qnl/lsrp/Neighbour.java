package com.uwaterloo.iqc.qnl.lsrp;

class Neighbour {
  public String name;
  public String addr;
  public long port;
  public int weight;

  public Neighbour(String name, String addr, long port) {
    this.name = name;
    this.addr = addr;
    this.port = port;
    this.weight = 1;
  }
}
