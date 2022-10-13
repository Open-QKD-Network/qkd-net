package com.uwaterloo.iqc.qnl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import org.apache.commons.codec.binary.Hex;

import com.uwaterloo.iqc.qnl.qll.QLLReader;
import com.uwaterloo.qkd.qnl.utils.QNLUtils;

public class OTPKey implements KeyListener {
    private static Logger LOGGER = LoggerFactory.getLogger(OTPKey.class);

    private QNLConfiguration qnlConfig;
    private String id;
    private byte[] otpKey;

    public static final String OTPKEYNAME = "otpkey";
    private boolean canRead = false;

    public OTPKey(QNLConfiguration qnlConfig, String id) {
        this.qnlConfig = qnlConfig;
        this.id = id;
        LOGGER.info("OTPKey.new:id=" + id);
        createKey();
    }

    public void otp(byte[] data) throws Exception {
        int i = 0;
        for (byte b : otpKey)
            data[i] = (byte)(b ^ data[i++]);
    }

    public void onKeyGenerated() {
        if (!this.canRead) {
            this.canRead = true;
            createKey();
        }
    }

    public void reset() {
        canRead = false;
    }

    private void createKey() {

        if(!canRead) {
            return;
        }

        QNLConfig config = qnlConfig.getConfig();
        byte[] hex =  new byte[config.getKeyBlockSz()*config.getKeyBytesSz()*2];

        try {

           File otpF = new File(config.getOTPKeyLoc(id));
           if (!otpF.exists()) {
              otpF.mkdirs();
           }

        } catch(Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            LOGGER.error("OTPKey.createKey exception:" + e + ",stacktrace:" + sw.toString());
        }

        String otpFile = config.getOTPKeyLoc(id) + "/" + OTPKEYNAME;
        File f = new File(otpFile);

        if(!f.exists()) {

            QLLReader QLLRdr = qnlConfig.getQLLReader(id);
            String keyId = QLLRdr.getNextKeyId(config.getKeyBlockSz()*config.getKeyBytesSz());
            byte[] binKey = QLLRdr.read(keyId);
            hex = Hex.encodeHexString(binKey).getBytes();
            LOGGER.info("OTPKey.writeKeys to :" + otpFile);
            QNLUtils.writeKeys(hex, otpFile, config.getKeyBlockSz());

        } else {
            QNLUtils.readKeys(hex, otpFile, config.getKeyBlockSz());
        }

        try {
            otpKey = new Hex().decode(hex);
        } catch(Exception e) {}
    }
}
