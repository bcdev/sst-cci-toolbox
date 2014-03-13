package org.esa.cci.sst.orm;


import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.Item;

import java.util.List;

public interface ColumnStorage {

    Column getColumn(String columnName);

    List<Item> getAllColumns();

    List<String> getAllColumnNames();

    void store(Column column);
}
