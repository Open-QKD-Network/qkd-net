package com.uwaterloo.iqc.kms.component;

import com.google.gson.Gson;

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

    public String toJsonString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
