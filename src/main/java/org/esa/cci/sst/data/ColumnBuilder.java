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

import com.bc.ceres.core.Assert;
import ucar.ma2.DataType;

/**
 * Public API for building immutable {@link Column} instances.
 *
 * @author Ralf Quast
 */
public final class ColumnBuilder {

    private static final String DIMENSIONS_PATTERN = "([a-zA-Z0-9_\\.]+){1}(\\s[a-zA-Z0-9_\\.]+)*";

    private String name;
    private DataType type;
    private boolean unsigned;
    private int rank;
    private String dimensions;
    private String unit;
    private Number addOffset;
    private Number scaleFactor;
    private Number fillValue;
    private Number validMin;
    private Number validMax;
    private String standardName;
    private String longName;
    private String role;
    private Sensor sensor;

    public ColumnBuilder() {
        setName("untitled");
        setType(DataType.INT);
        setDimensions("");
        setSensor(new SensorBuilder().build());
    }

    public ColumnBuilder(Item column) {
        setName(column.getName());
        setType(DataType.valueOf(column.getType()));
        setUnsigned(column.isUnsigned());
        setRank(column.getRank());
        setDimensions(column.getDimensions());
        setUnit(column.getUnit());
        setAddOffset(column.getAddOffset());
        setScaleFactor(column.getScaleFactor());
        setFillValue(column.getFillValue());
        setValidMin(column.getValidMin());
        setValidMax(column.getValidMax());
        setStandardName(column.getStandardName());
        setLongName(column.getLongName());
        setRole(column.getRole());
        setSensor(column.getSensor());
    }

    public ColumnBuilder setName(String name) {
        Assert.argument(name != null, "name == null");
        this.name = name;
        return this;
    }

    public ColumnBuilder setType(DataType type) {
        Assert.argument(type != null, "type == null");
        this.type = type;
        return this;
    }

    public ColumnBuilder setType(String type) {
        Assert.argument(type != null, "type == null");
        final DataType dataType = DataType.valueOf(type);
        return setType(dataType);
    }

    public ColumnBuilder setUnsigned(boolean unsigned) {
        this.unsigned = unsigned;
        return this;
    }

    public ColumnBuilder setRank(int rank) {
        Assert.argument(rank >= 0, "rank < 0");
        this.rank = rank;
        return this;
    }

    /**
     * Sets the dimensions string.
     *
     * @param dimensions The dimensions string, must either be empty or match the regular
     *                   expression {@code "([a-zA-Z0-9_\\.]+){1}(\\s[a-zA-Z0-9_\\.]+)*"}.
     *
     * @return {@code this}.
     */
    public ColumnBuilder setDimensions(String dimensions) {
        Assert.argument(dimensions != null, "dimensions == null");
        //noinspection ConstantConditions
        Assert.argument(dimensions.isEmpty() ||
                        dimensions.matches(DIMENSIONS_PATTERN),
                        "Illegal dimensions string '" + dimensions + "'.");
        this.dimensions = dimensions;
        return this;
    }

    public ColumnBuilder setUnit(String unit) {
        this.unit = unit;
        return this;
    }

    public ColumnBuilder setAddOffset(Number addOffset) {
        this.addOffset = addOffset;
        return this;
    }

    public ColumnBuilder setScaleFactor(Number scaleFactor) {
        this.scaleFactor = scaleFactor;
        return this;
    }

    public ColumnBuilder setFillValue(Number fillValue) {
        this.fillValue = fillValue;
        return this;
    }

    public ColumnBuilder setValidMin(Number validMin) {
        this.validMin = validMin;
        return this;
    }

    public ColumnBuilder setValidMax(Number validMax) {
        this.validMax = validMax;
        return this;
    }

    public ColumnBuilder setStandardName(String standardName) {
        this.standardName = standardName;
        return this;
    }

    public ColumnBuilder setLongName(String longName) {
        this.longName = longName;
        return this;
    }

    public ColumnBuilder setRole(String role) {
        this.role = role;
        return this;
    }

    public ColumnBuilder setSensor(Sensor sensor) {
        Assert.argument(sensor != null, "sensor == null");
        this.sensor = sensor;
        return this;
    }

    @SuppressWarnings({"deprecation"})
    public Item build() {
        if (rank == 0) {
            Assert.state(dimensions.isEmpty(),
                         "Number of dimensions does not match rank.");
        } else {
            Assert.state(rank == dimensions.split("\\s").length,
                         "Number of dimensions does not match rank.");
        }

        final Column column = new Column();
        column.setName(name);
        column.setType(type.name());
        column.setUnsigned(unsigned);
        column.setRank(rank);
        column.setDimensions(dimensions);
        column.setUnit(unit);
        column.setAddOffset(addOffset);
        column.setScaleFactor(scaleFactor);
        column.setFillValue(fillValue);
        column.setValidMin(validMin);
        column.setValidMax(validMax);
        column.setStandardName(standardName);
        column.setLongName(longName);
        column.setRole(role);
        column.setSensor(sensor);

        return column;
    }
}
