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

    private int id;
    private String name;
    private String type;
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

    VariableDescriptor(DescriptorBuilder builder) {
        name = builder.getName();
        type = builder.getType().name();
        dimensions = builder.getDimensions();
        unit = builder.getUnit();
        addOffset = builder.getAddOffset();
        scaleFactor = builder.getScaleFactor();
        fillValue = builder.getFillValue();
        validMin = builder.getValidMin();
        validMax = builder.getValidMax();
        standardName = builder.getStandardName();
        longName = builder.getLongName();
        role = builder.getRole();
        sensor = builder.getSensor();
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
    @Column(unique = true, nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    @Column(nullable = false)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    @Column(nullable = false)
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
    @ManyToOne
    @Column(nullable = false)
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
    public boolean equals(Object anObject) {
        return this == anObject || anObject instanceof VariableDescriptor && this.getId() == ((VariableDescriptor) anObject).getId();
    }

    @Override
    public int hashCode() {
        return 31 * getId();
    }
}