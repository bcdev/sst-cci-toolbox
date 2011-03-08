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
public class ReferenceObservation extends RelatedObservation {

    PGgeometry point;
    boolean clearSky;

    @Column(columnDefinition = "GEOGRAPHY(POINT,4326)")
    @Strategy("org.esa.cci.sst.orm.PointValueHandler")
    public PGgeometry getPoint() {
        return point;
    }

    public void setPoint(PGgeometry point) {
        this.point = point;
    }

    public boolean isClearSky() {
        return clearSky;
    }

    public void setClearSky(boolean clearSky) {
        this.clearSky = clearSky;
    }

    public String toString() {
        return String.format("Observation(%d,%s,%s,%s,%s,%s,%s,%d,%b)", getId(), getName(), getSensor(), TimeUtil.formatCcsdsUtcFormat(
                getTime()), getPoint(), getLocation(), getDatafile(), getRecordNo(), isClearSky());
    }
}

