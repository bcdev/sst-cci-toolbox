package org.esa.cci.sst.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Data item that represents the file type of a source file. The source
 * files have a record structure where each record describes an observation.
 * Each file type refers to one Sensor.
 *
 * @author Martin Boettcher
 */
@Entity
@Table(name = "mm_sensor")
public final class Sensor {

    private int id;
    private String name;
    private long pattern;
    private String observationType;

    Sensor() {
    }

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    // important: this is not public API
    void setId(int id) {
        this.id = id;
    }

    @Column(unique = true, nullable = false)
    public String getName() {
        return name;
    }

    // important: this is not public API
    void setName(String name) {
        this.name = name;
    }

    public long getPattern() {
        return pattern;
    }

    // important: this is not public API
    void setPattern(long pattern) {
        this.pattern = pattern;
    }

    @Column(nullable = false)
    public String getObservationType() {
        return observationType;
    }

    // important: this is not public API
    void setObservationType(String observationType) {
        this.observationType = observationType;
    }
}
