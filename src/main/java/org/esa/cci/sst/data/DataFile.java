package org.esa.cci.sst.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Data item that represents a source file with a record structure where each
 * record describes an observation. The objects are referred to in Observations
 * to identify the file.
 *
 * @author Martin Boettcher
 */
@Entity
@Table(name = "mm_datafile")
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
        return slashify(path);
    }

    public void setPath(String path) {
        this.path = slashify(path);
    }

    @ManyToOne
    public DataSchema getDataSchema() {
        return dataSchema;
    }

    public void setDataSchema(DataSchema dataSchema) {
        this.dataSchema = dataSchema;
    }

    private static String slashify(String path) {
        if (path != null) {
            path = path.replace('\\', '/');
        }
        return path;
    }
}
