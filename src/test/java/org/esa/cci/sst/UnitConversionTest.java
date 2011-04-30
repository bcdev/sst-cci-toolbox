package org.esa.cci.sst;

import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.rules.Converter;
import org.esa.cci.sst.rules.DescriptorRegistry;
import org.esa.cci.sst.rules.RuleException;
import org.esa.cci.sst.rules.RuleFactory;
import org.esa.cci.sst.rules.Rule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.DataType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Illustrates how a unit conversion can be carried out.
 *
 * @author Ralf Quast
 */
public class UnitConversionTest {

    private static final String TIME_VARIABLE_NAME = "time";
    private static final String TIME_VARIABLE_TYPE = DataType.DOUBLE.name();
    private static final double JULIAN_DATE_OF_EPOCH_1978 = 2443509.5;

    private DescriptorRegistry registry = DescriptorRegistry.getInstance();

    @Test
    public void testDescriptorConversion() {
        final VariableDescriptor targetDescriptor = registry.getDescriptor(TIME_VARIABLE_NAME);

        assertEquals(TIME_VARIABLE_NAME, targetDescriptor.getName());
        assertEquals(TIME_VARIABLE_TYPE, targetDescriptor.getType());
        assertEquals("seconds since 1978-01-01 00:00:00", targetDescriptor.getUnit());

        final VariableDescriptor sourceDescriptor = registry.getSourceDescriptor(targetDescriptor);

        assertNotNull(sourceDescriptor);
        assertEquals(TIME_VARIABLE_NAME, sourceDescriptor.getName());
        assertEquals(TIME_VARIABLE_TYPE, sourceDescriptor.getType());
        assertEquals("Julian Date", sourceDescriptor.getUnit());
    }

    @Test
    public void testNumericConversion() throws RuleException {
        final VariableDescriptor targetDescriptor = registry.getDescriptor(TIME_VARIABLE_NAME);

        assertNotNull(targetDescriptor);

        final Converter converter = registry.getConverter(targetDescriptor);

        assertNotNull(converter);
        assertEquals(0.0, converter.apply(JULIAN_DATE_OF_EPOCH_1978));
        assertEquals(86400.0, converter.apply(JULIAN_DATE_OF_EPOCH_1978 + 1.0));
    }

    @Before
    public void initRegistry() throws Exception {
        final Rule rule = RuleFactory.getInstance().getRule("JulianDateToSeconds");
        final VariableDescriptor descriptor = new VariableDescriptor();
        descriptor.setName(TIME_VARIABLE_NAME);
        descriptor.setType(TIME_VARIABLE_TYPE);
        descriptor.setUnit("Julian Date");

        registry.register(rule, descriptor);
    }

    @After
    public void clearRegistry() {
        registry.clear();
    }

}
