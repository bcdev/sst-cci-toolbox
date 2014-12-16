/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
public class Sensor {

    private int id;
    private String name;
    private long pattern;
    private String observationType;

    /**
     * Creates a new instance of this class.
     * <p/>
     * This constructor is used by JPA but does not belong to the public API.
     *
     * @deprecated use {@link SensorBuilder} instead.
     */
    @Deprecated
    public Sensor() {
    }

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    /**
     * Sets the sensor id.
     * <p/>
     * This method is used by JPA but does not belong to the public API.
     *
     * @param id The sensor id.
     *
     * @deprecated no replacement.
     */
    public void setId(int id) {
        this.id = id;
    }

    @Column(unique = true, nullable = false)
    public String getName() {
        return name;
    }

    /**
     * Sets the sensor name.
     * <p/>
     * This method is used by JPA but does not belong to the public API.
     *
     * @param name The sensor name.
     *
     * @deprecated no replacement.
     */
    @Deprecated
    public void setName(String name) {
        this.name = name;
    }

    public long getPattern() {
        return pattern;
    }

    /**
     * Sets the sensor pattern.
     * <p/>
     * This method is used by JPA but does not belong to the public API.
     *
     * @param pattern The sensor pattern.
     *
     * @deprecated no replacement.
     */
    @Deprecated
    public void setPattern(long pattern) {
        this.pattern = pattern;
    }

    @Column(nullable = false)
    public String getObservationType() {
        return observationType;
    }

    /**
     * Sets the sensor observation type.
     * <p/>
     * This method is used by JPA but does not belong to the public API.
     *
     * @param observationType The sensor observation type.
     *
     * @deprecated no replacement.
     */
    @Deprecated
    public void setObservationType(String observationType) {
        this.observationType = observationType;
    }
}
