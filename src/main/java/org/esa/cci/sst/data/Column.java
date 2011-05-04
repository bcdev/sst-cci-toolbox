package org.esa.cci.sst.data;

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
public final class Column {

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

    Column() {
    }

    @Id
    @GeneratedValue
    int getId() {
        return id;
    }

    void setId(int id) {
        this.id = id;
    }

    @javax.persistence.Column(unique = true, nullable = false)
    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    @javax.persistence.Column(nullable = false)
    public String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
    }

    @javax.persistence.Column(nullable = false)
    public String getDimensions() {
        return dimensions;
    }

    void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public String getUnit() {
        return unit;
    }

    void setUnit(String unit) {
        this.unit = unit;
    }

    public Number getAddOffset() {
        return addOffset;
    }

    void setAddOffset(Number addOffset) {
        this.addOffset = addOffset;
    }

    public Number getScaleFactor() {
        return scaleFactor;
    }

    void setScaleFactor(Number scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public Number getFillValue() {
        return fillValue;
    }

    void setFillValue(Number fillValue) {
        this.fillValue = fillValue;
    }

    public String getStandardName() {
        return standardName;
    }

    void setStandardName(String standardName) {
        this.standardName = standardName;
    }

    public Number getValidMin() {
        return validMin;
    }

    void setValidMin(Number validMin) {
        this.validMin = validMin;
    }

    public Number getValidMax() {
        return validMax;
    }

    void setValidMax(Number validMax) {
        this.validMax = validMax;
    }

    public String getLongName() {
        return longName;
    }

    void setLongName(String longName) {
        this.longName = longName;
    }

    @ManyToOne
    @javax.persistence.Column(nullable = false)
    public Sensor getSensor() {
        return sensor;
    }

    void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    public String getRole() {
        return role;
    }

    void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object anObject) {
        return this == anObject || anObject instanceof Column && this.getId() == ((Column) anObject).getId();
    }

    @Override
    public int hashCode() {
        return 31 * getId();
    }
}