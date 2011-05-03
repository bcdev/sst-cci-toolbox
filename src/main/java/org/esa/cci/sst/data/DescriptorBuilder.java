package org.esa.cci.sst.data;

import com.bc.ceres.core.Assert;
import ucar.ma2.DataType;

/**
 * Used for building variable descriptors.
 *
 * @author Ralf Quast
 */
public final class DescriptorBuilder {

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

    public DescriptorBuilder(Descriptor descriptor) {
        setName(descriptor.getName());
        setType(DataType.valueOf(descriptor.getType()));
        setDimensions(descriptor.getDimensions());
        setUnit(descriptor.getUnit());
        setAddOffset(descriptor.getAddOffset());
        setScaleFactor(descriptor.getScaleFactor());
        setFillValue(descriptor.getFillValue());
        setValidMin(descriptor.getValidMin());
        setValidMax(descriptor.getValidMax());
        setStandardName(descriptor.getStandardName());
        setLongName(descriptor.getLongName());
        setRole(descriptor.getRole());
        setSensor(descriptor.getSensor());
    }

    public DescriptorBuilder() {
        setName("untitled");
        setType(DataType.INT);
        setDimensions("");
        setSensor(INTERNAL_SENSOR);
    }

    public String getName() {
        return name;
    }

    public DescriptorBuilder setName(String name) {
        Assert.argument(name != null, "name == null");
        this.name = name;
        return this;
    }

    public DataType getType() {
        return type;
    }

    public DescriptorBuilder setType(DataType type) {
        this.type = type;
        return this;
    }

    public String getDimensions() {
        return dimensions;
    }

    public DescriptorBuilder setDimensions(String dimensions) {
        Assert.argument(dimensions != null, "dimensions == null");
        this.dimensions = dimensions;
        return this;
    }

    public String getUnit() {
        return unit;
    }

    public DescriptorBuilder setUnit(String unit) {
        this.unit = unit;
        return this;
    }

    public Number getAddOffset() {
        return addOffset;
    }

    public DescriptorBuilder setAddOffset(Number addOffset) {
        this.addOffset = addOffset;
        return this;
    }

    public Number getScaleFactor() {
        return scaleFactor;
    }

    public DescriptorBuilder setScaleFactor(Number scaleFactor) {
        this.scaleFactor = scaleFactor;
        return this;
    }

    public Number getFillValue() {
        return fillValue;
    }

    public DescriptorBuilder setFillValue(Number fillValue) {
        this.fillValue = fillValue;
        return this;
    }

    public Number getValidMin() {
        return validMin;
    }

    public DescriptorBuilder setValidMin(Number validMin) {
        this.validMin = validMin;
        return this;
    }

    public Number getValidMax() {
        return validMax;
    }

    public DescriptorBuilder setValidMax(Number validMax) {
        this.validMax = validMax;
        return this;
    }

    public String getStandardName() {
        return standardName;
    }

    public DescriptorBuilder setStandardName(String standardName) {
        this.standardName = standardName;
        return this;
    }

    public String getLongName() {
        return longName;
    }

    public DescriptorBuilder setLongName(String longName) {
        this.longName = longName;
        return this;
    }

    public String getRole() {
        return role;
    }

    public DescriptorBuilder setRole(String role) {
        this.role = role;
        return this;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public DescriptorBuilder setSensor(Sensor sensor) {
        this.sensor = sensor;
        return this;
    }

    public Descriptor build() {
        return new VariableDescriptor(this);
    }
}
