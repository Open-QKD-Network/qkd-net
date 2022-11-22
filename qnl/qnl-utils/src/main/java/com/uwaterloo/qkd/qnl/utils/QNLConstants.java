package com.uwaterloo.qkd.qnl.utils;

public class QNLConstants {

    //KMS-QNL OP
    // Request a new key from the QNL keypool
    public static final short REQ_GET_ALLOC_KP_BLOCK = 1;
    // Reply from QNL with key data
    public static final short REQ_POST_ALLOC_KP_BLOCK = 2;

    //QNL-QNL OP
    // Unused
    public static final short REQ_POST_OTP_BLOCK_INDEX = 3;
    // Push some key to KS, and return its index
    public static final short REQ_GET_KP_BLOCK_INDEX = 4;
    // Push key with given index to KMS
    public static final short REQ_POST_KP_BLOCK_INDEX = 5;
    public static final short REQ_POST_PEER_ALLOC_KP_BLOCK = 6;

    //Corresponding responses
    public static final short RESP_GET_ALLOC_KP_BLOCK = 101;
    public static final short RESP_POST_ALLOC_KP_BLOCK = 102;

    // Unused
    public static final short RESP_POST_OTP_BLOCK_INDEX = 103;
    public static final short RESP_GET_KP_BLOCK_INDEX = 104;
    public static final short RESP_POST_KP_BLOCK_INDEX = 105;
    public static final short RESP_POST_PEER_ALLOC_KP_BLOCK = 106;



    public static final int KP_BLOCK_BYTES_SZ = 32768;

}
