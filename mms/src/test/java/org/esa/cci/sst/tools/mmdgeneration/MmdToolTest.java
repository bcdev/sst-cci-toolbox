package org.esa.cci.sst.tools.mmdgeneration;


import org.esa.cci.sst.ColumnRegistry;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.orm.MatchupQueryParameter;
import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

public class MmdToolTest {

    @Test
    public void testCreateNetCDFWriter() throws IOException {
        final Configuration configuration = new Configuration();
        configuration.put(Configuration.KEY_MMS_MMD_TARGET_DIR, "here/we");
        configuration.put(Configuration.KEY_MMS_MMD_TARGET_FILENAME, "are_now.nc");

        final NetcdfFileWriter netCDFWriter = MmdTool.createNetCDFWriter(configuration);
        assertNotNull(netCDFWriter);
        assertEquals(NetcdfFileWriter.Version.netcdf4, netCDFWriter.getVersion());
        final NetcdfFile netcdfFile = netCDFWriter.getNetcdfFile();
        assertEquals(toPath("here", "we", "are_now.nc"), netcdfFile.getLocation());
    }

    @Test
    public void testCreateNetCDFWriter_withConfigurationDefaultValues() throws IOException {
        final Configuration configuration = new Configuration();

        final NetcdfFileWriter netCDFWriter = MmdTool.createNetCDFWriter(configuration);
        assertNotNull(netCDFWriter);
        assertEquals(NetcdfFileWriter.Version.netcdf4, netCDFWriter.getVersion());
        final NetcdfFile netcdfFile = netCDFWriter.getNetcdfFile();
        assertEquals(toPath(".", "mmd.nc"), netcdfFile.getLocation());
    }

    @Test
    public void testInitializeDimensionNames_emptyNameList() {
        final ColumnRegistry columnRegistry = new ColumnRegistry();
        final ArrayList<String> nameList = new ArrayList<>();

        final TreeSet<String> dimensionNames = MmdTool.initializeDimensionNames(nameList, columnRegistry);
        assertNotNull(dimensionNames);
        assertEquals(0, dimensionNames.size());
    }

    @Test
    public void testInitializeDimensionNames() {
        final ColumnRegistry columnRegistry = new ColumnRegistry();
        columnRegistry.register(new ColumnBuilder().name("Heike").dimensions("a b c").rank(3).build());
        columnRegistry.register(new ColumnBuilder().name("Klaus").dimensions("left right").rank(2).build());

        final ArrayList<String> nameList = new ArrayList<>();
        nameList.add("Klaus");
        nameList.add("Heike");

        final TreeSet<String> dimensionNames = MmdTool.initializeDimensionNames(nameList, columnRegistry);
        assertNotNull(dimensionNames);
        assertEquals(5, dimensionNames.size());
        assertThat(dimensionNames, hasItem("left"));
        assertThat(dimensionNames, hasItem("b"));
    }

