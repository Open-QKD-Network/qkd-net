package com.uwaterloo.iqc.qnl;

import java.util.Map;

/** Handles or wraps around ~/.qkd/qnl/config.yaml */
public class QNLConfig {
    
    /** Base location for finding other QNL-related paths & files. */
    private String base;
    
    /** Name of the route configuration file */
    private String routeConfigLoc;
    
    /** Location (relative to {@code base}) where QLL puts the key blocks for QNL to carve out key blocks for KMS.  */
    private String qnlSiteKeyLoc;
    
    private String siteId;
    
    /** Port on which key routing service is listening for key block requests. */
    private int port;
    
    /** Size of a key in bytes. */
    private int keyBytesSz;
    
    /** Number of keys in a block. Key routing service provides KMS keys in blocks of this size. */
    private int keyBlockSz;
    
    /** Number of bytes per block that QLL to provide keys in. Used by Key Routing Service. */
    private int qllBlockSz;
    
    // [@rahul doubt]: I'm not sure whether this is still used or not.
    private int headerSz;
    
    /** IP address where KMS QNL service is running. */
    private String kmsIP;
    
    /** Port on which KMS QNL service is running */
    private int kmsPort;
    
    /**
     * Contains 
     * keyBlockS: number of keys per block.
     * keyLoc: location of OTP key block relative to {@code base}
     */
    public Map<String, String> OTPConfig;
    public static final String OTP_KEYBLOCKSZ = "keyBlockSz";
    public static final String KMS = "kms";

    /** [@rahul temp] example return value: ~/.qkd/qnl/routes.json */         
    public String getRouteConfigLoc() { 
        return System.getProperty("user.home") + "/" + base + "/" + routeConfigLoc;
    }

    public String getQNLSiteKeyLoc(String siteId) {
        return System.getProperty("user.home") + "/" + base + "/"  +
               "/" + qnlSiteKeyLoc + "/" + siteId;
    }

    public String getOTPKeyLoc(String siteId) {
        return System.getProperty("user.home") + "/" + base + "/" +
               "/" + OTPConfig.get("keyLoc") + "/" + siteId;
    }

    /** Returns the base location for finding other QNL-related paths & files. For example {@code .qkd/qnl}*/
    public String getBase() {
        return base;
    }

    /** Returns the location (relative to {@code base}) where QLL puts the key blocks for QNL to carve out key blocks for KMS.  */
    public String getQnlSiteKeyLoc() {
        return qnlSiteKeyLoc;
    }

    /** Returns the size of a key in bytes. */
    public int getKeyBytesSz() {
        return keyBytesSz;
    }

    /** Returns the number of keys in a block. */
    public int getKeyBlockSz() {
        return keyBlockSz;
    }

    /** Returns the port on which Key Routing Service is listening for key block requests. */
    public int getPort() {
        return port;
    }

    /** Number of bytes per block that QLL to provide keys in. */
    public int getQllBlockSz() {
        return qllBlockSz;
    }

    public int getHeaderSz() {
        return headerSz;
    }

    public String getSiteId() {
        return siteId;
    }

    /** Returns the port on which KMS QNL service is running */
    public int getKmsPort() {
        return kmsPort;
    }

    /** Returns the IP address where KMS QNL service is running. */
    public String getKmsIP() {
        return kmsIP;
    }

    public int getOTPKeyBlockSz() {
        return Integer.valueOf(OTPConfig.get(OTP_KEYBLOCKSZ)).intValue();
    }

}