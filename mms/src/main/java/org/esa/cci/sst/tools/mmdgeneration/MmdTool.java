/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.util.io.WildcardMatcher;
import org.esa.cci.sst.ColumnRegistry;
import org.esa.cci.sst.Predicate;
import org.esa.cci.sst.common.ExtractDefinition;
import org.esa.cci.sst.common.ExtractDefinitionBuilder;
import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.orm.ColumnStorage;
import org.esa.cci.sst.orm.MatchupQueryParameter;
import org.esa.cci.sst.orm.MatchupStorage;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.rules.Context;
import org.esa.cci.sst.rules.Converter;
import org.esa.cci.sst.rules.RuleException;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.tools.ArchiveUtils;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.matchup.MatchupIO;
import org.esa.cci.sst.util.ConfigUtil;
import org.esa.cci.sst.util.Month;
import org.esa.cci.sst.util.ReaderCache;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.*;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import static ucar.nc2.NetcdfFileWriter.Version;

/**
 * Tool for writing the matchup data file.
 *
 * @author Ralf Quast
 */
public class MmdTool extends BasicTool {

    private final ColumnRegistry columnRegistry;
    private Map<String, Integer> dimensionConfiguration;
    private final List<String> targetColumnNames;

    private List<Matchup> matchupList;
    private ReaderCache readerCache;
    private String usecaseRootPath;
    private String[] sensorNames;

    public MmdTool() {
        super("mmd-tool.sh", "0.1");

        columnRegistry = new ColumnRegistry();
        targetColumnNames = new ArrayList<>(500);
    }

    /**
     * Main method. Generates a matchup data file based on the MMDB contents. Configured by the file
     * <code>mms-config.properties</code>.
     *
     * @param args The usual command line arguments.
     */
    public static void main(String[] args) {
        final MmdTool tool = new MmdTool();
        try {
            final boolean ok = tool.setCommandLineArgs(args);
            if (!ok) {
                tool.printHelp();
                return;
            }
            tool.initialize();
            tool.run();
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Exception e) {
            tool.getErrorHandler().terminate(new ToolException(e.getMessage(), e, ToolException.UNKNOWN_ERROR));
        } finally {
            tool.cleanup();
        }
    }

    @Override
    public void initialize() {
        super.initialize();

        final Configuration config = getConfig();

        final int readerCacheSize = config.getIntValue(Configuration.KEY_MMS_MMD_READER_CACHE_SIZE, 10);

        sensorNames = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR).split(",", 2);
        usecaseRootPath = ConfigUtil.getUsecaseRootPath(config);
        readerCache = new ReaderCache(readerCacheSize, config, logger);

