package org.esa.cci.sst.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Data item that represents a variable of the record structure of a source
 * file type. Variables are aggregated to DataSchema.
 *
 * @author Martin Boettcher
 */
@Entity
@Table(name = "mm_variable")
public final class VariableDescriptor {

    int id;
    DataSchema dataSchema;
    String role;
    String name;
    String type;
    String dimensions;
    // todo - get rid of this field (rq-20110421)
    String dimensionRoles;
    String units;
    String standardName;
    Number addOffset;
    Number scaleFactor;
    Number fillValue;
    Number validMin;
    Number validMax;
    String longName;

    public VariableDescriptor() {
    }

    public VariableDescriptor(String name) {
        this.name = name;
    }

    public VariableDescriptor(VariableDescriptor descriptor) {
        dataSchema = descriptor.dataSchema;
        role = descriptor.role;
        name = descriptor.name;
        type = descriptor.type;
        dimensions = descriptor.dimensions;
        dimensionRoles = descriptor.dimensionRoles;
        units = descriptor.units;
        standardName = descriptor.standardName;
        addOffset = descriptor.addOffset;
        scaleFactor = descriptor.scaleFactor;
        fillValue = descriptor.fillValue;
        validMin = descriptor.validMin;
        validMax = descriptor.validMax;
        longName = descriptor.longName;
    }

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne
    public DataSchema getDataSchema() {
        return dataSchema;
    }

    public void setDataSchema(DataSchema dataSchema) {
        this.dataSchema = dataSchema;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Column(unique = true, nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    @Deprecated
    public String getDimensionRoles() {
        return dimensionRoles;
    }

    @Deprecated
    public void setDimensionRoles(String dimensionRoles) {
        this.dimensionRoles = dimensionRoles;
    }

    public String getUnit() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public Number getAddOffset() {
        return addOffset;
    }

    public void setAddOffset(Number addOffset) {
        this.addOffset = addOffset;
    }

    public Number getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(Number scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public Number getFillValue() {
        return fillValue;
    }

    public void setFillValue(Number fillValue) {
        this.fillValue = fillValue;
    }

    public String getStandardName() {
        return standardName;
    }

    public void setStandardName(String standardName) {
        this.standardName = standardName;
    }

    public Number getValidMin() {
        return validMin;
    }

    public void setValidMin(Number validMin) {
        this.validMin = validMin;
    }

    public Number getValidMax() {
        return validMax;
    }

    public void setValidMax(Number validMax) {
        this.validMax = validMax;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

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