package org.esa.cci.sst.common;

import java.util.Date;

public class ExtraxtDefinitionImpl implements ExtractDefinition{

    private double lat;
    private double lon;
    private int recordNo;
    private int[] shape;
    private Date date;
    private Number fillValue;


    @Override
    public double getLat() {
        return lat;
    }

    @Override
    public double getLon() {
        return lon;
    }

    @Override
    public int getRecordNo() {
        return recordNo;
    }

    @Override
    public int[] getShape() {
        return shape;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public Number getFillValue() {
        return fillValue;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setRecordNo(int recordNo) {
        this.recordNo = recordNo;
    }

    public void setShape(int[] shape) {
        this.shape = shape;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setFillValue(Number fillValue) {
        this.fillValue = fillValue;
    }
}
