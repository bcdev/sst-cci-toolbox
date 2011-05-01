package org.esa.cci.sst.tools;


import org.esa.cci.sst.DescriptorRegistry;
import org.esa.cci.sst.data.Descriptor;
import org.esa.cci.sst.data.DescriptorBuilder;
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

    public static final DataType CALLSIGN_VARIABLE_TYPE = DataType.CHAR;
    public static final String CALLSIGN_VARIABLE_NAME = "callsign";

    private DescriptorRegistry registry = DescriptorRegistry.getInstance();

    @Test
    public void testDescriptorConversion() {
        final Descriptor targetDescriptor = registry.getDescriptor(CALLSIGN_VARIABLE_NAME);

        assertEquals(CALLSIGN_VARIABLE_NAME, targetDescriptor.getName());
        assertEquals(CALLSIGN_VARIABLE_TYPE.name(), targetDescriptor.getType());
        assertEquals("matchup callsign.length", targetDescriptor.getDimensions());

        final Descriptor sourceDescriptor = registry.getSourceDescriptor(targetDescriptor);

        assertNotNull(sourceDescriptor);
        assertEquals(CALLSIGN_VARIABLE_NAME, sourceDescriptor.getName());
        assertEquals(CALLSIGN_VARIABLE_TYPE.name(), sourceDescriptor.getType());
        assertEquals("match_up cs_length", sourceDescriptor.getDimensions());
    }

    @Before
    public void initRegistry() throws Exception {
        final Rule rule = RuleFactory.getInstance().getRule("MatchupDimension,CallsignDimension");
        final DescriptorBuilder builder = new DescriptorBuilder();
        builder.setName(CALLSIGN_VARIABLE_NAME);
        builder.setType(CALLSIGN_VARIABLE_TYPE);
        builder.setDimensions("match_up cs_length");

        registry.register(rule, builder.build());
    }

    @After
    public void clearRegistry() {
        registry.clear();
    }
}
