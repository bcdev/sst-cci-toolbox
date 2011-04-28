package org.esa.beam.dataio.cci.sst;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.*;

public class SeaIceProductReaderTest {

    private static final File TEST_FILE = new File("testdata/SeaIceConc", "ice_conc_nh_201006301200.hdf");
    private static final File TEST_QUALITY_FILE = new File("testdata/SeaIceConc", "ice_conc_sh_qual_201006301200.hdf");
    private SeaIceProductReader reader;

    @Before
    public void setUp() throws Exception {
        reader = new SeaIceProductReader(new SeaIceProductReaderPlugIn());
    }

    @Test
    public void testReadProductNodesImpl() throws Exception {
        final Product product = reader.readProductNodes(TEST_FILE, null);
        assertNotNull(product);
        final Calendar calendar = ProductData.UTC.parse("2010-06-30-12-00", "yyyy-MM-dd-HH-mm").getAsCalendar();
        assertEquals(calendar.getTimeInMillis(), product.getStartTime().getAsCalendar().getTimeInMillis());
        assertEquals(1, product.getBands().length);
        assertNotNull(product.getGeoCoding());
        assertNotNull(product.getFileLocation());
        assertNotNull(product.getProductReader());
    }

    @Test
    public void testSetStartTime() throws Exception {
        final Product dummyProduct = new Product("dummy", "dummyType", 10, 10);
        reader.setTimes(dummyProduct, 2011, 2, 16, 11, 50);
        final ProductData.UTC startTime = dummyProduct.getStartTime();
        assertNotNull(startTime);
        final Calendar calendar = ProductData.UTC.parse("2011-02-16-11-50", "yyyy-MM-dd-HH-mm").getAsCalendar();
        assertEquals(calendar.getTimeInMillis(), startTime.getAsCalendar().getTimeInMillis());
    }

    @Test
    public void testGetMetadata() throws Exception {
        final NetcdfFile ncFile = NetcdfFile.open(TEST_FILE.getPath());
        final Variable header = ncFile.findVariable("Header");
        final MetadataElement metadata = reader.getMetadata((Structure) header);
        assertNotNull(metadata);
        assertTrue(metadata.getAttributes() != null && metadata.getAttributes().length > 0);
        assertEquals(760, metadata.getAttribute("Header.iw").getData().getElemInt());
        assertEquals(1120, metadata.getAttribute("Header.ih").getData().getElemInt());
        assertEquals(-3850.0, metadata.getAttribute("Header.Bx").getData().getElemFloat(), 0.0);
        assertEquals(5850, metadata.getAttribute("Header.By").getData().getElemFloat(), 0.0);
        assertEquals(SeaIceProductReader.NH_GRID, metadata.getAttribute("Header.area").getData().getElemString());
    }

    @Test
    public void testIsSeaIceFile() throws Exception {
        assertTrue(SeaIceProductReader.isSeaIceFile(TEST_FILE));
        assertFalse(SeaIceProductReader.isSeaIceFile(TEST_QUALITY_FILE));
    }

    @Test
    public void testGeoCoding() throws Exception {
        final NetcdfFile ncFile = NetcdfFile.open(TEST_FILE.getPath());
        final Variable header = ncFile.findVariable("Header");
        final GeoCoding geoCoding = reader.createGeoCoding((Structure) header);
        assertNotNull(geoCoding);
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixelPos = new PixelPos();
        pixelPos.setLocation(688.18774, 943.7666);
        geoCoding.getGeoPos(pixelPos, geoPos);
        assertEquals(48.483643, geoPos.lat, 0.01);
        assertEquals(-4.7993994, geoPos.lon, 0.01);

        pixelPos.setLocation(718.5, 820.5);
        geoCoding.getGeoPos(pixelPos, geoPos);
        assertEquals(53.59092, geoPos.lat, 0.5);
        assertEquals(9.785294, geoPos.lon, 0.5);
    }

    @Test
    public void testFileStructure() throws IOException {
        assertTrue(NetcdfFile.canOpen(TEST_FILE.getPath()));

        final NetcdfFile ncFile = NetcdfFile.open(TEST_FILE.getPath());
        assertNotNull(ncFile);

        final List<Variable> variableList = ncFile.getVariables();
        assertFalse(variableList.isEmpty());
        for (Variable v : variableList) {
            System.out.println("v.getName() = " + v.getName());
        }

        final Variable header = ncFile.findVariable("Header");
        assertTrue(header instanceof Structure);
        final List<Variable> headerVariables = ((Structure) header).getVariables();
        for (Variable v : headerVariables) {
            System.out.println("v.getName() = " + v.getName());
            switch (v.getDataType()) {
            case CHAR:
                System.out.println("v.value = " + v.readScalarString());
                break;
            case FLOAT:
                System.out.println("v.value = " + v.readScalarFloat());
                break;
            case INT:
                System.out.println("v.value = " + v.readScalarInt());
                break;
            case SHORT:
                System.out.println("v.value = " + v.readScalarShort());
                break;
            }
        }
        final List<Attribute> attributeList = header.getAttributes();
        for (Attribute a : attributeList) {
            System.out.println("a.getName() = " + a.getName());
            System.out.println("a.getValue() = " + a.getValue(0));
        }
        final Variable data = ncFile.findVariable("Data/data[00]");
        assertNotNull(data);

        System.out.println(ncFile.toString());
    }
}
