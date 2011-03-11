package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.Variable;
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

public class ProductObservationReaderTest {

    private static final String AAI_RESOURCE_NAME = "20100601.egr";
    private static final String AMSRE_RESOURCE_NAME = "20100601-AMSRE-REMSS-L2P-amsr_l2b_v05_r42958.dat-v01.nc.gz";

    private static DataFile dataFile;
    private static ProductObservationReader reader;

    public void init(File file) throws IOException {
        dataFile = new DataFile();
        dataFile.setPath(file.getPath());
        reader = new ProductObservationReader("any", new DefaultGeoBoundaryCalculator());
        reader.init(dataFile);
    }

    @After
    public void clean() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    @Test
    public void testGetNumRecords() throws URISyntaxException, IOException {
        init(getResourceAsFile(AMSRE_RESOURCE_NAME));
        assertEquals(1, reader.getNumRecords());
        clean();

        init(getResourceAsFile(AAI_RESOURCE_NAME));
        assertEquals(1, reader.getNumRecords());
    }

    @Test
    public void testReadObservation() throws IOException, InvalidRangeException, URISyntaxException {
        Geometry geometry;
        Observation observation;

        init(getResourceAsFile(AMSRE_RESOURCE_NAME));
        observation = reader.readObservation(0);
        assertSame(dataFile, observation.getDatafile());
        assertNotNull(((RelatedObservation) observation).getLocation());
        geometry = ((RelatedObservation) observation).getLocation().getGeometry();
        assertTrue(geometry.checkConsistency());
        assertFalse(PgUtil.isClockwise(getPoints(geometry)));
        clean();

        init(getResourceAsFile(AAI_RESOURCE_NAME));
        observation = reader.readObservation(0);
        assertSame(dataFile, observation.getDatafile());
//        assertNotNull(observation.getLocation());
//        geometry = observation.getLocation().getGeometry();
//        assertTrue(geometry.checkConsistency());
//        assertFalse(PgUtil.isClockwise(getPoints(geometry)));
    }


    @Test
    public void testGetAmsreVariables() throws URISyntaxException, IOException {
        init(getResourceAsFile(AMSRE_RESOURCE_NAME));

        final Variable[] variables = reader.getVariables();
        for (Variable variable : variables) {
            System.out.println("variable.getName() = " + variable.getName());
        }
        assertEquals(12, variables.length);
    }

    @Test
    public void testGetAaiVariables() throws URISyntaxException, IOException {
        init(getResourceAsFile(AAI_RESOURCE_NAME));

        final Variable[] variables = reader.getVariables();
        for (Variable variable : variables) {
            System.out.println("variable.getName() = " + variable.getName());
        }
        assertEquals(1, variables.length);
    }

    @Test
    public void testCreateOriginArray() throws Exception {
        init(getResourceAsFile(AAI_RESOURCE_NAME));
        final org.esa.cci.sst.data.Variable variable = new org.esa.cci.sst.data.Variable();
        variable.setDimensions("ni nj");
        variable.setDimensionRoles("ni nj");
        int[] originArray = reader.createOriginArray(0, variable);
        assertEquals(3, originArray.length);

        variable.setDimensions("match_up ni nj");
        variable.setDimensionRoles("match_up ni nj");
        originArray = reader.createOriginArray(0, variable);
        assertEquals(3, originArray.length);

        variable.setDimensions("time");
        variable.setDimensionRoles("time");
        originArray = reader.createOriginArray(0, variable);
        assertEquals(2, originArray.length);

        variable.setDimensions("match_up time");
        variable.setDimensionRoles("match_up time");
        originArray = reader.createOriginArray(0, variable);
        assertEquals(2, originArray.length);

        variable.setDimensions("n ni nj");
        variable.setDimensionRoles("match_up ni nj");
        originArray = reader.createOriginArray(0, variable);
        assertEquals(3, originArray.length);

        variable.setDimensions("n ni nj");
        variable.setDimensionRoles("n ni nj");
        originArray = reader.createOriginArray(0, variable);
        assertEquals(4, originArray.length);
    }

    @Test
    public void testCreateShapeArray() throws Exception {
        init(getResourceAsFile(AAI_RESOURCE_NAME));
        int[] shapeArray = reader.createShapeArray(3, new int[]{11, 11});
        assertEquals(3, shapeArray.length);
        assertEquals(1, shapeArray[0]);
        assertEquals(11, shapeArray[1]);
        assertEquals(11, shapeArray[2]);

        try {
            reader.createShapeArray(2, new int[]{100000, 11, 11});
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
