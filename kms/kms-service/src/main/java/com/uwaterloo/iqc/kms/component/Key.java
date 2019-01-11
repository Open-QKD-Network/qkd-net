package com.uwaterloo.iqc.kms.component;

public class Key {
    public long index = -2;
    public String hexKey = null;
    public String blockId = null;

    public String toString() {
        StringBuffer sbf = new StringBuffer();
        sbf.append("	Index: " + index);
        sbf.append("	Key: " + hexKey);
        sbf.append("	BlockId: " + blockId);
        return sbf.toString();
    }

}