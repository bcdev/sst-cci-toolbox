package org.esa.cci.sst.tools;


import org.esa.cci.sst.ColumnRegistry;
import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.ColumnBuilder;
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

    private ColumnRegistry registry = ColumnRegistry.getInstance();

    @Test
    public void testColumnConversion() {
        final Column targetColumn = registry.getColumn(CALLSIGN_VARIABLE_NAME);

        assertEquals(CALLSIGN_VARIABLE_NAME, targetColumn.getName());
        assertEquals(CALLSIGN_VARIABLE_TYPE.name(), targetColumn.getType());
        assertEquals("matchup callsign.length", targetColumn.getDimensions());

        final Column sourceColumn = registry.getSourceColumn(targetColumn);

        assertNotNull(sourceColumn);
        assertEquals(CALLSIGN_VARIABLE_NAME, sourceColumn.getName());
        assertEquals(CALLSIGN_VARIABLE_TYPE.name(), sourceColumn.getType());
        assertEquals("match_up cs_length", sourceColumn.getDimensions());
    }

    @Before
    public void initRegistry() throws Exception {
        final Rule rule = RuleFactory.getInstance().getRule("MatchupDimension,CallsignDimension");
        final ColumnBuilder builder = new ColumnBuilder();
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
