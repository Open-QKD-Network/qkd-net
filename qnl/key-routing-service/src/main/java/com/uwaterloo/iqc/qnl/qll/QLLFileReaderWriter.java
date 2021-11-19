package com.uwaterloo.iqc.qnl.qll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.RandomAccessFile;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.concurrent.atomic.AtomicLong;

import com.uwaterloo.iqc.qnl.QNLConfig;


public class QLLFileReaderWriter implements QLLReader {
    private static Logger LOGGER = LoggerFactory.getLogger(QLLFileReaderWriter.class);

    /*TEMP TEST VALUES*/
    //The below currents are added to avoid un-needed file IO initializations
    private File currentFile;
    private BufferedWriter currentWriter;

    private String siteId;
    private AtomicLong qllBlockIndex = new AtomicLong(0);
    private int qllBlockSz;
    private String keyLoc;
    private int keyByteSz;

    public QLLFileReaderWriter(String id, QNLConfig qCfg) {
        siteId = id;
        this.qllBlockSz = qCfg.getQllBlockSz();
        this.keyLoc = qCfg.getQNLSiteKeyLoc(siteId);
        this.keyByteSz = qCfg.getKeyBytesSz();
        LOGGER.info("QLLFileReaderWriter.new:|" + this);
    }

    private synchronized void writeKeyBlock(long seqId, String keyHexString, String destinationSiteId) {

        long qllBlockIndex = seqId/this.qllBlockSz;
        String qllFileName = this.keyLoc + "/" + destinationSiteId+"_"+qllBlockIndex;

        try {

            if(currentFile == null || !qllFileName.equals(this.currentFile.getName())) {

                this.currentFile = new File(qllFileName);
                this.currentWriter = new BufferedWriter(new FileWriter(this.currentFile, true));

                /*
                    the branching statement below results in text files with only resulting line after ./scripts/run is complete
                    i suspect this is because the destination file changes all the time when the key blocks are being received.
                    i am not sure if that is the expected behaviour, however, because i initially expected qllFileName to remain constant for each block of keys being written.
                    if so, then we should clear the text files based on completion rather than existence.

                if(!this.currentFile.exists()) {
                    this.currentFile.createNewFile();
                } else {
                    new RandomAccessFile(qllFileName, "rw").setLength(0); //empties file
                }
                */

                if(!this.currentFile.exists()) {
                    this.currentFile.createNewFile();
                }

            }

            this.currentWriter.write(keyHexString + "\n");
            this.currentWriter.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized int readKeyBlock(byte[] dst, int len, long offset) {

        int linesRead = 0; int linesSkipped = 0; int destPos = 0;
        int qllIndexWithinBlock;
        long index, startIndex, qllBlockIndex;
        boolean isSkipLines = true;
        String qllFile, line;
        BufferedReader reader;

        LOGGER.info("QLLFileReaderWriter.readKeyBlock:len:" + len + ",end offset:" + offset + "|" + this);
        try {

            index = offset;
            startIndex = index - len;

            while (linesRead < len) {

                qllBlockIndex = startIndex / this.qllBlockSz;
                qllIndexWithinBlock = (int)startIndex % this.qllBlockSz;
                qllFile = this.keyLoc + "/" + this.siteId + "_" + qllBlockIndex;

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
                    line = (line.trim().split(" ", 2))[1]; //split the line by the whitespace separating seqID & key and retain the key

                    System.arraycopy(line.getBytes(), 0, dst, destPos, line.length());
                    destPos += line.length();
                    line = reader.readLine();
                }

                if (linesRead < len) {
                    startIndex = linesRead;
                }

                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("QLLFileReaderWriter.readKeyBlock:linesRead:" + linesRead + "|" + this);
        return linesRead;
    }

    public void write(long seqId, String keyHexString, String destinationSiteId){
        writeKeyBlock(seqId,keyHexString,destinationSiteId);
    }

    public int read(byte[] dst, int len, AtomicLong indexRef) {
        long index = this.qllBlockIndex.addAndGet(len);
        int linesRead = readKeyBlock(dst, len, index);
        if (linesRead == len)
        	indexRef.set(index);
        else {
        	indexRef.set(-1);
        	this.qllBlockIndex.addAndGet(-len);
        }
        return linesRead;
    }

    public int read(byte[] dst, int len, long offset) {
    	if (offset < 0)
    		return 0;
        return readKeyBlock(dst, len, offset);
    }

    public void getNextBlockIndex(int len, AtomicLong indexRef) {
        LOGGER.info("QLLFileReaderWriter.getNextBlockIndex,len:" + len + ",indexRef:" + indexRef.get() + "|" + this);
        long index = this.qllBlockIndex.addAndGet(len);
        int blockIndex = (int)index / this.qllBlockSz;
        String fileStr = this.keyLoc + "/" + this.siteId + "_" + blockIndex;
        File f = new File(fileStr);
        if (f.exists())
        	indexRef.set(index);
        else {
        	this.qllBlockIndex.addAndGet(-len);
        	indexRef.set(-1);
        }
    }

    public String toString() {
      StringBuilder sb = new StringBuilder("QLLFileReaderWriter(");
      sb.append(this.siteId);
      sb.append(")");
      sb.append(",qllBlockSz=").append(this.qllBlockSz);
      sb.append(",keyLoc=").append(this.keyLoc);
      sb.append(",keyByteSz=").append(this.keyByteSz);
      sb.append(",qllBLockIndex=").append(this.qllBlockIndex.get());
      return sb.toString();
    }
}