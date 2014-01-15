package org.esa.cci.sst.util;

public final class SamplingPoint {

    private double random;
    private double lon;
    private double lat;
    private long time;
    private int reference;
    private int x;
    private int y;

    public SamplingPoint(double lon, double lat, long time, double random) {
        this.random = random;

        this.lon = lon;
        this.lat = lat;
        this.time = time;
    }

    public SamplingPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getRandom() {
        return random;
    }

    public int getReference() {
        return reference;
    }

    public void setReference(int reference) {
        this.reference = reference;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
