package org.esa.cci.sst.tools.mmdgeneration;


import org.esa.cci.sst.ColumnRegistry;
import org.esa.cci.sst.data.*;
import org.esa.cci.sst.orm.MatchupQueryParameter;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MmdToolTest {

    @Test
    public void testCreateNetCDFWriter() throws IOException {
        final Configuration configuration = new Configuration();
        configuration.put(Configuration.KEY_MMS_MMD_TARGET_DIR, "here/we");
        configuration.put(Configuration.KEY_MMS_MMD_TARGET_FILENAME, "are_now.nc");

        final NetcdfFileWriter netCDFWriter = MmdTool.createNetcdfFileWriter(configuration);
        assertNotNull(netCDFWriter);
    }

    @Test
    public void testCreateNetCDFWriter_withConfigurationDefaultValues() throws IOException {
        final Configuration configuration = new Configuration();
        configuration.put("mms.target.filename", "mmd.nc");
        final NetcdfFileWriter netCDFWriter = MmdTool.createNetcdfFileWriter(configuration);
        assertNotNull(netCDFWriter);
    }

    @Test
    public void testInitializeDimensionNames_emptyNameList() {
        final ColumnRegistry columnRegistry = new ColumnRegistry();
        final List<String> nameList = new ArrayList<>();

        final Set<String> dimensionNames = MmdTool.getDimensionNames(nameList, columnRegistry);
        assertNotNull(dimensionNames);
        assertEquals(0, dimensionNames.size());
    }

    @Test
    public void testInitializeDimensionNames() {
        final ColumnRegistry columnRegistry = new ColumnRegistry();
        columnRegistry.register(new ColumnBuilder().name("Heike").dimensions("a b c").rank(3).build());
        columnRegistry.register(new ColumnBuilder().name("Klaus").dimensions("left right").rank(2).build());

        final List<String> nameList = new ArrayList<>();
        nameList.add("Klaus");
        nameList.add("Heike");

        final TreeSet<String> dimensionNames = MmdTool.getDimensionNames(nameList, columnRegistry);
        assertNotNull(dimensionNames);
        assertEquals(5, dimensionNames.size());
        assertThat(dimensionNames, hasItem("left"));
        assertThat(dimensionNames, hasItem("b"));
    }

    @Test
    public void testInitializeDimensionNames_emptyDimensionTriggersException() {
        final ColumnRegistry columnRegistry = new ColumnRegistry();
        columnRegistry.register(new ColumnBuilder().name("Heike").dimensions("").rank(0).build());

        final List<String> nameList = new ArrayList<>();
        nameList.add("Heike");

        try {
            MmdTool.getDimensionNames(nameList, columnRegistry);
            fail("ToolException expected");
        } catch (ToolException expected) {
            //
        }
    }

    @Test
    public void testGetPattern() {
        final Configuration config = new Configuration();
        config.put("mms.pattern.atsr.3", "10000");
        config.put(Configuration.KEY_MMS_MMD_SENSORS, "atsr.3");

        final long pattern = MmdTool.getPattern(config);
        assertEquals(0x10000L, pattern);
    }

    @Test
    public void testGetPattern_forSensorPair() {
        final Configuration config = new Configuration();
        config.put("mms.pattern.atsr.2", "08000");
        config.put("mms.pattern.atsr.3", "10000");
        config.put(Configuration.KEY_MMS_MMD_SENSORS, "atsr.3,atsr.2");

        final long pattern = MmdTool.getPattern(config);
        assertEquals(0x18000L, pattern);
    }

    @Test
    public void testGetPattern_withHistoryWhenSamplingReferenceSensorIsSet() {
        final Configuration config = new Configuration();
        config.put("mms.pattern.atsr.3", "10000");
        config.put(Configuration.KEY_MMS_MMD_SENSORS, "atsr.3");
        config.put(Configuration.KEY_MMS_SAMPLING_REFERENCE_SENSOR, Constants.SENSOR_NAME_HISTORY);
        config.put("mms.pattern.history", "4000000000000000");

        final long pattern = MmdTool.getPattern(config);
        assertEquals(0x10000L | 0x4000000000000000L, pattern);
    }

    @Test
    public void testGetPattern_usesDefaultWhenValueInConfigNotSet() {
        final Configuration config = new Configuration();
        config.put(Configuration.KEY_MMS_MMD_SENSORS, "atsr.3");

        final long pattern = MmdTool.getPattern(config);
        assertEquals(0, pattern);
    }

    @Test
    public void testGetPattern_throwsWhenValueCannotBeParsed() {
        final Configuration config = new Configuration();
        config.put("mms.pattern.atsr.3", "oooopsi");
        config.put(Configuration.KEY_MMS_MMD_SENSORS, "atsr.3");

        try {
            MmdTool.getPattern(config);
            fail("ToolException expected");
        } catch (ToolException expected) {
            //
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
        configuration.put(Configuration.KEY_MMS_MMD_TARGET_START_TIME, "1992-02-03T00:00:00Z");

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
        configuration.put(Configuration.KEY_MMS_MMD_TARGET_STOP_TIME, "1993-03-04T00:00:00Z");

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
            //
        }
    }

    @Test
    public void testCreateMatchupQueryParameter() {
        final Configuration config = new Configuration();
        config.put(Configuration.KEY_MMS_MMD_TARGET_START_TIME, "1993-03-04T00:00:00Z");
        config.put(Configuration.KEY_MMS_MMD_TARGET_STOP_TIME, "1994-05-05T00:00:00Z");
        config.put("mms.pattern.papa", "100");
        config.put("mms.pattern.mama", "008");
        config.put(Configuration.KEY_MMS_MMD_SENSORS, "papa,mama");
        config.put("mms.target.condition", "another_condition");

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
    public void testExtractVariableList() {
        final ColumnRegistry columnRegistry = new ColumnRegistry();
        columnRegistry.register(new ColumnBuilder().name("Heike").dimensions("a b c").rank(3).build());
        columnRegistry.register(new ColumnBuilder().name("Klaus").dimensions("left right").rank(2).build());
        columnRegistry.register(new ColumnBuilder().name("Carmen").dimensions("up down").rank(2).build());

        final List<String> targetNames = new ArrayList<>();
        targetNames.add("Heike");
        targetNames.add("Carmen");

        final List<Item> variableList = MmdTool.extractVariableList(targetNames, columnRegistry);
        assertNotNull(variableList);
        assertEquals(2, variableList.size());
        assertEquals("Heike", variableList.get(0).getName());
        assertEquals("Carmen", variableList.get(1).getName());
    }

    @Test
    public void testCreateOrderedSensorNameArray() {
        final Map<String, List<Variable>> sensorMap = new HashMap<>();
        sensorMap.put("Zenzi", null);
        sensorMap.put("Franz", null);
        sensorMap.put("Kevin", null);
        sensorMap.put("Walpurga", null);

        final String[] nameArray = MmdTool.createOrderedSensorNameArray(sensorMap);
        assertEquals(4, nameArray.length);
        assertEquals("Franz", nameArray[0]);
        assertEquals("Kevin", nameArray[1]);
        assertEquals("Walpurga", nameArray[2]);
        assertEquals("Zenzi", nameArray[3]);
    }

    @Test
    public void testFindObservation_nonePresent() {
        final Matchup matchup = createMatchupWithRefObs("different");
        matchup.setCoincidences(new ArrayList<Coincidence>());

        final Observation observation = MmdTool.findObservation("dont_care", matchup);
        assertNull(observation);
    }

    @Test
    public void testFindObservation_referenceObservation() {
        final Matchup matchup = createMatchupWithRefObs("the_one");

        final Observation observation = MmdTool.findObservation("the_one", matchup);
        assertNotNull(observation);
    }

    @Test
    public void testFindObservation_fromCoincidences() {
        final Matchup matchup = createMatchupWithRefObs("different");
        addCoincidence(matchup, "the_one");

        final Observation result = MmdTool.findObservation("the_one", matchup);
        assertNotNull(result);
    }

    @Test
    public void testFindObservation_notInCoincidences_detaches() {
        final Matchup matchup = createMatchupWithRefObs("different");

        final List<Coincidence> coincidences = new ArrayList<>();
        final Observation observation = new Observation();
        observation.setSensor("also_different");

        final Coincidence coincidence = new Coincidence();
        coincidence.setObservation(observation);
        coincidences.add(coincidence);
        matchup.setCoincidences(coincidences);

        final PersistenceManager persistenceManager = mock(PersistenceManager.class);

        final Observation result = MmdTool.findObservation("the_one", matchup);
        assertNull(result);

        verify(persistenceManager, times(0)).detach(observation);
        verify(persistenceManager, times(0)).detach(coincidence);
        verifyNoMoreInteractions(persistenceManager);
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

    private Matchup createMatchupWithRefObs(String refObsSensor) {
        final Matchup matchup = new Matchup();
        final ReferenceObservation refObs = new ReferenceObservation();
        refObs.setSensor(refObsSensor);
        matchup.setRefObs(refObs);
        matchup.setCoincidences(new ArrayList<Coincidence>());
        return matchup;
    }

    private void addCoincidence(Matchup matchup, String sensorName) {
        final List<Coincidence> coincidences = new ArrayList<>();
        final Observation observation = new Observation();
        observation.setSensor(sensorName);

        final Coincidence coincidence = new Coincidence();
        coincidence.setObservation(observation);
        coincidences.add(coincidence);
        matchup.setCoincidences(coincidences);
    }
}
