package com.uwaterloo.iqc.qnl.lsrp;

import java.util.List;
import java.util.LinkedList;

public class LSRPMessage {

  private boolean request;
  private String type;
  private String originator;
  private long timeStamp;
  private List<Node> neighbours = new LinkedList<Node>();

  public LSRPMessage() {
  }

  public LSRPMessage(boolean request, String type, String originator) {
    this.request = request;
    this.type = type;
    this.originator = originator;
    this.timeStamp = System.currentTimeMillis();
  }

  public void addNeighbour(Node neighbour) {
    this.neighbours.add(neighbour);
  }

  public long getTimeStamp() {
    return this.timeStamp;
  }

  public void setPayload(String payload) {
    // parse JSON
  }

  public String toString() {
    // convert to JSON
    return null;
  }
}
