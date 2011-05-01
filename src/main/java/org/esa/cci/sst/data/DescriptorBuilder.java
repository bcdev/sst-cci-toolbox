package org.esa.cci.sst.data;

import ucar.ma2.DataType;

/**
 * Used for building variable descriptors.
 *
 * @author Ralf Quast
 */
public final class DescriptorBuilder {

    private final VariableDescriptor descriptor;

    public DescriptorBuilder() {
        descriptor = new VariableDescriptor();
    }

    public DescriptorBuilder(Descriptor descriptor) {
        this.descriptor = new VariableDescriptor(descriptor);
    }

    public DescriptorBuilder setName(String name) {
        descriptor.setName(name);
        return this;
    }

    public DescriptorBuilder setType(DataType type) {
        descriptor.setType(type.name());
        return this;
    }

    public DescriptorBuilder setDimensions(String dimensions) {
        descriptor.setDimensions(dimensions);
        return this;
    }

    public DescriptorBuilder setUnit(String unit) {
        descriptor.setUnit(unit);
        return this;
    }

    public DescriptorBuilder setAddOffset(Number addOffset) {
        descriptor.setAddOffset(addOffset);
        return this;
    }

    public DescriptorBuilder setScaleFactor(Number scaleFactor) {
        descriptor.setScaleFactor(scaleFactor);
        return this;
    }

    public DescriptorBuilder setFillValue(Number fillValue) {
        descriptor.setFillValue(fillValue);
        return this;
    }

    public DescriptorBuilder setValidMin(Number validMin) {
        descriptor.setValidMin(validMin);
        return this;
    }

    public DescriptorBuilder setValidMax(Number validMax) {
        descriptor.setValidMax(validMax);
        return this;
    }

    public DescriptorBuilder setStandardName(String standardName) {
        descriptor.setStandardName(standardName);
        return this;
    }

    public DescriptorBuilder setLongName(String longName) {
        descriptor.setLongName(longName);
        return this;
    }

    public DescriptorBuilder setRole(String role) {
        descriptor.setRole(role);
        return this;
    }

    public DescriptorBuilder setSensor(Sensor sensor) {
        descriptor.setSensor(sensor);
        return this;
    }

    public Descriptor build() {
        return new VariableDescriptor(descriptor);
    }
}
