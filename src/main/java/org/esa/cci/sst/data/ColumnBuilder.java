package org.esa.cci.sst.data;

import com.bc.ceres.core.Assert;
import ucar.ma2.DataType;

/**
 * Public API used for building columns.
 *
 * @author Ralf Quast
 */
public final class ColumnBuilder {

    private static final Sensor INTERNAL_SENSOR = new SensorBuilder().build();

    private String name;
    private DataType type;
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

    public ColumnBuilder(Column column) {
        setName(column.getName());
        setType(DataType.valueOf(column.getType()));
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

    public ColumnBuilder() {
        setName("untitled");
        setType(DataType.INT);
        setDimensions("");
        setSensor(INTERNAL_SENSOR);
    }

    public String getName() {
        return name;
    }

    public ColumnBuilder setName(String name) {
        Assert.argument(name != null, "name == null");
        this.name = name;
        return this;
    }

    public DataType getType() {
        return type;
    }

    public ColumnBuilder setType(DataType type) {
        this.type = type;
        return this;
    }

    public String getDimensions() {
        return dimensions;
    }

    public ColumnBuilder setDimensions(String dimensions) {
        Assert.argument(dimensions != null, "dimensions == null");
        this.dimensions = dimensions;
        return this;
    }

    public String getUnit() {
        return unit;
    }

    public ColumnBuilder setUnit(String unit) {
        this.unit = unit;
        return this;
    }

    public Number getAddOffset() {
        return addOffset;
    }

    public ColumnBuilder setAddOffset(Number addOffset) {
        this.addOffset = addOffset;
        return this;
    }

    public Number getScaleFactor() {
        return scaleFactor;
    }

    public ColumnBuilder setScaleFactor(Number scaleFactor) {
        this.scaleFactor = scaleFactor;
        return this;
    }

    public Number getFillValue() {
        return fillValue;
    }

    public ColumnBuilder setFillValue(Number fillValue) {
        this.fillValue = fillValue;
        return this;
    }

    public Number getValidMin() {
        return validMin;
    }

    public ColumnBuilder setValidMin(Number validMin) {
        this.validMin = validMin;
        return this;
    }

    public Number getValidMax() {
        return validMax;
    }

    public ColumnBuilder setValidMax(Number validMax) {
        this.validMax = validMax;
        return this;
    }

    public String getStandardName() {
        return standardName;
    }

    public ColumnBuilder setStandardName(String standardName) {
        this.standardName = standardName;
        return this;
    }

    public String getLongName() {
        return longName;
    }

    public ColumnBuilder setLongName(String longName) {
        this.longName = longName;
        return this;
    }

    public String getRole() {
        return role;
    }

    public ColumnBuilder setRole(String role) {
        this.role = role;
        return this;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public ColumnBuilder setSensor(Sensor sensor) {
        this.sensor = sensor;
        return this;
    }

    public Column build() {
        final Column column = new Column();
        column.setName(name);
        column.setType(type.name());
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
