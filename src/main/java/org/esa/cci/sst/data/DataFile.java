package org.esa.cci.sst.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Data item that represents a source file with a record structure where each
 * record describes an observation. The objects are referred to in Observations
 * to identify the file.
 *
 * @author Martin Boettcher
 */
@Entity
@Table(name = "mm_datafile")
public final class DataFile {

    private int id;
    private String path;
    private Sensor sensor;

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    // important: this is not public API
    void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return slashify(path);
    }

    public void setPath(String path) {
        this.path = slashify(path);
    }

    public Sensor getSensor() {
        return sensor;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    @Override
    public String toString() {
        return String.format("DataFile(%d,%s)", getId(), getPath());
    }

    private static String slashify(String path) {
        if (path != null) {
            path = path.replace('\\', '/');
        }
        return path;
    }
}
