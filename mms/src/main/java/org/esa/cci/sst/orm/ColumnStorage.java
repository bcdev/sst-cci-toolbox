package org.esa.cci.sst.orm;


import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.Item;

import java.util.List;

public interface ColumnStorage {

    Column getColumn(String columnName);

    List<Item> getAllColumns();

    List<Item> getAllColumnsWithTransaction();

    List<String> getAllColumnNames();

    List<String> getAllColumnNamesWithTransaction();

    void store(Column column);

    void storeWithTransaction(Column column);

    void deleteAll();
}
