package org.esa.cci.sst.tools.samplepoint;


import java.util.Date;

class TimeRange {

    private Date startDate;
    private Date stopDate;

    TimeRange(Date startDate, Date stopDate) {
        this.startDate = startDate;
        this.stopDate = stopDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getStopDate() {
        return stopDate;
    }

    public boolean includes(Date date) {
        return (!startDate.after(date)) && (!stopDate.before(date));
    }

    public boolean intersectsWith(TimeRange other) {
        return includes(other.getStartDate()) || includes(other.getStopDate());
    }
}
