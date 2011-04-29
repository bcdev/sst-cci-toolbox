package org.esa.cci.sst.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Data item that represents the file type of a source file. The source
 * files have a record structure where each record describes an observation.
 * Each data file refers to one data schema.
 *
 * @author Martin Boettcher
 */
@Entity
@Table(name = "mm_sensor")
public class Sensor {

    int id;
    String name;
    long pattern;

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(unique = true, nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getPattern() {
        return pattern;
    }

    public void setPattern(long pattern) {
        this.pattern = pattern;
    }
}
