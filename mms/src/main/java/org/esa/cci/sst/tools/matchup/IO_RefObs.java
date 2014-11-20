package org.esa.cci.sst.tools.matchup;


import java.util.Date;

class IO_RefObs {

    private int id;
    private String name;
    private String sensor;
    private String filePath;
    private int sensorId;
    private Date time;
    private double timeRadius;
    private String location;
    private String point;
    private byte dataset;
    private byte referenceFlag;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSensor() {
        return sensor;
    }

    public void setSensor(String sensor) {
        this.sensor = sensor;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getSensorId() {
        return sensorId;
    }

    public void setSensorId(int sensorId) {
        this.sensorId = sensorId;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public double getTimeRadius() {
        return timeRadius;
    }

    public void setTimeRadius(double timeRadius) {
        this.timeRadius = timeRadius;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPoint() {
        return point;
    }

    public void setPoint(String point) {
        this.point = point;
    }

    public byte getDataset() {
        return dataset;
    }

    public void setDataset(byte dataset) {
        this.dataset = dataset;
    }

    public byte getReferenceFlag() {
        return referenceFlag;
    }

    public void setReferenceFlag(byte referenceFlag) {
        this.referenceFlag = referenceFlag;
    }
}
