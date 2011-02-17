package org.esa.cci.sst.reader;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.*;

public class SeaIceObservationReaderTest {

    private static final File TEST_FILE = new File("testdata/SeaIceConc", "ice_conc_sh_201006301200.hdf");
    private static final File TEST_QUALITY_FILE = new File("testdata/SeaIceConc", "ice_conc_sh_qual_201006301200.hdf");
    private SeaIceObservationReader reader;

    @Before
    public void setUp() throws Exception {
        reader = new SeaIceObservationReader(new SeaIceObservationReaderPlugIn());
    }

    @Test
    public void testRead() throws Exception {
        final Product product = ProductIO.readProduct(TEST_FILE);
        final Band band = product.getBands()[0];
        final MultiLevelImage sourceImage = band.getSourceImage();
        final DataBuffer data = sourceImage.getData().getDataBuffer();
        sourceImage.getSampleModel().getSample(0, 0, 0, data);
    }

    @Test
    public void testReadProductNodesImpl() throws Exception {
        final Product product = reader.readProductNodes(TEST_FILE, null);
        assertNotNull(product);
        final Calendar calendar = ProductData.UTC.parse("2010-06-30-12-00", "yyyy-MM-dd-HH-mm").getAsCalendar();
        assertEquals(calendar.getTimeInMillis(), product.getStartTime().getAsCalendar().getTimeInMillis());
        assertEquals(2, product.getBands().length);
        assertNotNull(product.getGeoCoding());
    }

    @Test
    public void testGetSourceFile() throws Exception {
        File sourceFile = SeaIceObservationReader.getSeaIceSourceFile(TEST_FILE.getAbsolutePath());
        assertEquals("ice_conc_sh_201006301200.hdf", sourceFile.getName());
        sourceFile = SeaIceObservationReader.getSeaIceSourceFile(TEST_QUALITY_FILE.getAbsolutePath());
        assertEquals("ice_conc_sh_201006301200.hdf", sourceFile.getName());
    }

    @Test
    public void testGetQualityFlagFile() throws Exception {
        File qualityFlagFile = SeaIceObservationReader.getQualityFlagSourceFile(TEST_FILE.getAbsolutePath());
        assertEquals("ice_conc_sh_qual_201006301200.hdf", qualityFlagFile.getName());
        qualityFlagFile = SeaIceObservationReader.getQualityFlagSourceFile(TEST_QUALITY_FILE.getAbsolutePath());
        assertEquals("ice_conc_sh_qual_201006301200.hdf", qualityFlagFile.getName());
    }

    @Test
    public void testSetStartTime() throws Exception {
        final Product dummyProduct = new Product("dummy", "dummyType", 10, 10);
        reader.setStartTime(dummyProduct, 2011, 2, 16, 11, 50);
        final ProductData.UTC startTime = dummyProduct.getStartTime();
        assertNotNull(startTime);
        final Calendar calendar = ProductData.UTC.parse("2011-02-16-11-50", "yyyy-MM-dd-HH-mm").getAsCalendar();
        assertEquals(calendar.getTimeInMillis(), startTime.getAsCalendar().getTimeInMillis());
    }

    @Test
    public void testReadObservation() throws IOException {
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
