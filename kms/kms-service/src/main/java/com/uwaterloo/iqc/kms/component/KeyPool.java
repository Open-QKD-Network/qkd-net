package com.uwaterloo.iqc.kms.component;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;


public class KeyPool {

    private AtomicLong index = new AtomicLong(0);
    private String blockId;
    private long blockSz;
    private Vector<String> keys;

    public KeyPool(String blockId, long blockSz, Vector<String> keys) {
        this.blockId = blockId;
        this.blockSz = blockSz;
        this.keys = keys;
    }

    public Key getKey() {
        Key key = new Key();

        if (keys.size() == 0) {
            key.hexKey = null;
            key.index = -2;
        } else {
            key.index = index.getAndIncrement();
            key.hexKey = keys.get((int)key.index);
        }

        key.blockId = blockId;
        return key;
    }

    public Key getKey(int index) {
        Key key = new Key();
        key.index = index;
        key.hexKey = keys.get(index);
        key.blockId = blockId;
        return key;
    }

    public void setBlockId(String id) {
        blockId = id;
    }

    public void setBlockSz(long sz) {
        blockSz = sz;
    }

    public void resetCntr() {
        index.set(0);
    }

    public Vector<String> getKeys() {
        return keys;
    }

    public String getBlockId() {
        return blockId;
    }

    public boolean isValid() {
        long cnt = index.get();
        return (cnt < blockSz && (keys.size() != 0));
    }
}

