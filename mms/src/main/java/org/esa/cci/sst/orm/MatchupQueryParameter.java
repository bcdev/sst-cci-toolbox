package org.esa.cci.sst.orm;

import java.util.Date;

public class MatchupQueryParameter {

    private Date startDate;
    private Date stopDate;
    private String condition;
    private int pattern;

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

    public void setPattern(int pattern) {
        this.pattern = pattern;
    }

    public int getPattern() {
        return pattern;
    }
}
