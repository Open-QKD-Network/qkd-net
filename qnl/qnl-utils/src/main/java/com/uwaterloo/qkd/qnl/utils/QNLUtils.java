package com.uwaterloo.qkd.qnl.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

public class QNLUtils {

    public static int readKeys(byte[] dst, String file, int keyBlockSz) {
        String line;
        int linesRead = 0;
        int destPos = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            line = reader.readLine();
            while (line != null) {
                ++linesRead;
                System.arraycopy(line.getBytes(), 0, dst, destPos, line.length());
                destPos += line.length();
                line = reader.readLine();
            }
            reader.close();
        } catch(IOException ioe) {}

        return linesRead;
    }

    public static int readKeys(Vector<String> v, String file, int keyBlockSz) {
        String line;
        int linesRead = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            line = reader.readLine();
            while (line != null) {
                ++linesRead;
                v.addElement(line);
                line = reader.readLine();
            }
            reader.close();
        } catch(IOException ioe) {}

        return linesRead;
    }

    public static void writeKeys(byte[] src, String file, int keyBlockSz) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            int hexBytes = src.length / keyBlockSz;
            for(int k = 0; k < keyBlockSz; ++k) {
                bw.write(new String(src, k*hexBytes, hexBytes));
                bw.newLine();
            }
            bw.close();
        } catch(IOException ioe) {}
    }
    
    public static void writeKeys(Vector<String> src, String file, int keyBlockSz) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for(String key : src) {
            	bw.write(key);
            	bw.newLine();
            }
            bw.close();
        } catch(IOException ioe) {}
    }


}
