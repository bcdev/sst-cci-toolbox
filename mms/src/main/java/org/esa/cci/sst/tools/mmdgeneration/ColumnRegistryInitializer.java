package org.esa.cci.sst.tools.mmdgeneration;


import org.esa.cci.sst.ColumnRegistry;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.orm.ColumnStorage;

import java.util.List;

class ColumnRegistryInitializer {

    private final ColumnRegistry registry;
    private final ColumnStorage columnStorage;

    ColumnRegistryInitializer(ColumnRegistry registry, ColumnStorage storage) {
        this.registry = registry;
        this.columnStorage = storage;
    }

    void initialize() {
        final List<Item> allColumns = columnStorage.getAllColumns();
        for (Item next : allColumns) {
            registry.register(next);
        }

        registry.register(new ColumnBuilder().build());
    }
}
