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

/**
 * Data item that represents a column in the record structure of a source
 * file type. Columns are aggregated in a Sensor.
 * <p/>
 * This class is used by JPA but does not belong to the public API.
 *
 * @author Martin Boettcher
 */
@Entity
@Table(name = "mm_variable")
public class Column implements Item {

    private int id;
    private String name;
    private String type;
    private boolean unsigned;
    private int rank;
    private String dimensions;
    private String unit;
    private String flagMasks;
    private String flagMeanings;
    private String flagValues;
    private Number addOffset;
    private Number scaleFactor;
    private Number fillValue;
    private Number validMin;
    private Number validMax;
    private String standardName;
    private String longName;
    private String role;
    private Sensor sensor;

    // todo - flag meanings, flag values

    /**
     * Creates a new instance of this class.
     * <p/>
     * This constructor is used by JPA but does not belong to the public API.
     *
     * @deprecated use {@link ColumnBuilder} instead.
     */
    @Deprecated
    public Column() {
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

    @Override
    @javax.persistence.Column(unique = true, nullable = false)
    public String getName() {
        return name;
    }

    // important: this is not public API
    @Deprecated
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @javax.persistence.Column(nullable = false)
    public String getType() {
        return type;
    }

    // important: this is not public API
    @Deprecated
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int getRank() {
        return rank;
    }

    // important: this is not public API
    @Deprecated
    public void setRank(int rank) {
        this.rank = rank;
    }

    @Override
    @javax.persistence.Column(nullable = false)
    public String getDimensions() {
        return dimensions;
    }

    // important: this is not public API
    @Deprecated
    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public String getFlagMasks() {
        return flagMasks;
    }

    // important: this is not public API
    @Deprecated
    public void setFlagMasks(String flagMasks) {
        this.flagMasks = flagMasks;
    }

    @Override
    public String getFlagMeanings() {
        return flagMeanings;
    }

    // important: this is not public API
    @Deprecated
    public void setFlagMeanings(String flagMeanings) {
        this.flagMeanings = flagMeanings;
    }

    @Override
    public String getFlagValues() {
        return flagValues;
    }

    // important: this is not public API
    @Deprecated
    public void setFlagValues(String flagValues) {
        this.flagValues = flagValues;
    }

    // important: this is not public API
    @Deprecated
    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public Number getAddOffset() {
        return addOffset;
    }

    // important: this is not public API
    @Deprecated
    public void setAddOffset(Number addOffset) {
        this.addOffset = addOffset;
    }

    @Override
    public Number getScaleFactor() {
        return scaleFactor;
    }

    // important: this is not public API
    @Deprecated
    public void setScaleFactor(Number scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    @Override
    public Number getFillValue() {
        return fillValue;
    }

    // important: this is not public API
    @Deprecated
    public void setFillValue(Number fillValue) {
        this.fillValue = fillValue;
    }

    @Override
    public String getStandardName() {
        return standardName;
    }

    // important: this is not public API
    @Deprecated
    public void setStandardName(String standardName) {
        this.standardName = standardName;
    }

    @Override
    public Number getValidMin() {
        return validMin;
    }

    // important: this is not public API
    @Deprecated
    public void setValidMin(Number validMin) {
        this.validMin = validMin;
    }

    @Override
    public Number getValidMax() {
        return validMax;
    }

    // important: this is not public API
    @Deprecated
    public void setValidMax(Number validMax) {
        this.validMax = validMax;
    }

    @Override
    public String getLongName() {
        return longName;
    }

    // important: this is not public API
    @Deprecated
    public void setLongName(String longName) {
        this.longName = longName;
    }

    @Override
    @ManyToOne
    @javax.persistence.Column(nullable = false)
    public Sensor getSensor() {
        return sensor;
    }

    // important: this is not public API
    @Deprecated
    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    @Override
    public String getRole() {
        return role;
    }

    // important: this is not public API
    @Deprecated
    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean isUnsigned() {
        return unsigned;
    }

    // important: this is not public API
    @Deprecated
    public void setUnsigned(boolean unsigned) {
        this.unsigned = unsigned;
    }
}