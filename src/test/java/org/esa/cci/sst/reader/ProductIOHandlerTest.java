package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.util.PgUtil;
import org.junit.After;
import org.junit.Test;
import org.postgis.Geometry;
import org.postgis.Point;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ProductIOHandlerTest {

    private static final String AAI_RESOURCE_NAME = "20100601.egr";
    private static final String AMSRE_RESOURCE_NAME = "20100601-AMSRE-REMSS-L2P-amsr_l2b_v05_r42958.dat-v01.nc.gz";

    private static DataFile dataFile;
    private static ProductIOHandler handler;

    public void init(File file) throws IOException {
        dataFile = new DataFile();
        dataFile.setPath(file.getPath());
        handler = new ProductIOHandler("any", new DefaultBoundaryCalculator());
        handler.init(dataFile);
    }

    @After
    public void clean() {
        if (handler != null) {
            handler.close();
        }
    }

    @Test
    public void testGetNumRecords() throws URISyntaxException, IOException {
        init(getResourceAsFile(AMSRE_RESOURCE_NAME));
        assertEquals(1, handler.getNumRecords());
        clean();

        init(getResourceAsFile(AAI_RESOURCE_NAME));
        assertEquals(1, handler.getNumRecords());
    }

    @Test
    public void testReadObservation() throws IOException, InvalidRangeException, URISyntaxException {
        Geometry geometry;
        Observation observation;

        init(getResourceAsFile(AMSRE_RESOURCE_NAME));
        observation = handler.readObservation(0);
        assertSame(dataFile, observation.getDatafile());
        assertNotNull(((RelatedObservation) observation).getLocation());
        geometry = ((RelatedObservation) observation).getLocation().getGeometry();
        assertTrue(geometry.checkConsistency());
        assertFalse(PgUtil.isClockwise(getPoints(geometry)));
        clean();

        init(getResourceAsFile(AAI_RESOURCE_NAME));
        observation = handler.readObservation(0);
        assertSame(dataFile, observation.getDatafile());
    }


    @Test
    public void testGetAmsreVariables() throws URISyntaxException, IOException {
        init(getResourceAsFile(AMSRE_RESOURCE_NAME));

        final VariableDescriptor[] variableDescriptors = handler.getVariableDescriptors();
        for (VariableDescriptor variableDescriptor : variableDescriptors) {
            System.out.println("variableDescriptor.getName() = " + variableDescriptor.getName());
        }
        assertEquals(12, variableDescriptors.length);
    }

    @Test
    public void testGetAaiVariables() throws URISyntaxException, IOException {
        init(getResourceAsFile(AAI_RESOURCE_NAME));

        final VariableDescriptor[] variableDescriptors = handler.getVariableDescriptors();
        for (VariableDescriptor variableDescriptor : variableDescriptors) {
            System.out.println("variableDescriptor.getName() = " + variableDescriptor.getName());
        }
        assertEquals(1, variableDescriptors.length);
    }

    private static List<Point> getPoints(Geometry geometry) {
        final List<Point> pointList = new ArrayList<Point>(geometry.numPoints());
        for (int i = 0; i < geometry.numPoints(); i++) {
            pointList.add(geometry.getPoint(i));
        }
        return pointList;
    }

    private static File getResourceAsFile(String name) throws URISyntaxException {
        final URL url = AaiProductReaderTest.class.getResource(name);
        final URI uri = url.toURI();

        return new File(uri);
    }
}
