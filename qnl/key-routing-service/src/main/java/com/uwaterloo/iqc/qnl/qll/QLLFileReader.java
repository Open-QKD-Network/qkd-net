package com.uwaterloo.iqc.qnl.qll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import com.uwaterloo.iqc.qnl.QNLConfig;

public class QLLFileReader implements QLLReader {
    private static Logger LOGGER = LoggerFactory.getLogger(QLLFileReader.class);

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
        LOGGER.info("QLLFileReader.new:" + this);
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

        LOGGER.info(this + "-readKeyBlock:len:" + len + ",offset:" + offset);
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
        LOGGER.info(this + "-readKeyBlock:linesRead:" + linesRead);
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
        LOGGER.info(this + ".getNextBlockIndex,len:" + len + ",indexRef:" + indexRef.get());
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

    public String toString() {
      StringBuilder sb = new StringBuilder("QLLFileReader(");
      sb.append(this.siteId);
      sb.append(")");
      sb.append(",qllBlockSz=").append(this.qllBlockSz);
      sb.append(",keyLoc=").append(this.keyLoc);
      sb.append(",keyByteSz=").append(this.keyByteSz);
      sb.append(",qllBLockIndex=").append(this.qllBlockIndex.get());
      return sb.toString();
    }
}
