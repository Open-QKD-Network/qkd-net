package com.uwaterloo.iqc.qnl.qll;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.uwaterloo.iqc.qnl.QNLConfig;

import static java.lang.Math.min;


public class QLLFileReaderWriter implements QLLReader {
    private static Logger LOGGER = LoggerFactory.getLogger(QLLFileReaderWriter.class);

    /*TEMP TEST VALUES*/
    //The below currents are added to avoid un-needed file IO initializations
    private File currentFile;
    private BufferedWriter currentWriter;

    private String siteId;

    // Current block to read from
    private int qllReadBlockIndex = 0;

    // Current offset into read block
    private int qllReadBlockOffset = 0;

    private int qllBlockIndex = 0;
    // Current block to write into
    private int qllWriteBlockIndex = 0;

    // Current offset into write block
    private int qllWriteBlockOffset = 0;
    private int qllBlockSz;
    private String keyLoc;

    // Maximum size of blocks
    private static final int blockBytes = 4096 * 2;

    public QLLFileReaderWriter(String id, QNLConfig qCfg) {
        siteId = id;
        this.qllBlockSz = qCfg.getQllBlockSz();
        this.keyLoc = qCfg.getQNLSiteKeyLoc(siteId);
        LOGGER.info("QLLFileReaderWriter.new:|" + this);
    }

    private File blockName(int blockIndex) {
        return new File(this.keyLoc + "/" + siteId + "_" + blockIndex);
    }

    private void openBlock() {
        File qllFile = blockName(qllWriteBlockIndex);

        try {
            if(this.currentFile == null || !qllFile.equals(this.currentFile)) {
                this.currentFile = qllFile;
                if(this.currentFile.exists()) {
                    this.currentFile.delete();
                    this.currentFile.createNewFile();
                }
                if (currentWriter != null) {
                    currentWriter.close();
                }
                this.currentWriter = new BufferedWriter(new FileWriter(this.currentFile, true));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(byte[] key) {
        char[] keyHex = Hex.encodeHex(key);

        try {
            int written = 0;
            while (written < keyHex.length) {
                openBlock();
                // Write at most so many bytes until block is full
                int toWrite = min(blockBytes - qllWriteBlockOffset, keyHex.length - written);
                this.currentWriter.write(keyHex, written, toWrite);
                this.currentWriter.flush();
                qllWriteBlockOffset += toWrite;
                written += toWrite;

                // Advance to next block if current is full
                if (qllWriteBlockOffset >= blockBytes) {
                    LOGGER.info("Full key block written, index is {}",  qllWriteBlockIndex);
                    qllWriteBlockOffset = 0;
                    qllWriteBlockIndex++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNextKeyId(int len) {
        return String.valueOf(qllReadBlockIndex) + "-" + String.valueOf(qllReadBlockOffset) + "-" + String.valueOf(len);
    }

    @Override
    public String readNextKey(byte[] dst, int len) {
        String keyId = String.valueOf(qllReadBlockIndex) + "-" + String.valueOf(qllReadBlockOffset) + "-" + String.valueOf(len);
        byte[] data = readKeyBlock(len, qllReadBlockIndex, qllReadBlockOffset);
        System.arraycopy(data, 0, dst, 0,len);
        return keyId;
    }

    public byte[] read(String identifier) {
        String[] parts = identifier.split("-");
        if (parts.length != 3) {
            throw new RuntimeException("Invalid key identifier " + identifier);
        }
        int block = Integer.parseInt(parts[0]);
        int offset = Integer.parseInt(parts[1]);
        int length = Integer.parseInt(parts[2]);
        return readKeyBlock(length, block, offset);
    }

    private synchronized byte[] readKeyBlock(int len, int block, int offset) {
        File blockName = blockName(block);
        len *= 2; // read as hex, so 2 characters per byte
        char[] hexbuf = new char[len];
        try {
            BufferedReader reader = new BufferedReader(new FileReader(blockName));
            reader.skip(offset);
            int read = 0;


            while (read < len) {
                if (offset >= blockBytes) {
                    blockName = blockName(++block);
                    reader = new BufferedReader(new FileReader(blockName));
                    offset = 0;
                }
                int res = reader.read(hexbuf, read, len - read);
                if (res <= 0) {
                    throw new IOException("Key block did not provide sufficient data");
                }
                offset += res;
                read += res;
            }
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            LOGGER.error("QLLFileReaderWriter.readKeyBlock exception:" + e + ",stacktrace:" + sw.toString());
        }
        try {
            return Hex.decodeHex(hexbuf);
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString() {
      StringBuilder sb = new StringBuilder("QLLFileReaderWriter(");
      sb.append(this.siteId);
      sb.append(")");
      sb.append(",qllBlockSz=").append(this.qllBlockSz);
      sb.append(",keyLoc=").append(this.keyLoc);
      sb.append(",qllBLockIndex=").append(this.qllBlockIndex);
      return sb.toString();
    }
}
