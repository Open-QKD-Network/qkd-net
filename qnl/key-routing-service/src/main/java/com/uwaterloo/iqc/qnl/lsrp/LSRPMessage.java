package com.uwaterloo.iqc.qnl.lsrp;

import java.util.List;
import java.util.LinkedList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSRPMessage {
  private static Logger LOGGER = LoggerFactory.getLogger(LSRPMessage.class);

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
    if (payload == null || payload.length() == 0)
      return;
    // parse JSON
    JSONParser jsonParser = new JSONParser();
    try {
      JSONObject json = (JSONObject) jsonParser.parse(payload);
      this.originator = (String) json.get("originator");
      this.type = (String) json.get("type");
      this.request = (Boolean) json.get("request");
      this.timeStamp = (Long) json.get("timestamp");

      JSONArray neighbours = (JSONArray) json.get("neighbours");
      for (int index = 0; index < neighbours.size(); index++) {
        JSONObject jn = (JSONObject) neighbours.get(index);
        Node n = new Node((String) jn.get("name"),
          (String) jn.get("address"),
          (Integer) jn.get("port"));
        this.neighbours.add(n);
      }
    } catch (Exception e) {
      LOGGER.error("Fails to parse JSON:" + e + ", for payload:" + payload);
    }
  }

  public String toString() {
    JSONObject lsp = new JSONObject();
    lsp.put("request", this.request);
    lsp.put("type", this.type);
    lsp.put("originator", this.originator);
    lsp.put("timestamp", this.timeStamp);

    JSONArray ns = new JSONArray();

    for (int index = 0; index < this.neighbours.size(); index++) {
      Node neighbour = this.neighbours.get(index);
      JSONObject n = new JSONObject();
      n.put("name", neighbour.getName());
      n.put("address", neighbour.getAddress());
      n.put("port", neighbour.getPort());
      n.put("weight", 1);
      ns.add(n);
    }

    lsp.put("neighbours", ns);
    return lsp.toJSONString();
  }
}
