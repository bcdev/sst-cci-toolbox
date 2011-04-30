package org.esa.cci.sst;


import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.rules.Rule;
import org.esa.cci.sst.rules.RuleFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.DataType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Illustrates how a dimension conversion can be carried out.
 *
 * @author Ralf Quast
 */
public class DimensionConversionTest {

    public static final String CALLSIGN_VARIABLE_TYPE = DataType.CHAR.name();
    public static final String CALLSIGN_VARIABLE_NAME = "callsign";

    private VariableDescriptorRegistry registry = VariableDescriptorRegistry.getInstance();

    @Test
    public void testDescriptorConversion() {
        final VariableDescriptor targetDescriptor = registry.getDescriptor(CALLSIGN_VARIABLE_NAME);

        assertEquals(CALLSIGN_VARIABLE_NAME, targetDescriptor.getName());
        assertEquals(CALLSIGN_VARIABLE_TYPE, targetDescriptor.getType());
        assertEquals("matchup callsign.length", targetDescriptor.getDimensions());

        final VariableDescriptor sourceDescriptor = registry.getSourceDescriptor(targetDescriptor);

        assertNotNull(sourceDescriptor);
        assertEquals(CALLSIGN_VARIABLE_NAME, sourceDescriptor.getName());
        assertEquals(CALLSIGN_VARIABLE_TYPE, sourceDescriptor.getType());
        assertEquals("match_up cs_length", sourceDescriptor.getDimensions());
    }

    @Before
    public void initRegistry() throws Exception {
        final Rule rule = RuleFactory.getInstance().getRule("MatchupDimension,CallsignDimension");
        final VariableDescriptor descriptor = new VariableDescriptor();
        descriptor.setName(CALLSIGN_VARIABLE_NAME);
        descriptor.setType(CALLSIGN_VARIABLE_TYPE);
        descriptor.setDimensions("match_up cs_length");

        registry.register(rule, descriptor);
    }

    @After
    public void clearRegistry() {
        registry.clear();
    }
}
