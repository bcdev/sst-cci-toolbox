package org.esa.beam.dataio.cci.sst;

import org.esa.beam.common.NetcdfProductReaderTemplate;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NcAvhrrGacReaderTest {

    private Variable variable;
    private Attribute attribute_1;
    private Attribute attribute_2;
    private Band testBand;

    @Before
    public void setUp() {
        variable = mock(Variable.class);
        attribute_1 = mock(Attribute.class);
        attribute_2 = mock(Attribute.class);

        testBand = new Band("test", ProductData.TYPE_INT16, 2, 2);
        assertNull(testBand.getValidPixelExpression());
    }

    @Test
    public void testAddValidPixelExpression_doesNotOverrideExistingExpression() {
        when(variable.findAttribute("valid_min")).thenReturn(attribute_1);
        when(attribute_1.getNumericValue()).thenReturn(19.0);

        testBand.setValidPixelExpression("express yourself");

        NetcdfProductReaderTemplate.addValidPixelExpression(variable, testBand);

        assertEquals("express yourself", testBand.getValidPixelExpression());
    }

    @Test
    public void testAddValidPixelExpression_onlyMinValueSet() {
        when(variable.findAttribute("valid_min")).thenReturn(attribute_1);
        when(attribute_1.getNumericValue()).thenReturn(20.0);

        NetcdfProductReaderTemplate.addValidPixelExpression(variable, testBand);

        assertEquals("test.raw >= 20.0", testBand.getValidPixelExpression());
    }

    @Test
    public void testAddValidPixelExpression_onlyMaxValueSet() {
        when(variable.findAttribute("valid_max")).thenReturn(attribute_2);
        when(attribute_2.getNumericValue()).thenReturn(21.0);

        NetcdfProductReaderTemplate.addValidPixelExpression(variable, testBand);

        assertEquals("test.raw <= 21.0", testBand.getValidPixelExpression());
    }

    @Test
    public void testAddValidPixelExpression_minAndMaxValueSet() {
        when(variable.findAttribute("valid_min")).thenReturn(attribute_1);
        when(variable.findAttribute("valid_max")).thenReturn(attribute_2);
        when(attribute_1.getNumericValue()).thenReturn(17.0);
        when(attribute_2.getNumericValue()).thenReturn(22.0);

        NetcdfProductReaderTemplate.addValidPixelExpression(variable, testBand);

        assertEquals("test.raw >= 17.0 && test.raw <= 22.0", testBand.getValidPixelExpression());
    }

    @Test
    public void testAddFlagCoding()  {
        final Product product = new Product("test_prod", "tst_type", 2, 2);
        product.addBand(testBand);

        when(variable.findAttribute("flag_masks")).thenReturn(attribute_1);
        when(variable.findAttribute("flag_meanings")).thenReturn(attribute_2);
        when(variable.getFullName()).thenReturn("TestFlagCoding");
        when(attribute_1.getLength()).thenReturn(3);
        when(attribute_1.getNumericValue(0)).thenReturn(1);
        when(attribute_1.getNumericValue(1)).thenReturn(2);
        when(attribute_1.getNumericValue(2)).thenReturn(4);
        when(attribute_2.getStringValue()).thenReturn("flag_1 flag_2 flag_3");

        NetcdfProductReaderTemplate.addFlagCoding(variable, testBand, product);

        final FlagCoding flagCodingFromBand = testBand.getFlagCoding();
        assertNotNull(flagCodingFromBand);
        final FlagCoding flagCodingFromProduct = product.getFlagCodingGroup().get("TestFlagCoding");
        assertNotNull(flagCodingFromProduct);
        assertSame(flagCodingFromBand, flagCodingFromProduct);

        assertEquals(1, flagCodingFromBand.getFlagMask("flag_1"));
        assertEquals(2, flagCodingFromBand.getFlagMask("flag_2"));
        assertEquals(4, flagCodingFromBand.getFlagMask("flag_3"));
    }

    @Test
    public void testAddFlagCoding_missingFlagMasksAttribute()  {
        final Product product = new Product("test_prod", "tst_type", 2, 2);
        product.addBand(testBand);

        when(variable.findAttribute("flag_masks")).thenReturn(null);
        when(variable.findAttribute("flag_meanings")).thenReturn(attribute_2);
        when(variable.getFullName()).thenReturn("TestFlagCoding");

        NetcdfProductReaderTemplate.addFlagCoding(variable, testBand, product);

        assertFlagCodingNotPresent(product);
    }

    @Test
    public void testAddFlagCoding_missingFlagMeaningsAttribute()  {
        final Product product = new Product("test_prod", "tst_type", 2, 2);
        product.addBand(testBand);

        when(variable.findAttribute("flag_masks")).thenReturn(attribute_1);
        when(variable.findAttribute("flag_meanings")).thenReturn(null);
        when(variable.getFullName()).thenReturn("TestFlagCoding");

        NetcdfProductReaderTemplate.addFlagCoding(variable, testBand, product);

        assertFlagCodingNotPresent(product);
    }

    @Test
    public void testAddFlagCoding_missingBothAttributes()  {
        final Product product = new Product("test_prod", "tst_type", 2, 2);
        product.addBand(testBand);

        when(variable.findAttribute("flag_masks")).thenReturn(null);
        when(variable.findAttribute("flag_meanings")).thenReturn(null);
        when(variable.getFullName()).thenReturn("TestFlagCoding");

        NetcdfProductReaderTemplate.addFlagCoding(variable, testBand, product);

        assertFlagCodingNotPresent(product);
    }

    @Test
    public void testAddFlagCoding_attributesHaveDifferenrNumberOfValues()  {
        final Product product = new Product("test_prod", "tst_type", 2, 2);
        product.addBand(testBand);

        when(variable.findAttribute("flag_masks")).thenReturn(attribute_1);
        when(variable.findAttribute("flag_meanings")).thenReturn(attribute_2);
        when(variable.getFullName()).thenReturn("TestFlagCoding");
        when(attribute_1.getLength()).thenReturn(2);
        when(attribute_1.getNumericValue(0)).thenReturn(1);
        when(attribute_1.getNumericValue(1)).thenReturn(2);
        when(attribute_2.getStringValue()).thenReturn("flag_1 flag_2 flag_3");

        NetcdfProductReaderTemplate.addFlagCoding(variable, testBand, product);

        assertFlagCodingNotPresent(product);
    }

    private void assertFlagCodingNotPresent(Product product) {
        assertEquals(-1, product.getFlagCodingGroup().indexOf("TestFlagCoding"));
        assertNull(testBand.getFlagCoding());

    }
}
