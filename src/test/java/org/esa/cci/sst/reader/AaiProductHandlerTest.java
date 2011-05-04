package org.esa.cci.sst.reader;

import org.esa.beam.dataio.cci.sst.AaiProductReaderTest;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.GlobalObservation;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.VariableDescriptor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.*;

public class AaiProductHandlerTest {

    private static final String AAI_RESOURCE_NAME = "20100601.egr";

    private static DataFile dataFile;
    private static AbstractProductHandler handler;

    @BeforeClass
    public static void init() throws IOException, URISyntaxException {
        final URL url = AaiProductReaderTest.class.getResource(AAI_RESOURCE_NAME);
        final URI uri = url.toURI();
        final File file = new File(uri);

        dataFile = new DataFile();
        dataFile.setPath(file.getPath());
        handler = new AaiProductHandler("aai");
        handler.init(dataFile);
    }

    @AfterClass
    public static void clean() {
        if (handler != null) {
            handler.close();
        }
    }

    @Test
    public void testGetNumRecords() throws URISyntaxException, IOException {
        assertEquals(1, handler.getNumRecords());
    }

    @Test
    public void testReadObservation() throws IOException, InvalidRangeException, URISyntaxException {
        final Observation observation = handler.readObservation(0);

        assertTrue(observation instanceof GlobalObservation);
        assertSame(dataFile, observation.getDatafile());
    }

    @Test
    public void testGetVariableDescriptors() throws URISyntaxException, IOException {
        final VariableDescriptor[] descriptors = handler.getVariableDescriptors();

        assertEquals(1, descriptors.length);
    }
}
