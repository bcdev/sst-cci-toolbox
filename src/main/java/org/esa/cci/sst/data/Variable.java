package org.esa.cci.sst.data;

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
@Table(name="mm_variable")
public class Variable {
    int id;
    DataSchema dataSchema;
    String role;
    String name;
    String type;
    String dimensions;
    String dimensionRoles;
    String units;
    String standardName;

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

    public String getDimensionRoles() {
        return dimensionRoles;
    }

    public void setDimensionRoles(String dimensionRoles) {
        this.dimensionRoles = dimensionRoles;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getStandardName() {
        return standardName;
    }

    public void setStandardName(String standardName) {
        this.standardName = standardName;
    }
}
