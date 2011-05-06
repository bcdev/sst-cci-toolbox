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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.File;

/**
 * Data item that represents a source file with a record structure where each
 * record describes an observation. The objects are referred to in Observations
 * to identify the file.
 *
 * @author Martin Boettcher
 */
@Entity
@Table(name = "mm_datafile")
public class DataFile {

    private int id;
    private String path;
    private Sensor sensor;

    public DataFile() {
    }

    public DataFile(File file, Sensor sensor) {
        this.path = file == null ? null : file.getPath();
        this.sensor = sensor;
    }

    public DataFile(String path, Sensor sensor) {
        this.path = path;
        this.sensor = sensor;
    }

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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @ManyToOne
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
}
