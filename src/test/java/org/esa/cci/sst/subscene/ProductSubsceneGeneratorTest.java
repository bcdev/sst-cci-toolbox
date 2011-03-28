package org.esa.cci.sst.subscene;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.cci.sst.Constants;
import org.esa.cci.sst.orm.PersistenceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

import javax.persistence.Query;
import java.awt.Rectangle;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 *
 * todo - resources required by this test are missing (rq-20110322)
 */
//@Ignore
public class ProductSubsceneGeneratorTest {

    private static final String TEST_OUTPUT_FILENAME = "test_output.nc";
    private ProductSubsceneGenerator generator;
    private NetcdfFileWriteable ncFile;

    @Before
    public void setUp() throws Exception {
        final Properties properties = new Properties();
        final InputStream stream = getClass().getResourceAsStream("test.properties");
        properties.load(stream);
        PersistenceManager persistenceManager = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME, properties);
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
        if (ncFile != null) {
            ncFile.close();
        }
        new File(TEST_OUTPUT_FILENAME).delete();
    }

    @Test
    public void testGetMatchupIds() throws Exception {
        final Date date = ProductData.UTC.parse("2010-06-01 13:21", "yyyy-MM-dd HH:mm").getAsDate();
        final LinearRing[] rings = new LinearRing[]{
                new LinearRing(new Point[]{
                        new Point(33.7, -27.0),
                        new Point(34, -27),
                        new Point(34, -28),
                        new Point(33, -28),
                        new Point(33.7, -27.0)
                })
        };
        List<Integer> matchupIds = generator.getMatchupIds("aatsr-md", date, new Polygon(rings).toString());
        for (Integer matchupId : matchupIds) {
            System.out.println("matchupId = " + matchupId);
        }
    }

    @Test
    public void testGetPoint() throws Exception {
        final Point point = generator.getPoint(getSomeExistingMatchupId());
        assertNotNull(point);
        System.out.println("point = " + point);
        try {
            generator.getPoint(-1);
            fail();
        } catch (IllegalStateException expected) {
            System.out.println("expected.getMessage() = " + expected.getMessage());
        }
    }

    private int getSomeExistingMatchupId() {
        final Query query = generator.getPersistenceManager().createQuery("SELECT m.id FROM Matchup m");
        final Object someId = query.getResultList().get(0);
        return (Integer)someId;
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
    public void testCreateRegion() throws Exception {
        String geometry = generator.createRegion(new GeoPos(48.5f, 8.3f), new GeoPos(49.5f, 9.3f));

        final String[] split = geometry.split(",");
        String current = split[0].substring(split[0].lastIndexOf("(") + 1);
        assertEquals(48.5, Double.parseDouble(current.split(" ")[0]), 0.0001);
        assertEquals(8.3, Double.parseDouble(current.split(" ")[1]), 0.0001);

        current = split[1];
        assertEquals(48.5, Double.parseDouble(current.split(" ")[0]), 0.0001);
        assertEquals(9.3, Double.parseDouble(current.split(" ")[1]), 0.0001);

        current = split[2];
        assertEquals(49.5, Double.parseDouble(current.split(" ")[0]), 0.0001);
        assertEquals(9.3, Double.parseDouble(current.split(" ")[1]), 0.0001);

        current = split[3];
        assertEquals(49.5, Double.parseDouble(current.split(" ")[0]), 0.0001);
        assertEquals(8.3, Double.parseDouble(current.split(" ")[1]), 0.0001);

        current = split[4].substring(0, split[4].lastIndexOf(")") - 1);
        assertEquals(48.5, Double.parseDouble(current.split(" ")[0]), 0.0001);
        assertEquals(8.3, Double.parseDouble(current.split(" ")[1]), 0.0001);

//      the above is only the long and, sadly, needed version for this line:
//      assertEquals("POLYGON((48.5 8.3,48.5 9.3,49.5 9.3,49.5 8.3,48.5 8.3))", geometry);
    }

    @Test
    public void testConvertGeometry() throws Exception {
        String pointCode = "0101000020E6100000000000C0AFE640400000006039803BC0";
        Point point = (Point) PGgeometry.geomFromString(pointCode);
        System.out.println("point.x = " + point.x);
        System.out.println("point.y = " + point.y);
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
                new MetadataAttribute("Barbarian's strength", ProductData.createInstance(new int[]{Integer.MAX_VALUE}),
                                      true));

        product.getMetadataRoot().addElement(mortalKombatElement);
        product.getMetadataRoot().addElement(diabloElement);
        return product;
    }
}
