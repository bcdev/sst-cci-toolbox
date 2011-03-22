package org.esa.cci.sst.data;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DataFileTest {

    private DataFile dataFile;

    @Test
    public void testSetGetPath() {
        assertNull(dataFile.getPath());

        dataFile.setPath("a\\b");
        assertEquals("a/b", dataFile.getPath());

        dataFile.setPath(null);
        assertNull(dataFile.getPath());
    }

    @Before
    public void init() {
        dataFile = new DataFile();
    }
}