    @Test
    public void testInitializeDimensionNames_emptyDimensionTriggersException() {
        final ColumnRegistry columnRegistry = new ColumnRegistry();
        columnRegistry.register(new ColumnBuilder().name("Heike").dimensions("").rank(0).build());

        final ArrayList<String> nameList = new ArrayList<>();
        nameList.add("Heike");

        try {
            MmdTool.initializeDimensionNames(nameList, columnRegistry);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    @Test
    public void testGetPattern() {
        final Configuration config = new Configuration();
        config.put("mms.target.pattern", "10000");

        final int pattern = MmdTool.getPattern(config);
        assertEquals(65536, pattern);   // which is 10000 to the base of 16 tb 2014-03-10
    }

    @Test
    public void testGetPattern_usesDefaultWhenValueInConfigNotSet() {
        final Configuration config = new Configuration();

        final int pattern = MmdTool.getPattern(config);
        assertEquals(0, pattern);
    }

    @Test
    public void testGetPattern_throwsWhenValueIsUnparseable() {
        final Configuration config = new Configuration();
        config.put("mms.target.pattern", "oooopsi");

        try {
            MmdTool.getPattern(config);
            fail("ToolException expected");
        } catch (ToolException expected) {
            assertEquals("Property 'mms.target.pattern' must be set to an integral number.", expected.getMessage());
        }
    }

    @Test
    public void testGetCondition() {
        final Configuration config = new Configuration();
        config.put("mms.target.condition", "the_condition");

        final String condition = MmdTool.getCondition(config);
        assertEquals("the_condition", condition);
    }

    @Test
    public void testGetCondition_returnsNullWhenNotPresentInConfig() {
        final Configuration config = new Configuration();

        final String condition = MmdTool.getCondition(config);
        assertNull(condition);
    }

    @Test
    public void testGetStartTime() {
        final Configuration configuration = new Configuration();
        configuration.put(Constants.PROPERTY_TARGET_START_TIME, "1992-02-03T00:00:00Z");

        final Date startTime = MmdTool.getStartTime(configuration);
        assertNotNull(startTime);
        assertCorrectDate(1992, 2, 3, startTime);
    }

    @Test
    public void testGetStartTime_throwsWhenValueNotInConfig() {
        final Configuration configuration = new Configuration();

        try {
            MmdTool.getStartTime(configuration);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    @Test
    public void testGetStopTime() {
        final Configuration configuration = new Configuration();
        configuration.put(Constants.PROPERTY_TARGET_STOP_TIME, "1993-03-04T00:00:00Z");

        final Date stopTime = MmdTool.getStopTime(configuration);
        assertNotNull(stopTime);
        assertCorrectDate(1993, 3, 4, stopTime);
    }

    @Test
    public void testGetStopTime_throwsWhenValueNotInConfig() {
        final Configuration configuration = new Configuration();

        try {
            MmdTool.getStopTime(configuration);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    @Test
    public void testCreateMatchupQueryParameter() {
        final Configuration config = new Configuration();
        config.put(Constants.PROPERTY_TARGET_START_TIME, "1993-03-04T00:00:00Z");
        config.put(Constants.PROPERTY_TARGET_STOP_TIME, "1994-05-05T00:00:00Z");
        config.put("mms.target.condition", "another_condition");
        config.put("mms.target.pattern", "108");

        final MatchupQueryParameter parameter = MmdTool.createMatchupQueryParameter(config);
        assertNotNull(parameter);

        final Date startDate = parameter.getStartDate();
        assertCorrectDate(1993, 3, 4, startDate);

        final Date stopDate = parameter.getStopDate();
        assertCorrectDate(1994, 5, 5, stopDate);

        assertEquals("another_condition", parameter.getCondition());
        assertEquals(264, parameter.getPattern());  // remember, it's hex
    }

    @Test
    public void testExtractVariableList()  {
        final ColumnRegistry columnRegistry = new ColumnRegistry();
        columnRegistry.register(new ColumnBuilder().name("Heike").dimensions("a b c").rank(3).build());
        columnRegistry.register(new ColumnBuilder().name("Klaus").dimensions("left right").rank(2).build());
        columnRegistry.register(new ColumnBuilder().name("Carmen").dimensions("up down").rank(2).build());

        final ArrayList<String> targetNames = new ArrayList<>();
        targetNames.add("Heike");
        targetNames.add("Carmen");

        final List<Item> variableList = MmdTool.extractVariableList(targetNames, columnRegistry);
        assertNotNull(variableList);
        assertEquals(2, variableList.size());
        assertEquals("Heike", variableList.get(0).getName());
        assertEquals("Carmen", variableList.get(1).getName());
    }

    private String toPath(String... pathComponents) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (String component : pathComponents) {
            stringBuilder.append(component);
            stringBuilder.append(File.separator);
        }

        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    private void assertCorrectDate(int year, int month, int day, Date date) {
        final GregorianCalendar utcCalendar = TimeUtil.createUtcCalendar();
        utcCalendar.setTime(date);
        assertEquals(year, utcCalendar.get(Calendar.YEAR));
        assertEquals(month - 1, utcCalendar.get(Calendar.MONTH));
        assertEquals(day, utcCalendar.get(Calendar.DAY_OF_MONTH));
    }
}
