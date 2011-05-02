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
    String observationType;

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

    @Column(nullable = false)
    public String getObservationType() {
        return observationType;
    }

    public void setObservationType(String observationType) {
        try {
            // todo - move code to builder
            @SuppressWarnings({"unchecked", "UnusedDeclaration"})
            final Class<? extends Observation> observationClass = (Class<? extends Observation>) Class.forName(
                    String.format("%s.%s", getClass().getPackage().getName(), observationType));
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        this.observationType = observationType;
    }
}
