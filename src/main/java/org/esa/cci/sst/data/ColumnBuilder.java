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
        name("untitled");
        type(DataType.INT);
        dimensions("");
        sensor(new SensorBuilder().build());
    }

    public ColumnBuilder(Item column) {
        name(column.getName());
        type(DataType.valueOf(column.getType()));
        unsigned(column.isUnsigned());
        rank(column.getRank());
        dimensions(column.getDimensions());
        unit(column.getUnit());
        addOffset(column.getAddOffset());
        scaleFactor(column.getScaleFactor());
        fillValue(column.getFillValue());
        validMin(column.getValidMin());
        validMax(column.getValidMax());
        standardName(column.getStandardName());
        longName(column.getLongName());
        role(column.getRole());
        sensor(column.getSensor());
    }

    public ColumnBuilder name(String name) {
        Assert.argument(name != null, "name == null");
        this.name = name;
        return this;
    }

    public ColumnBuilder type(DataType type) {
        Assert.argument(type != null, "type == null");
        this.type = type;
        return this;
    }

    public ColumnBuilder type(String type) {
        Assert.argument(type != null, "type == null");
        final DataType dataType = DataType.valueOf(type);
        return type(dataType);
    }

    public ColumnBuilder unsigned(boolean unsigned) {
        this.unsigned = unsigned;
        return this;
    }

    public ColumnBuilder rank(int rank) {
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
    public ColumnBuilder dimensions(String dimensions) {
        Assert.argument(dimensions != null, "dimensions == null");
        //noinspection ConstantConditions
        Assert.argument(dimensions.isEmpty() ||
                        dimensions.matches(DIMENSIONS_PATTERN),
                        "Illegal dimensions string '" + dimensions + "'.");
        this.dimensions = dimensions;
        return this;
    }

    public ColumnBuilder unit(String unit) {
        this.unit = unit;
        return this;
    }

    public ColumnBuilder addOffset(Number addOffset) {
        this.addOffset = addOffset;
        return this;
    }

    public ColumnBuilder scaleFactor(Number scaleFactor) {
        this.scaleFactor = scaleFactor;
        return this;
    }

    public ColumnBuilder fillValue(Number fillValue) {
        this.fillValue = fillValue;
        return this;
    }

    public ColumnBuilder validMin(Number validMin) {
        this.validMin = validMin;
        return this;
    }

    public ColumnBuilder validMax(Number validMax) {
        this.validMax = validMax;
        return this;
    }

    public ColumnBuilder standardName(String standardName) {
        this.standardName = standardName;
        return this;
    }

    public ColumnBuilder longName(String longName) {
        this.longName = longName;
        return this;
    }

    public ColumnBuilder role(String role) {
        this.role = role;
        return this;
    }

    public ColumnBuilder sensor(Sensor sensor) {
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
