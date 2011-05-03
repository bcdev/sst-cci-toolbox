package org.esa.cci.sst.reader;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Descriptor;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.util.PgUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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

public class ProductHandlerTest {

    private static final String AMSR_RESOURCE_NAME = "20100601-AMSRE-REMSS-L2P-amsr_l2b_v05_r42970.dat-v01.nc.gz";

    private static DataFile dataFile;
    private static GzipDeflatingIOHandlerWrapper handler;
    private static AbstractProductHandler productHandler;

    @BeforeClass
    public static void init() throws IOException, URISyntaxException {
        final URL url = ProductHandlerTest.class.getResource(AMSR_RESOURCE_NAME);
        final URI uri = url.toURI();
        final File file = new File(uri);

        dataFile = new DataFile();
        dataFile.setPath(file.getPath());
        productHandler = new ProductHandler("amsre");
        handler = new GzipDeflatingIOHandlerWrapper(productHandler);
        handler.init(dataFile);
    }

    @AfterClass
    public static void clean() {
        if (handler != null) {
            handler.close();
        }
    }

    @Test
    public void testGeoCoding() throws URISyntaxException, IOException {
        final GeoCoding geoCoding = productHandler.getProduct().getGeoCoding();
        final float wantedLat = -47.9727f;
        final float wantedLon = 18.7432f;
        final GeoPos g = new GeoPos(wantedLat, wantedLon);
        final PixelPos p = new PixelPos();
        geoCoding.getPixelPos(g, p);

        assertTrue(p.isValid());

        final float actualLat = g.getLat();
        final float actualLon = g.getLon();
        final float actualDelta = delta(wantedLat, wantedLon, actualLat, actualLon);

        // check that delta for pixel position found is the least
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                p.setLocation(p.x + j, p.y + i);
                geoCoding.getGeoPos(p, g);

                assertTrue(actualDelta <= delta(wantedLat, wantedLon, g.lat, g.lon));
            }
        }
    }

    @Test
    public void testGetNumRecords() throws URISyntaxException, IOException {
        assertEquals(1, handler.getNumRecords());
    }

    @Test
    public void testReadObservation() throws IOException, InvalidRangeException, URISyntaxException {
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
    public void testGetVariableDescriptors() throws URISyntaxException, IOException {
        final Descriptor[] descriptors = handler.getVariableDescriptors();

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
}
