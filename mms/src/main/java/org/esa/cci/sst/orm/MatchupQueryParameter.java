package org.esa.cci.sst.orm;

import java.util.Date;

public class MatchupQueryParameter {

    private Date startDate;
    private Date stopDate;
    private String condition;
    private long pattern;
    private String sensorName;

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStopDate(Date stopDate) {
        this.stopDate = stopDate;
    }

    public Date getStopDate() {
        return stopDate;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getCondition() {
        return condition;
    }

    public void setPattern(long pattern) {
        this.pattern = pattern;
    }

    public long getPattern() {
        return pattern;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    public String getSensorName() {
        return sensorName;
    }
}