        initializeTargetColumns(config, getPersistenceManager().getColumnStorage());
        final Set<String> dimensionNames = getDimensionNames(targetColumnNames, columnRegistry);
        dimensionConfiguration = DimensionConfigurationInitializer.initialize(dimensionNames, config);
    }

    private void cleanup() {
        readerCache.clear();
        getPersistenceManager().close();
    }

    private void run() throws IOException {
        final Configuration config = getConfig();

        matchupList = readMatchups();
        final Map<Long, Integer> matchupIdToRecordIndexMap = createMatchupIdToRecordIndexMap();
        final int matchupCount = matchupIdToRecordIndexMap.size();
        if (matchupCount == 0) {
            throw new ToolException("No matchups to write.", ToolException.ZERO_MATCHUPS_ERROR);
        }
        final NetcdfFileWriter writer = createNetcdfFileWriter(config);
        try (MmdWriter mmdWriter = createMmdWriter(writer)) {
            writeMmdFile(mmdWriter, matchupIdToRecordIndexMap);
        }
    }

    private List<Matchup> readMatchups() throws IOException {
        final Configuration config = getConfig();

        final File[] inputFiles = globMatchingInputFiles(config, usecaseRootPath, sensorNames[0]);
        logger.info(inputFiles.length + " input files found.");

        final ArrayList<Matchup> allMatchups = new ArrayList<Matchup>();
        final long pattern = getPattern(config);

        for(final File jsonFile : inputFiles) {
            logger.info("Reading matchups from file '" + jsonFile.getName() +"'.");
            final List<Matchup> matchups = MatchupIO.read(new BufferedInputStream(new FileInputStream(jsonFile)));
            logger.info(matchups.size() +" matchups loaded.");
            for (final Matchup matchup : matchups) {
                if (matchup.getPattern() == pattern) {
                    allMatchups.add(matchup);
                }
            }
        }

        return allMatchups;
    }

    static File[] globMatchingInputFiles(Configuration config, String usecaseRootPath, String primarySensorName) throws IOException {
        final String inputType = config.getStringValue(Configuration.KEY_MMS_MMD_INPUT_TYPE);
        final Month centerMonth = ConfigUtil.getCenterMonth(Configuration.KEY_MMS_MMD_TARGET_START_TIME, Configuration.KEY_MMS_MMD_TARGET_STOP_TIME, config);

        final String wildcardPath = ArchiveUtils.createWildcardPath(usecaseRootPath, primarySensorName, inputType, centerMonth.getYear(), centerMonth.getMonth());
        final File[] files;
        try {
            files = WildcardMatcher.glob(wildcardPath);
        } catch (IOException e) {
            return new File[0];
        }
        return files;
    }

    /**
     * Writes MMD by having the input files in the outermost loop to avoid re-opening them.
     *
     * @param mmdWriter                 The MMD writer.
     * @param matchupIdToRecordIndexMap The mapping from matchup ID to MMD record index.
     */
    void writeMmdFile(MmdWriter mmdWriter, Map<Long, Integer> matchupIdToRecordIndexMap) {
        final List<Variable> mmdVariables = mmdWriter.getVariables();

        // group variables by sensors
        Map<String, List<Variable>> sensorMap = createVariableSensorMap(mmdVariables);
        final String[] sensorNames = createOrderedSensorNameArray(sensorMap);

        for (String sensorName : sensorNames) {
            final List<Matchup> sensorMatchupList = new ArrayList<>(matchupList.size());
            // TODO - make new sorted list from matchups in matchupList. Sort by filename of sensor observation
            // TODO - or something like this. As in MatchupStorage.forMmd() horrible query SQL :-(

            for (final Matchup matchup : sensorMatchupList) {
                try {
                    final Observation observation = findObservation(sensorName, matchup);
                    final ReferenceObservation referenceObservation = matchup.getRefObs();
                    final List<Variable> variables = sensorMap.get(sensorName);
                    for (final Variable variable : variables) {
                        if (observation == null || isAccurateCoincidence(referenceObservation, observation)) {
                            final Item targetColumn = columnRegistry.getColumn(variable.getShortName());
                            final Item sourceColumn = columnRegistry.getSourceColumn(targetColumn);
                            final int targetRecordNo = matchupIdToRecordIndexMap.get(matchup.getId());

                            if ("Implicit".equals(sourceColumn.getName())) {
                                final Context context = new ContextBuilder(readerCache)
                                        .matchup(matchup)
                                        .observation(observation)
                                        .targetVariable(variable)
                                        .dimensionConfiguration(dimensionConfiguration)
                                        .configuration(getConfig())
                                        .build();
                                writeImplicitColumn(mmdWriter, variable, targetRecordNo, targetColumn, context);
                            } else {
                                if (observation != null) {
                                    writeColumn(mmdWriter, variable, targetRecordNo, targetColumn, sourceColumn,
                                                observation,
                                                referenceObservation);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    final String message = MessageFormat.format("matchup {0}: {1}",
                                                                matchup.getId(),
                                                                e.getMessage());
                    throw new ToolException(message, e, ToolException.TOOL_IO_ERROR);
                }
            }
        }
    }

    private Map<Long, Integer> createMatchupIdToRecordIndexMap() {
        final Map<Long, Integer> matchupIdToRecordIndexMap = new HashMap<>(matchupList.size());
        for (int i = 0; i < matchupList.size(); ++i) {
            matchupIdToRecordIndexMap.put(matchupList.get(i).getId(), i);
        }
        return matchupIdToRecordIndexMap;
    }


    private Map<String, List<Variable>> createVariableSensorMap(List<Variable> mmdVariables) {
        final Map<String, List<Variable>> variableSensorMap = new HashMap<>();
        for (final Variable variable : mmdVariables) {
            final Item targetColumn = columnRegistry.getColumn(variable.getShortName());
            final String sensorName = targetColumn.getSensor().getName();
            if (!variableSensorMap.containsKey(sensorName)) {
                variableSensorMap.put(sensorName, new ArrayList<Variable>());
            }
            variableSensorMap.get(sensorName).add(variable);
        }
        return variableSensorMap;
    }

    private void initializeTargetColumns(Configuration config, ColumnStorage columnStorage) {
        new ColumnRegistryInitializer(columnRegistry, columnStorage).initialize();
        registerTargetColumns(config);
    }

    private boolean isAccurateCoincidence(ReferenceObservation refObs, Observation observation) throws IOException {
        final Reader observationReader = readerCache.getReader(observation.getDatafile());
        final GeoCoding geoCoding;
        try {
            geoCoding = observationReader.getGeoCoding(observation.getRecordNo());
        } catch (IOException e) {
            throw new ToolException("Unable to get geo coding.", e, ToolException.TOOL_ERROR);
        }

        if (geoCoding == null) {
            return true;
        }

        final Point location = refObs.getPoint().getGeometry().getFirstPoint();
        final GeoPos geoPos = new GeoPos((float) location.y, (float) location.x);
        final PixelPos pixelPos = new PixelPos();
        geoCoding.getPixelPos(geoPos, pixelPos);
        if (pixelPos.x < 0 || pixelPos.y < 0) {
            final String msg = String.format(
                    "Observation (id=%d) does not contain reference observation and is ignored.", observation.getId());
            logger.warning(msg);
            return false;
        }
        if (pixelPos.x >= observationReader.getElementCount() || pixelPos.y >= observationReader.getScanLineCount()) {
            final String msg = String.format(
                    "Observation (id=%d) does not contain reference observation and is ignored.", observation.getId());
            logger.warning(msg);
            return false;
        }

        return true;
    }

    private void writeImplicitColumn(MmdWriter mmdWriter, Variable variable, int targetRecordNo, Item targetColumn,
                                     Context context) {
        try {
            final Converter converter = columnRegistry.getConverter(targetColumn);
            converter.setContext(context);
            final Array targetArray = converter.apply(null);
            if (targetArray != null) {
                final int[] targetStart = new int[variable.getRank()];
                targetStart[0] = targetRecordNo;
                mmdWriter.write(variable, targetStart, targetArray);
            }
        } catch (IOException e) {
            final String message = MessageFormat.format("matchup {0}: {1}", context.getMatchup().getId(),
                                                        e.getMessage());
            throw new ToolException(message, e, ToolException.TOOL_IO_ERROR);
        } catch (RuleException | InvalidRangeException e) {
            final String message = MessageFormat.format("matchup {0}: {1}", context.getMatchup().getId(),
                                                        e.getMessage());
            throw new ToolException(message, e, ToolException.TOOL_ERROR);
        }
    }

    private void writeColumn(MmdWriter mmdWriter, Variable variable, int i, Item targetColumn, Item sourceColumn,
                             Observation observation, ReferenceObservation refObs) {
        try {
            final Reader reader = readerCache.getReader(observation.getDatafile());
            final String role = sourceColumn.getRole();
            final ExtractDefinitionBuilder builder = new ExtractDefinitionBuilder()
                    .referenceObservation(refObs)
                    .recordNo(observation.getRecordNo())
                    .shape(variable.getShape())
                    .fillValue(targetColumn.getFillValue());
            if (observation instanceof InsituObservation) {
                final int halfExtractDuration = getConfig().getIntValue(Configuration.KEY_MMS_SAMPLING_EXTRACTION_TIME);
                builder.halfExtractDuration(halfExtractDuration);
            }
            final ExtractDefinition extractDefinition = builder.build();
            final Array sourceArray = reader.read(role, extractDefinition);
            if (sourceArray != null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(MessageFormat.format("source column: {0}, {1}", sourceColumn.getName(),
                                                     sourceColumn.getRole()));
                }
                sourceColumn = reader.getColumn(role);
                if (sourceColumn == null) {
                    throw new IllegalStateException(MessageFormat.format("Unknown role ''{0}''.", role));
                }
                final Converter converter = columnRegistry.getConverter(targetColumn, sourceColumn);
                final Array targetArray = converter.apply(sourceArray);

                final int[] targetStart = new int[variable.getRank()];
                targetStart[0] = i;
                mmdWriter.write(variable, targetStart, targetArray);
            }
        } catch (IOException e) {
            final String message = MessageFormat.format("observation {0}: {1}", observation.getId(), e.getMessage());
            logger.warning(message);
        } catch (RuleException | InvalidRangeException e) {
            final String message = MessageFormat.format("observation {0}: {1}", observation.getId(), e.getMessage());
            throw new ToolException(message, e, ToolException.TOOL_ERROR);
        }
    }

    static TreeSet<String> getDimensionNames(List<String> targetColumnNames, ColumnRegistry columnRegistry) {
        final TreeSet<String> dimensionNames = new TreeSet<>();
        for (final String name : targetColumnNames) {
            final Item column = columnRegistry.getColumn(name);
            final String dimensions = column.getDimensions();
            if (dimensions.isEmpty()) {
                final String message = MessageFormat.format(
                        "Expected at least one dimension for target column ''{0}''.", name);
                throw new ToolException(message, ToolException.TOOL_CONFIGURATION_ERROR);
            }
            dimensionNames.addAll(Arrays.asList(dimensions.split("\\s")));
        }

        return dimensionNames;
    }


    private MmdWriter createMmdWriter(NetcdfFileWriter fileWriter) throws IOException {
        final List<Item> variableList = extractVariableList(targetColumnNames, columnRegistry);

        return new MmdWriter(fileWriter, matchupList.size(), dimensionConfiguration, variableList);
    }


    private void registerTargetColumns(Configuration config) {
        // @todo 3 tb/tb move to initializer class - need to discuss. tb 2014-03-10
        final Predicate predicate = new SensorPredicate(sensorNames);
        final String configFilePath = config.getStringValue(Configuration.KEY_MMS_MMD_TARGET_VARIABLES);
        try {
            final List<String> names = columnRegistry.registerColumns(new File(configFilePath), predicate);
            targetColumnNames.addAll(names);
        } catch (FileNotFoundException e) {
            throw new ToolException(e.getMessage(), e, ToolException.CONFIGURATION_FILE_NOT_FOUND_ERROR);
        } catch (ParseException e) {
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    // package access for testing only tb 2014-11-19
    static Observation findObservation(String sensorName, Matchup matchup) {
        final ReferenceObservation referenceObservation = matchup.getRefObs();
        if (sensorName.equals(referenceObservation.getSensor())) {
            return referenceObservation;
        }
        for (final Coincidence coincidence : matchup.getCoincidences()) {
            final Observation observation = coincidence.getObservation();
            if (sensorName.equals(observation.getSensor())) {
                return observation;
            }
        }
        return null;
    }

    // package access for testing only tb 2014-03-18
    static String[] createOrderedSensorNameArray(Map<String, List<Variable>> sensorMap) {
        final Set<String> keys = sensorMap.keySet();
        final String[] sensorNames = keys.toArray(new String[keys.size()]);
        Arrays.sort(sensorNames);
        return sensorNames;
    }

    // package access for testing only tb 2014-03-10
    static NetcdfFileWriter createNetcdfFileWriter(Configuration config) throws IOException {
        final String mmdDirPath = config.getStringValue(Configuration.KEY_MMS_MMD_TARGET_DIR, ".");
        final String mmdFileName = config.getStringValue(Configuration.KEY_MMS_MMD_TARGET_FILENAME);
        final File mmdFile = new File(mmdDirPath, mmdFileName);
        if (mmdFile.exists()) {
            if (!mmdFile.delete()) {
                throw new IOException("unable to delete file: " + mmdFile.getAbsolutePath());
            }
        }
        return NetcdfFileWriter.createNew(Version.netcdf3, mmdFile.getPath());
    }

    // package access for testing only tb 2014-03-11
    static String getCondition(Configuration config) {
        return config.getStringValue("mms.target.condition", null);
    }

    // package access for testing only tb 2014-03-10
    static long getPattern(Configuration config) {
        final String[] sensorNames = getSensorNames(config);
        long pattern = 0;
        for (final String sensorName : sensorNames) {
            pattern |= config.getPattern(sensorName, 0);
        }
        final String referenceSensor = config.getStringValue(Configuration.KEY_MMS_SAMPLING_REFERENCE_SENSOR, null);
        if (Constants.SENSOR_NAME_HISTORY.equalsIgnoreCase(referenceSensor)) {
            pattern |= config.getPattern(Constants.SENSOR_NAME_HISTORY);
        }
        return pattern;
    }

    // package access for testing only rq 2014-08-12
    static String[] getSensorNames(Configuration config) {
        return config.getStringValue(Configuration.KEY_MMS_MMD_SENSORS).split(",", 2);
    }

    // package access for testing only tb 2014-03-11
    static Date getStartTime(Configuration configuration) {
        return configuration.getDateValue(Configuration.KEY_MMS_MMD_TARGET_START_TIME);
    }

    // package access for testing only tb 2014-03-11
    static Date getStopTime(Configuration configuration) {
        return configuration.getDateValue(Configuration.KEY_MMS_MMD_TARGET_STOP_TIME);
    }

    // package access for testing only tb 2014-03-11
    static MatchupQueryParameter createMatchupQueryParameter(Configuration config) {
        final MatchupQueryParameter parameter = new MatchupQueryParameter();
        parameter.setStartDate(getStartTime(config));
        parameter.setStopDate(getStopTime(config));
        parameter.setCondition(getCondition(config));
        parameter.setPattern(getPattern(config));
        return parameter;
    }

    // package access for testing only tb 2014-03-12
    static List<Item> extractVariableList(List<String> targetNames, ColumnRegistry columnRegistry) {
        final List<Item> variableList = new ArrayList<>();

        for (String targetName : targetNames) {
            variableList.add(columnRegistry.getColumn(targetName));
        }

        return variableList;
    }
}
