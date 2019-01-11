package com.uwaterloo.iqc.qnl.qll;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import com.uwaterloo.iqc.qnl.QNLConfig;

public class QLLFileReader implements QLLReader {

    private String siteId;
    private AtomicLong qllBlockIndex = new AtomicLong(0);
    private int qllBlockSz;
    private String keyLoc;
    private int keyByteSz;

    public QLLFileReader(String id, QNLConfig qCfg) {
        siteId = id;
        this.qllBlockSz = qCfg.getQllBlockSz();
        this.keyLoc = qCfg.getQNLSiteKeyLoc(siteId);
        this.keyByteSz = qCfg.getKeyBytesSz();
    }

    private int readKeyBlock(byte[] dst, int len, long offset) {
        int linesRead = 0;
        long index;
        long startIndex;
        long qllBlockIndex;
        int qllIndexWithinBlock;
        String qllFile;
        boolean isSkipLines = true;
        int linesSkipped = 0;
        String line;
        BufferedReader reader;
        int destPos = 0;
        try {
            index = offset;
            startIndex = index - len;
            while (linesRead < len) {
                qllBlockIndex = startIndex / qllBlockSz;
                qllIndexWithinBlock = (int)startIndex % qllBlockSz;
                qllFile = keyLoc + "/" + siteId + "_" + qllBlockIndex;

                reader = new BufferedReader(new FileReader(qllFile));

                if (qllIndexWithinBlock > 0 && isSkipLines) {
                	while (linesSkipped < qllIndexWithinBlock) {
                        line = reader.readLine();
                        if (line != null)
                        	++linesSkipped;
                        else
                        	break;
                    }
                    isSkipLines = false;
                }

                line = reader.readLine();
                while (line != null && linesRead < len) {
                	++linesRead;
                    System.arraycopy(line.getBytes(), 0, dst, destPos, line.length());
                    destPos += line.length();
                    line = reader.readLine();
                    //if (line != null)
                    //	++linesRead;
                }
                if (linesRead < len)
                    startIndex = linesRead;
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return linesRead;
    }

    public int read(byte[] dst, int len, AtomicLong indexRef) {
        long index = qllBlockIndex.addAndGet(len);
        int linesRead = readKeyBlock(dst, len, index);
        if (linesRead == len)
        	indexRef.set(index);
        else {
        	indexRef.set(-1);
        	qllBlockIndex.addAndGet(-len);
        }
        return linesRead;
    }

    public int read(byte[] dst, int len, long offset) {
    	if (offset < 0)
    		return 0;
        return readKeyBlock(dst, len, offset);
    }

    public void getNextBlockIndex(int len, AtomicLong indexRef) {
        long index = qllBlockIndex.addAndGet(len);
        int blockIndex = (int)index / qllBlockSz;
        String fileStr = keyLoc + "/" + siteId + "_" + blockIndex;
        File f = new File(fileStr);
        if (f.exists())
        	indexRef.set(index);
        else {
        	qllBlockIndex.addAndGet(-len);
        	indexRef.set(-1);
        }
    }

}

