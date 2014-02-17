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

    public boolean isWithin(Date date) { // rq-20140217: suggestion - rename to 'includes'?
        return (!startDate.after(date)) && (!stopDate.before(date));
    }

    public boolean hasIntersectWith(TimeRange other) { // rq-20140217: suggestion - rename to 'intersectsWith'?
        return isWithin(other.getStartDate()) || isWithin(other.getStopDate());
    }
}
