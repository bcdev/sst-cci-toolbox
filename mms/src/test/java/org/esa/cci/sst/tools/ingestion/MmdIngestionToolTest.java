package org.esa.cci.sst.tools.ingestion;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.SensorBuilder;
import org.esa.cci.sst.orm.Storage;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class MmdIngestionToolTest {

    private Storage mockStorage;

    @Before
    public void setUp(){
        mockStorage = mock(Storage.class);
    }

    @Test
    public void testStoreDataFile() {
        final String path = "/path/to/wherever";
        final DataFile dataFile = new DataFile(path, new SensorBuilder().name("Firlefanz").build());

        when(mockStorage.getDatafile(path)).thenReturn(null);
        when(mockStorage.store(dataFile)).thenReturn(12);

        MmdIngestionTool.storeDataFile(dataFile, mockStorage);

        verify(mockStorage, times(1)).getDatafile(path);
        verify(mockStorage, times(1)).store(dataFile);
        verifyNoMoreInteractions(mockStorage);
    }

    @Test
    public void testStoreDataFile_alreadyInDb() {
        final String path = "/path/to/wherever";
        final DataFile dataFile = new DataFile(path, new SensorBuilder().name("Firlefanz").build());

        when(mockStorage.getDatafile(path)).thenReturn(dataFile);

        try {
            MmdIngestionTool.storeDataFile(dataFile, mockStorage);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }

        verify(mockStorage, times(1)).getDatafile(path);
        verifyNoMoreInteractions(mockStorage);
    }
}
