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

import org.apache.openjpa.persistence.jdbc.Index;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Data item that represents a single observation that is not associated with
 * any location or time.
 *
 * @author Martin Boettcher
 */
@Entity
@Table(name = "mm_observation")
public class Observation {

    private int id;
    private String sensor;
    private DataFile datafile;
    private int recordNo;

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    // important: this is not public API
    @Deprecated
    public void setId(int id) {
        this.id = id;
    }

    @Index
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

    @SuppressWarnings({"CallToSimpleGetterFromWithinClass"})
    @Override
    public String toString() {
        return String.format("Observation(id=%d, sensor='%s\', datafile=%s, recordNo=%d)", getId(), getSensor(),
                             getDatafile(), getRecordNo());
    }
}

