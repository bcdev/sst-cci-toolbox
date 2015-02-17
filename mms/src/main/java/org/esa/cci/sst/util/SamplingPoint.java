package org.esa.cci.sst.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.esa.cci.sst.common.InsituDatasetId;

public final class SamplingPoint {

    private double random;
    private double lon;
    private double lat;
    private long time;
    private int reference;
    private int index;

    private int reference2 = -1;
    private int x;
    private int y;
    private int insituReference;
    private long referenceTime;
    private double referenceLat;
    private double referenceLon;
    private InsituDatasetId insituDatasetId;
    private String datasetName;
    private long reference2Time;

    public SamplingPoint() {
    }

    public SamplingPoint(double lon, double lat, long time, double random) {
        this.random = random;

        this.lon = GeometryUtil.normalizeLongitude(lon);
        this.lat = lat;
        this.time = time;
    }

    public SamplingPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @JsonIgnore
    public boolean isInsitu() {
        return Double.isNaN(getRandom());
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = GeometryUtil.normalizeLongitude(lon);
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

    public void setReferenceTime(long referenceTime) {
        this.referenceTime = referenceTime;
    }

    public long getReferenceTime() {
        return referenceTime;
    }

    public void setReferenceLat(double referenceLat) {
        this.referenceLat = referenceLat;
    }

    public double getReferenceLat() {
        return referenceLat;
    }

    public void setReferenceLon(double referenceLon) {
        this.referenceLon = GeometryUtil.normalizeLongitude(referenceLon);
    }

    public double getReferenceLon() {
        return referenceLon;
    }

    public void setInsituDatasetId(InsituDatasetId insituDatasetId) {
        this.insituDatasetId = insituDatasetId;
    }

    public InsituDatasetId getInsituDatasetId() {
        return insituDatasetId;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setReference2Time(long reference2Time) {
        this.reference2Time = reference2Time;
    }

    public long getReference2Time() {
        return reference2Time;
    }
}
