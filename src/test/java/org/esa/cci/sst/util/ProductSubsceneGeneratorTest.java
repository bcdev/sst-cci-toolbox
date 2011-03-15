package org.esa.cci.sst.util;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.cci.sst.orm.PersistenceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author Thomas Storm
 */
public class ProductSubsceneGeneratorTest {

    private ProductSubsceneGenerator generator;
    private NetcdfFileWriteable ncFile;

    @Before
    public void setUp() throws Exception {
        PersistenceManager persistenceManager = mock(PersistenceManager.class);
        generator = new ProductSubsceneGenerator(persistenceManager);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @After
    public void tearDown() throws Exception {
        new File(ncFile.getLocation()).delete();
    }

    @Ignore
    @Test
    public void testGetMatchupIds() throws Exception {
        List<Integer> matchupIds = generator.getMatchupIds("aatsr", "20100601120000", "0,0,100,100");
        for (Integer matchupId : matchupIds) {
            System.out.println("matchupId = " + matchupId);
        }
    }

    @Test
    public void testCreateNcFile() throws Exception {
        Product product = createDummyProduct();
        ncFile = generator.createNcFile("test_output", product);
        assertNotNull(ncFile);
        assertEquals(3, ncFile.getRootGroup().getVariables().size());
        List<Dimension> elephantDensityDims = ncFile.findVariable("elephant_density").getDimensions();
        assertEquals(3, elephantDensityDims.size());
        assertEquals(1, elephantDensityDims.get(0).getLength());
        assertEquals(10, elephantDensityDims.get(1).getLength());
        assertEquals(10, elephantDensityDims.get(2).getLength());
        assertEquals(2, ncFile.getGlobalAttributes().size());
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
