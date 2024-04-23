package com.uwaterloo.iqc.qnl.qll;

import java.util.concurrent.atomic.AtomicLong;

public interface QLLReaderV2 {
    public void getNextBlockIndex(int len, AtomicLong index);

    public int read(byte[] dst, int len, long offset);

    public int ETSIRead(byte[] dst, int len, long offset);

}
