package org.esa.cci.sst.tools.mmdgeneration;


import org.esa.cci.sst.TestHelper;
import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.ToolException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

public class DimensionConfigurationInitializerTest {

    @Test
    public void testReadDimensionProperties() {
        final String dimensionFilePath = TestHelper.getResourcePath(getClass(), "dimension.properties");
        final Configuration config = new Configuration();
        config.put(Configuration.KEY_MMS_MMD_DIMENSIONS, dimensionFilePath);

        final Properties dimensionProperties = DimensionConfigurationInitializer.readDimensionProperties(config);
        assertNotNull(dimensionProperties);
        assertEquals("unlimited", dimensionProperties.getProperty("matchup"));
        assertEquals("80", dimensionProperties.getProperty("filename_length"));
    }

    @Test
    public void testReadDimensionProperties_missingConfigValue() {
        final Configuration config = new Configuration();

        try {
            DimensionConfigurationInitializer.readDimensionProperties(config);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    @Test
    public void testParseDimensions() {
        final ArrayList<String> dimensionNames = new ArrayList<>();
        dimensionNames.add("upwards");
        final Properties dimensionProps = new Properties();
        dimensionProps.put("upwards", "72");

        final Map<String, Integer> dimensionConfiguration = DimensionConfigurationInitializer.parseDimensionConfiguration(dimensionNames, dimensionProps);
        assertNotNull(dimensionConfiguration);
        final Integer upwards = dimensionConfiguration.get("upwards");
        assertNotNull(upwards);
        assertEquals(72, upwards.intValue());
    }

    @Test
    public void testParseDimensions_matchupDimensionIsSkipped() {
        final ArrayList<String> dimensionNames = new ArrayList<>();
        dimensionNames.add(Constants.DIMENSION_NAME_MATCHUP);
        dimensionNames.add("left");
        final Properties dimensionProps = new Properties();
        dimensionProps.put("left", "73");
        dimensionProps.put(Constants.DIMENSION_NAME_MATCHUP, "unparseable_integer");

        final Map<String, Integer> dimensionConfiguration = DimensionConfigurationInitializer.parseDimensionConfiguration(dimensionNames, dimensionProps);
        assertNotNull(dimensionConfiguration);
        final Integer left = dimensionConfiguration.get("left");
        assertNotNull(left);
        assertEquals(73, left.intValue());
    }

    @Test
    public void testParseDimensions_missingDimensionInPropertiesThrows() {
        final ArrayList<String> dimensionNames = new ArrayList<>();
        dimensionNames.add("right");
        final Properties dimensionProps = new Properties();

        try {
            DimensionConfigurationInitializer.parseDimensionConfiguration(dimensionNames, dimensionProps);
            fail("ToolException expected");
        } catch (ToolException expected) {
            assertEquals("Length of dimension 'right' is not configured.", expected.getMessage());
        }
    }

    @Test
    public void testParseDimensions_unparseableDimensionValueThrows() {
        final ArrayList<String> dimensionNames = new ArrayList<>();
        dimensionNames.add("down");
        final Properties dimensionProps = new Properties();
        dimensionProps.put("down", "weired_value");

        try {
            DimensionConfigurationInitializer.parseDimensionConfiguration(dimensionNames, dimensionProps);
            fail("ToolException expected");
        } catch (ToolException expected) {
            assertEquals("Cannot parse length of dimension 'down'.", expected.getMessage());
        }
    }
}
