package org.esa.cci.sst.data;

import org.apache.openjpa.persistence.jdbc.Index;
import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
@Table(name = "mm_observation")
public class Observation {
    int id;
    String name;
    String sensor;
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

    public String getSensor() {
        return sensor;
    }

    public void setSensor(String sensor) {
        this.sensor = sensor;
    }

    @ManyToOne
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
        return String.format("Observation(%d,%s,%s,%s,%d)", getId(), getName(), getSensor(), getDatafile(), getRecordNo());
    }
}

