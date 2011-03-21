package org.esa.cci.sst.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DataFileTest {

    @Test
    public void testSetGetPath() {
        final DataFile dataFile = new DataFile();

        assertNull(dataFile.getPath());
        dataFile.setPath("a\\b");
        assertEquals("a/b", dataFile.getPath());

        dataFile.setPath(null);
        assertNull(dataFile.getPath());
    }
}
