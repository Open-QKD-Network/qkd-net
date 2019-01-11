package com.uwaterloo.qkd.qnl.utils;

public class QNLConstants {

    //KMS-QNL OP
    public static final short REQ_GET_ALLOC_KP_BLOCK = 1;
    public static final short REQ_POST_ALLOC_KP_BLOCK = 2;

    //QNL-QNL OP
    public static final short REQ_POST_OTP_BLOCK_INDEX = 3;
    public static final short REQ_GET_KP_BLOCK_INDEX = 4;
    public static final short REQ_POST_KP_BLOCK_INDEX = 5;
    public static final short REQ_POST_PEER_ALLOC_KP_BLOCK = 6;

    //Corresponding responses
    public static final short RESP_GET_ALLOC_KP_BLOCK = 101;
    public static final short RESP_POST_ALLOC_KP_BLOCK = 102;

    public static final short RESP_POST_OTP_BLOCK_INDEX = 103;
    public static final short RESP_GET_KP_BLOCK_INDEX = 104;
    public static final short RESP_POST_KP_BLOCK_INDEX = 105;
    public static final short RESP_POST_PEER_ALLOC_KP_BLOCK = 106;



    public static final int KP_BLOCK_BYTES_SZ = 32768;

}
