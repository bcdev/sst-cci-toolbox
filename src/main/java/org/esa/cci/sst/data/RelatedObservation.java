package org.esa.cci.sst.data;

import org.apache.openjpa.persistence.jdbc.Index;
import org.apache.openjpa.persistence.jdbc.Strategy;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * Data item that represents a single observation that is associated with
 * a location and time of observation.
 *
 * @author Martin Boettcher
 */
@Entity
public class RelatedObservation extends Observation implements Timed {

    Date time;
    PGgeometry location;

    @Column(columnDefinition = "GEOGRAPHY(GEOMETRY,4326)")
    @Strategy("org.esa.cci.sst.orm.GeographyValueHandler")
    public PGgeometry getLocation() {
        return location;
    }

    @Index
    @Temporal(TemporalType.TIMESTAMP)
    @Override
    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public void setLocation(PGgeometry location) {
        this.location = location;
    }

    public String toString() {
        return String.format("RelatedObservation(%d,%s,%s,%s,%s,%s,%d)", getId(), getName(), getSensor(),
                             TimeUtil.formatCcsdsUtcFormat(getTime()), getLocation(), getDatafile(), getRecordNo());
    }
}

