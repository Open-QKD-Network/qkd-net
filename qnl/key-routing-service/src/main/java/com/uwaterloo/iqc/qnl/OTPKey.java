package com.uwaterloo.iqc.qnl;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.codec.binary.Hex;

import com.uwaterloo.iqc.qnl.qll.QLLReader;
import com.uwaterloo.qkd.qnl.utils.QNLUtils;

public class OTPKey {

    private QNLConfiguration qnlConfig;
    private String id;
    private byte[] otpKey;

    public static final String OTPKEYNAME = "otpkey";

    public OTPKey(QNLConfiguration qnlConfig, String id) {
        this.qnlConfig = qnlConfig;
        this.id = id;
        createKey();
    }

    public void otp(byte[] data) throws Exception {
        int i = 0;
        for (byte b : otpKey)
            data[i] = (byte)(b ^ data[i++]);
    }

    private void createKey() {
        QNLConfig config = qnlConfig.getConfig();
        byte[] hex =  new byte[config.getKeyBlockSz()*config.getKeyBytesSz()*2];
        try {
        	File otpF = new File(config.getOTPKeyLoc(id));
        	if (!otpF.exists()) {
        		otpF.mkdirs();
        	}
        } catch(Exception e) {}
        String otpFile = config.getOTPKeyLoc(id) + "/" + OTPKEYNAME;
        File f = new File(otpFile);
        if(!f.exists()) {
            QLLReader QLLRdr = qnlConfig.getQLLReader(id);
            AtomicLong ref = new AtomicLong(0);
            QLLRdr.getNextBlockIndex(config.getKeyBlockSz(), ref);
            QLLRdr.read(hex, config.getKeyBlockSz(), ref.get());
            QNLUtils.writeKeys(hex, otpFile, config.getKeyBlockSz());
        } else {
            QNLUtils.readKeys(hex, otpFile, config.getKeyBlockSz());
        }

        try {
            otpKey = new Hex().decode(hex);
        } catch(Exception e) {}
    }
}
