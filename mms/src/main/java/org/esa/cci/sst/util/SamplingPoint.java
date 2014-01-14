package org.esa.cci.sst.util;

public final class SamplingPoint {

    private final double lon;
    private final double lat;
    private long time;
    private final double random;
    private int reference;

    public SamplingPoint(double lon, double lat, long time, double random) {
        this.lon = lon;
        this.lat = lat;
        this.time = time;
        this.random = random;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    public long getTime() {
        return time;
    }

    public double getRandom() {
        return random;
    }

    public void setReference(int reference) {
        this.reference = reference;
    }

    public int getReference() {
        return reference;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
