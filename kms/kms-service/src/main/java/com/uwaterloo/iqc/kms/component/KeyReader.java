package com.uwaterloo.iqc.kms.component;

public interface KeyReader {
    public byte[] read(String poolname, long blockSz, long index) throws Exception;
}
