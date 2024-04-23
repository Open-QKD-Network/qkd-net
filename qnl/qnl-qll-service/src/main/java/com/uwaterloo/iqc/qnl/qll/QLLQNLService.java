package com.uwaterloo.iqc.qnl.qll;

public class QLLQNLService implements IQnlQll {
    @Override
    public void sendSyn(byte[] data) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendSyn'");
    }

    @Override
    public void sendSynAck(byte[] data) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendSynAck'");
    }

    @Override
    public void sendAck(byte[] data) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendAck'");
    }

    @Override
    public void sendBootstrap(byte[] data) {
        // TODO Auto-generated method stub
        // send a QNL REQUEST to neighbour QNL for neighbouring QNL
        throw new UnsupportedOperationException("Unimplemented method 'sendBootstrap'");
    }

    @Override
    public void sendBootstrapAck(byte[] data) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendBootstrapAck'");
    }    
}
