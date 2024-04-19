package com.uwaterloo.iqc.kms.qnl;

public class KMSQNLConfig {

    /** Port on which KMS QNL service {@link KeyReceivingServer} is listening. */ 
    private int port;
    
    /** Size of a key in bytes */
    private int keyByteSz;
    
    /** Number of keys in a block. QNL's Key Routing Service provides KMS keys in blocks of this size. */
    private int keyBlockSz;
    
    /** Location (relative to {@code .qkd} directory) where the KMS QNL service puts the keyblocks pushed QNL's Key Routing Service */
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
