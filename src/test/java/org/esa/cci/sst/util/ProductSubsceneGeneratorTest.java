package org.esa.cci.sst.util;

import org.esa.beam.framework.datamodel.*;
import org.esa.cci.sst.orm.PersistenceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.postgis.*;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @author Thomas Storm
 */
public class ProductSubsceneGeneratorTest {

    private static final String TEST_OUTPUT_FILENAME = "test_output.nc";
    private ProductSubsceneGenerator generator;
    private NetcdfFileWriteable ncFile;

    @Before
    public void setUp() throws Exception {
        PersistenceManager persistenceManager = mock(PersistenceManager.class);
        generator = new ProductSubsceneGenerator(persistenceManager, "ATSR") {
            @Override
            int getSensorDimensionSize() {
                return 11;
            }
        };
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @After
    public void tearDown() throws Exception {
        if(ncFile != null) {
            ncFile.close();
        }
        new File(TEST_OUTPUT_FILENAME).delete();
    }

    @Ignore
    @Test
    public void testGetMatchupIds() throws Exception {
        List<Integer> matchupIds = generator.getMatchupIds("aatsr", new Date(58906834), new PGgeometry(new Point("0,0,100,100")));
        for (Integer matchupId : matchupIds) {
            System.out.println("matchupId = " + matchupId);
        }
    }

    @Test
    public void testCreateNcFile() throws Exception {
        Product product = createDummyProduct();
        ncFile = generator.createNcFile(TEST_OUTPUT_FILENAME, product, createMatchupIds());
        assertNotNull(ncFile);
        assertEquals(3, ncFile.getRootGroup().getVariables().size());
        List<Dimension> elephantDensityDims = ncFile.findVariable("elephant_density").getDimensions();
        assertEquals(3, elephantDensityDims.size());
        assertEquals(3, elephantDensityDims.get(0).getLength());
        assertEquals(11, elephantDensityDims.get(1).getLength());
        assertEquals(11, elephantDensityDims.get(2).getLength());
        assertEquals(2, ncFile.getGlobalAttributes().size());
    }

    @Test
    public void testGetTimeStamp() throws Exception {
        Product product = createDummyProduct();
        try {
            generator.getTimeStamp(product);
            fail();
        } catch (IllegalArgumentException expected) {
            System.out.println("expected.getMessage() = " + expected.getMessage());
        }

        ProductData.UTC start = ProductData.UTC.parse("20090125", "yyyyMMdd");
        product.setStartTime(start);
        Date timeStamp = generator.getTimeStamp(product);
        assertEquals(timeStamp.getTime(), start.getAsDate().getTime());

        // todo - ts - this may fail when more accurate time stamp is computed
        start = ProductData.UTC.parse("20110101", "yyyyMMdd");
        ProductData.UTC end = ProductData.UTC.parse("20130101", "yyyyMMdd");
        product.setStartTime(start);
        product.setEndTime(end);
        timeStamp = generator.getTimeStamp(product);
        assertEquals(ProductData.UTC.parse("2012010112", "yyyyMMddHH").getAsDate().getTime(), timeStamp.getTime());

    }

    @Test
    public void testCreateShape() throws Exception {
        Rectangle bounds = new Rectangle(25, 50);
        int[] shape = generator.createShape(5, bounds);
        assertEquals(5, shape[0]);
        assertEquals(25, shape[1]);
        assertEquals(50, shape[2]);
    }

    @Test
    public void testCreateRegionString() throws Exception {
        PGgeometry geometry = generator.createRegion(new GeoPos(48.5f, 8.3f), new GeoPos(49.5f, 9.3f));
        Point[] expectedPoints = new Point[4];
        expectedPoints[0] = new Point(48.5, 8.3);
        expectedPoints[1] = new Point(48.5, 9.3);
        expectedPoints[2] = new Point(49.5, 9.3);
        expectedPoints[3] = new Point(49.5, 8.3);
        assertTrue(geometry.getGeometry() instanceof MultiPoint );
        Point[] computedPoints = ((MultiPoint) geometry.getGeometry()).getPoints();
        for (int i = 0; i < computedPoints.length; i++) {
            assertEquals(computedPoints[i].getX(), expectedPoints[i].getX(), 0.0001);
        }
    }

    private ArrayList<Integer> createMatchupIds() {
        ArrayList<Integer> matchupIds = new ArrayList<Integer>();
        matchupIds.add(1);
        matchupIds.add(2);
        matchupIds.add(3);
        return matchupIds;
    }

    private Product createDummyProduct() {
        Product product = new Product("dummy_product", ProductData.TYPESTRING_INT32, 10, 10);
        product.addBand("elephant_density", ProductData.TYPE_INT16);
        product.addBand("mouse_density", ProductData.TYPE_INT8);
        product.addBand("cat_density", ProductData.TYPE_INT32);
        MetadataElement mortalKombatElement = new MetadataElement("Finishing_Moves");
        mortalKombatElement.addAttribute(
                new MetadataAttribute("Fatality", ProductData.createInstance("Up, down, left, punch"), true));

        MetadataElement diabloElement = new MetadataElement("Skills");
        diabloElement.addAttribute(
                new MetadataAttribute("Barbarian's strength", ProductData.createInstance(new int[]{Integer.MAX_VALUE}), true));

        product.getMetadataRoot().addElement(mortalKombatElement);
        product.getMetadataRoot().addElement(diabloElement);
        return product;
    }
}
