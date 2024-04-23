package com.uwaterloo.iqc.qnl.qll;

/**
 * Copied from the mock ETSI Server implementation.
 */
public class QLLRequest {

    private int seed;

    public QLLRequest() {
        seed = -1;
    }

    public QLLRequest(int seed) {
        System.out.println("QLLRequest ctor called with seed");
        this.seed = seed;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public boolean seedAbsent() { return seed == -1; }
    @Override
    public String toString() {
        return "QLLRequest{" +
                "seed=" + seed +
                '}';
    }
}
