package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.util.PgUtil;
import org.junit.After;
import org.junit.Ignore;
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
        handler = new ProductIOHandler("any", new DefaultGeoBoundaryCalculator());
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
//        assertNotNull(observation.getLocation());
//        geometry = observation.getLocation().getGeometry();
//        assertTrue(geometry.checkConsistency());
//        assertFalse(PgUtil.isClockwise(getPoints(geometry)));
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

    @Test
    public void testCreateOriginArray() throws Exception {
        init(getResourceAsFile(AAI_RESOURCE_NAME));
        final VariableDescriptor variableDescriptor = new VariableDescriptor();
        variableDescriptor.setDimensions("ni nj");
        variableDescriptor.setDimensionRoles("ni nj");
        int[] originArray = handler.createOriginArray(0, variableDescriptor);
        assertEquals(3, originArray.length);

        variableDescriptor.setDimensions("match_up ni nj");
        variableDescriptor.setDimensionRoles("match_up ni nj");
        originArray = handler.createOriginArray(0, variableDescriptor);
        assertEquals(3, originArray.length);

        variableDescriptor.setDimensions("time");
        variableDescriptor.setDimensionRoles("time");
        originArray = handler.createOriginArray(0, variableDescriptor);
        assertEquals(2, originArray.length);

        variableDescriptor.setDimensions("match_up time");
        variableDescriptor.setDimensionRoles("match_up time");
        originArray = handler.createOriginArray(0, variableDescriptor);
        assertEquals(2, originArray.length);

        variableDescriptor.setDimensions("n ni nj");
        variableDescriptor.setDimensionRoles("match_up ni nj");
        originArray = handler.createOriginArray(0, variableDescriptor);
        assertEquals(3, originArray.length);

        variableDescriptor.setDimensions("n ni nj");
        variableDescriptor.setDimensionRoles("n ni nj");
        originArray = handler.createOriginArray(0, variableDescriptor);
        assertEquals(4, originArray.length);
    }

    @Test
    @Ignore
    public void testCreateShapeArray() throws Exception {
        init(getResourceAsFile(AAI_RESOURCE_NAME));
        // todo - call of this method results in exception (rq-20110322)
        int[] shapeArray = handler.createShapeArray(3, new int[]{11, 11});
        assertEquals(3, shapeArray.length);
        assertEquals(1, shapeArray[0]);
        assertEquals(11, shapeArray[1]);
        assertEquals(11, shapeArray[2]);

        try {
            handler.createShapeArray(2, new int[]{100000, 11, 11});
            fail();
        } catch (IllegalArgumentException expected) {
            // ok
        }
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
