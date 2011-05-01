package org.esa.cci.sst.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Data item that represents a variable of the record structure of a source
 * file type. Variables are aggregated in a Sensor.
 *
 * @author Martin Boettcher
 */
@Entity
@Table(name = "mm_variable")
public final class VariableDescriptor implements Descriptor {

    int id;
    String name;
    String type;
    String dimensions;
    String unit;
    Number addOffset;
    Number scaleFactor;
    Number fillValue;
    Number validMin;
    Number validMax;
    String standardName;
    String longName;
    String role;
    Sensor sensor;

    public VariableDescriptor() {
    }

    VariableDescriptor(Descriptor descriptor) {
        name = descriptor.getName();
        type = descriptor.getType();
        dimensions = descriptor.getDimensions();
        unit = descriptor.getUnit();
        addOffset = descriptor.getAddOffset();
        scaleFactor = descriptor.getScaleFactor();
        fillValue = descriptor.getFillValue();
        validMin = descriptor.getValidMin();
        validMax = descriptor.getValidMax();
        standardName = descriptor.getStandardName();
        longName = descriptor.getLongName();
        role = descriptor.getRole();
        sensor = descriptor.getSensor();
    }

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    @ManyToOne
    public Sensor getSensor() {
        return sensor;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    @Override
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    @Column(unique = true, nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public Number getAddOffset() {
        return addOffset;
    }

    public void setAddOffset(Number addOffset) {
        this.addOffset = addOffset;
    }

    @Override
    public Number getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(Number scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    @Override
    public Number getFillValue() {
        return fillValue;
    }

    public void setFillValue(Number fillValue) {
        this.fillValue = fillValue;
    }

    @Override
    public String getStandardName() {
        return standardName;
    }

    public void setStandardName(String standardName) {
        this.standardName = standardName;
    }

    @Override
    public Number getValidMin() {
        return validMin;
    }

    public void setValidMin(Number validMin) {
        this.validMin = validMin;
    }

    @Override
    public Number getValidMax() {
        return validMax;
    }

    public void setValidMax(Number validMax) {
        this.validMax = validMax;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    @Override
    public String getLongName() {
        return longName;
    }

    @Override
    public boolean equals(Object anObject) {
        return this == anObject || anObject instanceof VariableDescriptor && this.getId() == ((VariableDescriptor) anObject).getId();
    }

    @Override
    public int hashCode() {
        return 31 * getId();
    }

}