package com.uwaterloo.iqc.qnl.qll;

public interface IQnlQll {
    public void sendSyn(byte[] data);
    public void sendSynAck(byte[] data);
    public void sendAck(byte[] data);
    public void sendBootstrap(byte[] data);
    public void sendBootstrapAck(byte[] data);
    // QLL needs to know where to write keys to.
}