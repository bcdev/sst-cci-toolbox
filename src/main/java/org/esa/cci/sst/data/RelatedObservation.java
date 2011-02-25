package org.esa.cci.sst.data;

import org.apache.openjpa.persistence.jdbc.Strategy;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Data item that represents a single observation, either with an SST value
 * or a sub-scene or a related value for aerosol, sea ice, etc. The object
 * refers to a record in an MD file or a record in a related file.
 *
 * @author Martin Boettcher
 */
@Entity
public class RelatedObservation extends GlobalObservation {

    PGgeometry location;

    @Column(columnDefinition = "GEOGRAPHY(GEOMETRY,4326)")
    @Strategy("org.esa.cci.sst.orm.GeographyValueHandler")
    public PGgeometry getLocation() {
        return location;
    }

    public void setLocation(PGgeometry location) {
        this.location = location;
    }

    public String toString() {
        return String.format("Observation(%d,%s,%s,%s,%s,%s,%d)", getId(), getName(), getSensor(), TimeUtil.formatCcsdsUtcFormat(getTime()), getLocation(), getDatafile(), getRecordNo());
    }
}

