package org.esa.cci.sst.tools.mmdgeneration;


import org.esa.cci.sst.ColumnRegistry;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.orm.Storage;

import java.util.List;

class ColumnRegistryInitializer {

    private final ColumnRegistry registry;
    private final Storage storage;

    ColumnRegistryInitializer(ColumnRegistry registry, Storage storage) {
        this.registry = registry;
        this.storage = storage;
    }

    void initialize() {
        final List<Item> allColumns = storage.getAllColumns();
        for (Item next : allColumns) {
            registry.register(next);
        }

        registry.register(new ColumnBuilder().build());
    }
}
