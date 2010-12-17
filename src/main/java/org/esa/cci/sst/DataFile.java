package org.esa.cci.sst;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * TODO add API doc
 *
 * @author Martin Boettcher
 */
@Entity
public class DataFile {
    int id;
    String path;
    DataSchema dataSchema;

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public DataSchema getDataSchema() {
        return dataSchema;
    }

    public void setDataSchema(DataSchema dataSchema) {
        this.dataSchema = dataSchema;
    }
}
