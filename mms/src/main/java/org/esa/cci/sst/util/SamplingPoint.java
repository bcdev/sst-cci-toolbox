package org.esa.cci.sst.util;

public final class SamplingPoint {

    private double random;
    private double lon;
    private double lat;
    private long time;
    // todo - make Integer?
    private int reference;
    private int index;

    // todo - make Integer?
    private int reference2 = -1;
    private int x;
    private int y;
    private int insituReference;

    public SamplingPoint() {
    }

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

    public boolean isInsitu() {
        return Double.isNaN(getRandom());
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

    public int getReference2() {
        return reference2;
    }

    public void setReference2(int reference2) {
        this.reference2 = reference2;
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setInsituReference(int insituReference) {
        this.insituReference = insituReference;
    }

    public int getInsituReference() {
        return insituReference;
    }
}
