/*
 * 
 * The reader needs to rely on the QNL for some functionality:
 * - QNL needs to init the reader on both ends
 * - QNL needs to actually send the (1) SYN, (2) SYN-ACK, (3) ACK triple handshake
 * -- get QNL to provide sendSYN(); sendSynACK(), sendACK();
 * 
 * 
 * After some config stuff, should give a very simple interface to the developer
 * The simple .read() functionality should still work.
 * 
 */

package com.uwaterloo.iqc.qnl.qll;

import java.util.concurrent.atomic.AtomicLong;

public class ETSIFileReader implements QLLReaderV2 {
    public static final int SAVE_FILE_AFTER_X_SECONDS = 30; 

    private static final int getTimeoutMilliseconds() {
        return SAVE_FILE_AFTER_X_SECONDS * 1000;
    }
    @Override
    public int ETSIRead(byte[] dst, int len, long offset) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void getNextBlockIndex(int len, AtomicLong index) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int read(byte[] dst, int len, long offset) {
        // TODO Auto-generated method stub
        // rn A calls read first.
        // Step 1: from A sendSyn(keyinfo, randomNum) to B.
        // Step 2: from B sendAck(random num alpha, beta + 1) send to A.
        // Step 3: from A sendAck(alpha + 1) to B. Wait for 30 seconds, write the key to file.
        return 0;
    }
    
}
