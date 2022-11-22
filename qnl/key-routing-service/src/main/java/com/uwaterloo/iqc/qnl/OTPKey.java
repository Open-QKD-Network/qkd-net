package com.uwaterloo.iqc.qnl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.uwaterloo.iqc.qnl.qll.QLLReader;

public class OTPKey {
    private static Logger LOGGER = LoggerFactory.getLogger(OTPKey.class);

    private QNLConfiguration qnlConfig;
    private String id;

    public OTPKey(QNLConfiguration qnlConfig, String id) {

        this.qnlConfig = qnlConfig;
        this.id = id;
        LOGGER.info("OTPKey.new:id=" + id);
    }

    public String encode(byte[] data) {
        QLLReader qllRdr = qnlConfig.getQLLReader(id);
        byte[] otpKey = new byte[data.length];
        String keyIdentifier = qllRdr.readNextKey(otpKey, data.length);

        int i = 0;
        for (byte b : otpKey) {
            data[i] = (byte) (b ^ data[i++]);
        }

        return keyIdentifier;
    }

    public void decode(byte[] data, String keyIdentifier) {
        QLLReader qllRdr = qnlConfig.getQLLReader(id);
        byte[] otpKey = qllRdr.read(keyIdentifier);

        int i = 0;
        for (byte b : otpKey)
            data[i] = (byte)(b ^ data[i++]);
    }
}
