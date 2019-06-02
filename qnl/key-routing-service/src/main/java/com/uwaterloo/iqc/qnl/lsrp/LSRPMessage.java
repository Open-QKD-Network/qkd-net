package com.uwaterloo.iqc.qnl.lsrp;

import java.util.List;

public class LSRPMessage {
  private boolean request;
  private String type;
  private String originator;
  private int timeStamp;
  private List<Node> neighgours = new LinkedList<Node>();

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

  public void setPayload(String payload) {
    // parse JSON
  }

  public String toString() {
    // convert to JSON
    return null;
  }
}
