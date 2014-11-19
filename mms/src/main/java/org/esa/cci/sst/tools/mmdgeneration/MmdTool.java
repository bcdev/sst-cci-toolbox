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
import org.esa.cci.sst.ColumnRegistry;
import org.esa.cci.sst.Predicate;
import org.esa.cci.sst.common.ExtractDefinition;
import org.esa.cci.sst.common.ExtractDefinitionBuilder;
import org.esa.cci.sst.data.*;
import org.esa.cci.sst.orm.ColumnStorage;
import org.esa.cci.sst.orm.MatchupQueryParameter;
import org.esa.cci.sst.orm.MatchupStorage;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.rules.Context;
import org.esa.cci.sst.rules.Converter;
import org.esa.cci.sst.rules.RuleException;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.ReaderCache;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;

import static ucar.nc2.NetcdfFileWriter.Version;

/**
 * Tool for writing the matchup data file.
 *
 * @author Ralf Quast
 */
public class MmdTool extends BasicTool {

    private static final String AVHRR_M02_TIME = "avhrr.m02.time";

    private final ColumnRegistry columnRegistry;
    private Map<String, Integer> dimensionConfiguration;
    private final List<String> targetColumnNames;

    private ReaderCache readerCache;
    private int matchupCount;

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
        tool.run(args);
    }

    @Override
    public void initialize() {
        super.initialize();

        final Configuration config = getConfig();
        initializeTargetColumns(config, getPersistenceManager().getColumnStorage());
        final Set<String> dimensionNames = getDimensionNames(targetColumnNames, columnRegistry);
        dimensionConfiguration = DimensionConfigurationInitializer.initialize(dimensionNames, config);

        final int readerCacheSize = config.getIntValue(Configuration.KEY_MMS_MMD_READER_CACHE_SIZE, 10);
        readerCache = new ReaderCache(readerCacheSize, config, logger);
    }

    private void run(String[] args) {
        MmdWriter mmdWriter = null;

        try {
            final boolean performWork = setCommandLineArgs(args);
            if (!performWork) {
                return;
            }

            initialize();

            final Configuration config = getConfig();
            final Map<Integer, Integer> matchupIdToRecordIndexMap = createMatchupIdToRecordIndexMap();
            matchupCount = matchupIdToRecordIndexMap.size();
            final NetcdfFileWriter writer = createNetCDFWriter(config);
            mmdWriter = prepareMmdWriter(writer);
            if (matchupCount == 0) {
                return;
            }
            writeMmdFile(mmdWriter, matchupIdToRecordIndexMap);
        } catch (ToolException e) {
            getErrorHandler().terminate(e);
        } catch (Throwable t) {
            getErrorHandler().terminate(new ToolException(t.getMessage(), t, ToolException.UNKNOWN_ERROR));
        } finally {
            readerCache.clear();
            if (mmdWriter != null) {
                try {
                    mmdWriter.close();
                } catch (IOException ignored) {
                }
            }
            getPersistenceManager().close();
        }
    }

    /**
     * Writes MMD by having the input files in the outermost loop to avoid re-opening them.
     *
     * @param mmdWriter                 The MMD writer.
     * @param matchupIdToRecordIndexMap The mapping from matchup ID to MMD record index.
     */
    void writeMmdFile(MmdWriter mmdWriter, Map<Integer, Integer> matchupIdToRecordIndexMap) {
        final List<Variable> mmdVariables = mmdWriter.getVariables();

        // group variables by sensors
        Map<String, List<Variable>> sensorMap = createVariableSensorMap(mmdVariables);
        final String[] sensorNames = createOrderedSensorNameArray(sensorMap);

        // loop over sensors, matchups ordered by sensor files, variables of sensor
        final PersistenceManager persistenceManager = getPersistenceManager();
        final MatchupStorage matchupStorage = persistenceManager.getMatchupStorage();
        DataFile previousDataFile = null;
        final Configuration config = getConfig();

        for (String sensorName : sensorNames) {
            final List<Matchup> matchups = getMatchupsFromDb(matchupStorage, config, sensorName);

            for (final Matchup matchup : matchups) {
                try {
                    final Integer recordNo = matchupIdToRecordIndexMap.get(matchup.getId());
                    if (recordNo == null) {
                        logger.warning(
                                String.format("skipping matchup %s for update - not found in MMD", matchup.getId()));
                        continue;
                    }

                    final int targetRecordNo = recordNo;
                    final ReferenceObservation referenceObservation = matchup.getRefObs();
                    final Observation observation = findObservation(sensorName, matchup, getPersistenceManager());
                    if (observation != null && observation.getDatafile() != null &&
                            !observation.getDatafile().equals(previousDataFile)) {
                        if (previousDataFile != null) {
                            readerCache.closeReader(previousDataFile);
                        }
                        previousDataFile = observation.getDatafile();
                    }
                    final List<Variable> variables = sensorMap.get(sensorName);
                    for (final Variable variable : variables) {
                        if (observation != null) {
                            if (!isAccurateCoincidence(referenceObservation, observation)) {
                                if (variable.getShortName().equalsIgnoreCase(AVHRR_M02_TIME)) {
                                    logger.info("NO accurate coincidence for Variable " + AVHRR_M02_TIME + "and observation " + observation.getId());
                                }
                                continue;
                            }
                        }
                        final Item targetColumn = columnRegistry.getColumn(variable.getShortName());
                        final Item sourceColumn = columnRegistry.getSourceColumn(targetColumn);
                        if ("Implicit".equals(sourceColumn.getName())) {
                            final Context context = new ContextBuilder(readerCache)
                                    .matchup(matchup)
                                    .observation(observation)
                                    .targetVariable(variable)
                                    .dimensionConfiguration(dimensionConfiguration)
                                    .configuration(getConfig())
                                    .build();
                            if (variable.getShortName().equalsIgnoreCase(AVHRR_M02_TIME)) {
                                logger.info("writing implicit column " + targetColumn.getName() + " for variable " + AVHRR_M02_TIME);
                            }
                            writeImplicitColumn(mmdWriter, variable, targetRecordNo, targetColumn, context);
                        } else {
                            if (observation != null) {
                                if (variable.getShortName().equalsIgnoreCase(AVHRR_M02_TIME)) {
                                    logger.info("writing column " + targetColumn.getName() + " for variable " + AVHRR_M02_TIME + "and observation " + observation.getId());
                                }
                                writeColumn(mmdWriter, variable, targetRecordNo, targetColumn, sourceColumn,
                                        observation,
                                        referenceObservation);
                            }
                        }
                    }
                    persistenceManager.detach(matchup);
                    if (observation != null) {
                        persistenceManager.detach(observation);
                    }
                    if (referenceObservation != null) {
                        persistenceManager.detach(referenceObservation);
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

    private List<Matchup> getMatchupsFromDb(MatchupStorage matchupStorage, Configuration config, String sensorName) {
        logger.info(String.format("going to retrieve matchups for %s", sensorName));

        final MatchupQueryParameter parameter = new MatchupQueryParameter();
        parameter.setSensorName(sensorName);
        parameter.setStartDate(getStartTime(config));
        parameter.setStopDate(getStopTime(config));
        parameter.setCondition(getCondition(config));
        parameter.setPattern(getPattern(config));

        final List<Matchup> matchups = matchupStorage.getForMmd(parameter);

        logger.info(String.format("%d matchups retrieved for %s", matchups.size(), sensorName));
        return matchups;
    }

    private Map<Integer, Integer> createMatchupIdToRecordIndexMap() {
        final Configuration config = getConfig();
        final PersistenceManager persistenceManager = getPersistenceManager();
        final MatchupStorage matchupStorage = persistenceManager.getMatchupStorage();
        final MatchupQueryParameter queryParameter = createMatchupQueryParameter(config);
        final List<Matchup> matchups = matchupStorage.get(queryParameter);
        logger.info(String.format("%d matchups retrieved", matchups.size()));

        final Map<Integer, Integer> matchupIdToRecordIndexMap = new HashMap<>(matchups.size());
        for (int i = 0; i < matchups.size(); ++i) {
            matchupIdToRecordIndexMap.put(matchups.get(i).getId(), i);
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
        final ColumnRegistryInitializer columnRegistryInitializer = new ColumnRegistryInitializer(columnRegistry,
                columnStorage);
        columnRegistryInitializer.initialize();
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
                final String message = MessageFormat.format("Expected at least one dimension for target column ''{0}''.", name);
                throw new ToolException(message, ToolException.TOOL_CONFIGURATION_ERROR);
            }
            dimensionNames.addAll(Arrays.asList(dimensions.split("\\s")));
        }

        return dimensionNames;
    }


    private MmdWriter prepareMmdWriter(NetcdfFileWriter fileWriter) throws IOException {
        final List<Item> variableList = extractVariableList(targetColumnNames, columnRegistry);
        final MmdWriter mmdWriter = new MmdWriter(fileWriter);
        mmdWriter.initialize(matchupCount, dimensionConfiguration, variableList);

        return mmdWriter;
    }


    private void registerTargetColumns(Configuration config) {
        // @todo 3 tb/tb move to initializer class - need to discuss. tb 2014-03-10
        final String[] sensorNames = getSensorNames(config);
        final Predicate predicate = new SensorPredicate(sensorNames);
        final String configFilePath = config.getStringValue(Configuration.KEY_MMS_MMD_TARGET_VARIABLES);
        try {
            final List<String> names = columnRegistry.registerColumns(new File(configFilePath), predicate);

            for (String name : names) {
                if (AVHRR_M02_TIME.equalsIgnoreCase(name)) {
                    logger.info("Registered column " + AVHRR_M02_TIME);
                }
            }

            targetColumnNames.addAll(names);
        } catch (FileNotFoundException e) {
            throw new ToolException(e.getMessage(), e, ToolException.CONFIGURATION_FILE_NOT_FOUND_ERROR);
        } catch (ParseException e) {
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    // package access for testing only tb 2014-11-19
    static Observation findObservation(String sensorName, Matchup matchup, PersistenceManager persistenceManager) {
        final ReferenceObservation referenceObservation = matchup.getRefObs();
        if (sensorName.equals(referenceObservation.getSensor())) {
            return referenceObservation;
        }
        for (final Coincidence coincidence : matchup.getCoincidences()) {
            final Observation observation = coincidence.getObservation();
            if (sensorName.equals(observation.getSensor())) {
                return observation;
            } else {
                persistenceManager.detach(observation);
                persistenceManager.detach(coincidence);
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
    static NetcdfFileWriter createNetCDFWriter(Configuration config) throws IOException {
        final String mmdDirPath = config.getStringValue(Configuration.KEY_MMS_MMD_TARGET_DIR, ".");
        final String mmdFileName = config.getStringValue(Configuration.KEY_MMS_MMD_TARGET_FILENAME);
        final File mmdFile = new File(mmdDirPath, mmdFileName);
        if (mmdFile.exists()) {
            if (!mmdFile.delete()) {
                throw new IOException("unable to delete file: " + mmdFile.getAbsolutePath());
            }
        }
        return NetcdfFileWriter.createNew(Version.netcdf4_classic, mmdFile.getPath());
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
