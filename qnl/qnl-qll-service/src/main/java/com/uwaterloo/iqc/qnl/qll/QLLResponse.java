package com.uwaterloo.iqc.qnl.qll;

/**
 * Copied from the sample server implementation.
 */
public class QLLResponse {
    private int seed;
    private String message;
    public QLLResponse() {}

    public QLLResponse(int seed, String message) {
        this.seed = seed;
        this.message = message;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    @Override
    public String toString() {
        return "QLLResponse{" +
                "seed=" + seed +
                "message=" + message +
                '}';
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

