package org.esa.cci.sst.reader;

import org.esa.beam.dataio.cci.sst.AaiProductReaderTest;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
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
    private static final String AMSR_RESOURCE_NAME = "20100601-AMSRE-REMSS-L2P-amsr_l2b_v05_r42970.dat-v01.nc.gz";

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
    public void testWorkAroundBeamIssue1240() throws URISyntaxException, IOException {
        init(getResourceAsFile(AAI_RESOURCE_NAME));
        final GeoCoding geoCoding = handler.getProduct().getGeoCoding();
        final float wantedLat = -47.9727f;
        final float wantedLon = 18.7432f;
        final GeoPos g = new GeoPos(wantedLat, wantedLon);
        final PixelPos p = new PixelPos();
        geoCoding.getPixelPos(g, p);

        assertTrue(p.isValid());

        final float actualLat = g.getLat();
        final float actualLon = g.getLon();
        final float actualDelta = delta(wantedLat, wantedLon, actualLat, actualLon);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                p.setLocation(p.x + j, p.y + i);
                geoCoding.getGeoPos(p, g);

                assertTrue(actualDelta <= delta(wantedLat, wantedLon, g.lat, g.lon));
            }
        }
    }

    @Test
    public void testGetNumRecords_AAI() throws URISyntaxException, IOException {
        init(getResourceAsFile(AAI_RESOURCE_NAME));

        assertEquals(1, handler.getNumRecords());
    }

    @Test
    public void testGetNumRecords_AMSR() throws URISyntaxException, IOException {
        init(getResourceAsFile(AMSR_RESOURCE_NAME));

        assertEquals(1, handler.getNumRecords());
    }

    @Test
    public void testReadObservation_AAI() throws IOException, InvalidRangeException, URISyntaxException {
        init(getResourceAsFile(AAI_RESOURCE_NAME));
        final Observation observation = handler.readObservation(0);

        assertSame(dataFile, observation.getDatafile());
    }

    @Test
    public void testReadObservation_AMSR() throws IOException, InvalidRangeException, URISyntaxException {
        init(getResourceAsFile(AMSR_RESOURCE_NAME));
        final Observation observation = handler.readObservation(0);

        assertSame(dataFile, observation.getDatafile());
        assertTrue(observation instanceof RelatedObservation);

        final RelatedObservation relatedObservation = (RelatedObservation) observation;
        assertNotNull(relatedObservation.getLocation());

        final Geometry geometry = relatedObservation.getLocation().getGeometry();

        assertTrue(geometry.checkConsistency());
        assertFalse(PgUtil.isClockwise(getPoints(geometry)));
    }

    @Test
    public void testGetVariableDescriptors_AAI() throws URISyntaxException, IOException {
        init(getResourceAsFile(AAI_RESOURCE_NAME));
        final VariableDescriptor[] descriptors = handler.getVariableDescriptors();

        assertEquals(1, descriptors.length);
    }

    @Test
    public void testGetVariableDescriptors_AMSR() throws URISyntaxException, IOException {
        init(getResourceAsFile(AMSR_RESOURCE_NAME));
        final VariableDescriptor[] descriptors = handler.getVariableDescriptors();

        assertEquals(12, descriptors.length);
    }

    private static float delta(float wantedLat, float wantedLon, float lat, float lon) {
        return (lat - wantedLat) * (lat - wantedLat) + (lon - wantedLon) * (lon - wantedLon);
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
