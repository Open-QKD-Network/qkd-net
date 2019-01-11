package com.uwaterloo.iqc.kms.component;

import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class KeyUtils {

    public static String getKey(RandomAccessFile stream, int index, String md5Str) throws Exception {

        if (md5Str == null || md5Str.length() == 0)
            return "";

        byte[] bytes = keyBytes(stream, index);
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] md5Digest = md.digest(bytes);
        String md5CalcStr = bytesToHexString(md5Digest);
        if (md5CalcStr.equals(md5Str))
            return bytesToHexString(bytes);
        else
            return "";
    }

    public static String newKey(RandomAccessFile stream, int index) throws Exception {
        byte[] bytes = keyBytes(stream, index);
        return bytesToHexString(bytes);
    }

    public static String SHAsum(byte[] convertme) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return byteArray2Hex(md.digest(convertme));
    }

    public static String byteArray2Hex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String sha1 = formatter.toString();
        formatter.close();
        return sha1;
    }

    public static String bytesToHexString(byte[] bytes) throws Exception {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String hexStr = formatter.toString();
        formatter.close();
        return hexStr;
    }

    public static byte[] hexStringToBytes(String s) throws Exception {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                  + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }



    private static byte[] keyBytes(RandomAccessFile stream, int index) throws Exception {
        byte[] bytes = new byte[32];
        stream.seek(0);
        stream.skipBytes(index*32);
        stream.readFully(bytes);
        return bytes;

    }
}
