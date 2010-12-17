package org.esa.cci.sst;

import org.apache.openjpa.persistence.Type;
import org.apache.openjpa.persistence.jdbc.Strategy;
import org.postgis.PGgeometry;
import org.postgis.PGgeometryLW;
import org.postgresql.jdbc2.TimestampUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * TODO add API doc
 *
 * @author Martin Boettcher
 */
@Entity
//@NamedQuery()
public class Observation {
    int id;
    String name;
    Date time;
    PGgeometry location;  // TODO maybe use non-postgres type for external representation
    DataFile datafile;
    int recordNo;

    @Id
    @GeneratedValue
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

    @Temporal(TemporalType.TIMESTAMP)
    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    @Column(columnDefinition = "GEOGRAPHY(POINT,4326)")
    //@Type(PGgeometry.class)
    @Strategy("org.esa.cci.sst.GeographyValueHandler")
    public PGgeometry getLocation() {
        return location;
    }

    public void setLocation(PGgeometry location) {
        this.location = location;
    }

    public DataFile getDatafile() {
        return datafile;
    }

    public void setDatafile(DataFile datafile) {
        this.datafile = datafile;
    }

    public int getRecordNo() {
        return recordNo;
    }

    public void setRecordNo(int recordNo) {
        this.recordNo = recordNo;
    }

    public String toString() {
        return String.format("Observation(%d,%s,%s,%s,%s,%d", id, name, TimeUtil.formatCcsdsUtcFormat(time), location, datafile, recordNo);
    }
}

