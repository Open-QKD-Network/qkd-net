package com.uwaterloo.iqc.kms.qnl;

public class KMSQNLConfig {

    private int port;
    private int keyByteSz;
    private int keyBlockSz;
    private String poolLoc;

    private String absPoolLoc;


    public KMSQNLConfig() {
    }

    public void setLoc(String loc) {
        this.absPoolLoc = loc + poolLoc;
    }

    public String getpoolLoc() {
        return poolLoc;
    }

    public String getLoc() {
        return absPoolLoc;
    }

    public int getPort() {
        return port;
    }

    public int getKeyByteSz() {
        return keyByteSz;
    }

    public int getKeyBlockSz() {
        return keyBlockSz;
    }
}
