package org.esa.cci.sst.util;

public class StopWatch {

    private long t0;
    private long t1;

    public void start() {
        t0 = System.currentTimeMillis();
        t1 = t0;
    }

    public void stop() {
        t1 = System.currentTimeMillis();
    }

    public long getElapsedMillis() {
        return t1 - t0;
    }
}
