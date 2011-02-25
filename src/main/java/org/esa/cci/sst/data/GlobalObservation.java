package org.esa.cci.sst.data;

import org.apache.openjpa.persistence.jdbc.Index;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * Data item that represents a single observation, either with an SST value
 * or a sub-scene or a related value for aerosol, sea ice, etc. The object
 * refers to a record in an MD file or a record in a related file.
 *
 * @author Martin Boettcher
 */
@Entity
public class GlobalObservation extends Observation {

    Date time;

    @Index
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String toString() {
        return String.format("Observation(%d,%s,%s,%s,%s,%d)", getId(), getName(), getSensor(), TimeUtil.formatCcsdsUtcFormat(getTime()), getDatafile(), getRecordNo());
    }
}

