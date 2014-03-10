package org.esa.cci.sst.tools.mmdgeneration;


import org.esa.cci.sst.ColumnRegistry;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.orm.Storage;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class ColumnRegistryInitializerTest {

    @Test
    public void testInitialize() {
        final Storage storage = mock(Storage.class);
        final List<Item> columnList = new ArrayList<>();
        columnList.add(new ColumnBuilder().name("col_1").build());
        columnList.add(new ColumnBuilder().name("col_2").build());
        when(storage.getAllColumns()).thenReturn(columnList);

        final ColumnRegistry registry = new ColumnRegistry();

        final ColumnRegistryInitializer initializer = new ColumnRegistryInitializer(registry, storage);
        initializer.initialize();

        Item item = registry.getColumn("col_1");
        assertNotNull(item);
        item = registry.getColumn("col_2");
        assertNotNull(item);

        item = registry.getColumn("Implicit");
        assertNotNull(item);

        verify(storage, times(1)).getAllColumns();
        verifyNoMoreInteractions(storage);
    }
}
