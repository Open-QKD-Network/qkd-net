package com.uwaterloo.iqc.qnl.qll;

import java.util.concurrent.atomic.AtomicLong;

public interface QLLReader {
    public int read(byte[] dst, int len, AtomicLong index);

    public void getNextBlockIndex(int len, AtomicLong index);

    public int read(byte[] dst, int len, long offset);

}
