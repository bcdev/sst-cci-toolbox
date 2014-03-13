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
import org.esa.cci.sst.common.ExtractDefinition;
import org.esa.cci.sst.common.ExtractDefinitionBuilder;
import org.esa.cci.sst.data.*;
import org.esa.cci.sst.orm.*;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.rules.Context;
import org.esa.cci.sst.rules.Converter;
import org.esa.cci.sst.rules.RuleException;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.ReaderCache;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import javax.persistence.Query;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;

/**
 * Tool for writing the matchup data file.
 *
 * @author Ralf Quast
 */
public class MmdTool extends BasicTool {

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

        initializeColumns();

        final Configuration config = getConfig();

        final Set<String> dimensionNames = initializeDimensionNames(targetColumnNames, columnRegistry);
        dimensionConfiguration = DimensionConfigurationInitializer.initalize(dimensionNames, config);

        final int readerCacheSize = config.getIntValue(Constants.PROPERTY_TARGET_READERCACHESIZE, 10);
        readerCache = new ReaderCache(readerCacheSize, config, getLogger());
    }

    private void run(String[] args) {
        MmdWriter mmdWriter = null;

        try {
            final boolean performWork = setCommandLineArgs(args);
            if (!performWork) {
                return;
            }

            initialize();

            matchupCount = getMatchupCount();
            if (matchupCount == 0) {
                return;
            }

            final Configuration config = getConfig();

            final NetcdfFileWriter fileWriter = createNetCDFWriter(config);
            mmdWriter = prepareMmdWriter(fileWriter);

            final Map<Integer, Integer> recordOfMatchupMap = createInvertedIndexOfMatchups();

            writeMmdShuffled(mmdWriter, recordOfMatchupMap);
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

    static TreeSet<String> initializeDimensionNames(List<String> targetColumnNames, ColumnRegistry columnRegistry) {
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

    /**
     * Writes MMD by having the input files in the outermost loop to avoid re-opening them.
     *
     * @param mmdWriter       the mmd writer
     * @param recordOfMatchup inverted index of matchups and their (foreseen or existing) record numbers in the mmd
     */
    void writeMmdShuffled(MmdWriter mmdWriter, Map<Integer, Integer> recordOfMatchup) {

        final NetcdfFileWriter mmd = mmdWriter.getFileWriter();
        final List<Variable> mmdVariables = mmdWriter.getVariables();

        final Configuration config = getConfig();
        final String condition = getCondition(config);
        final int pattern = getPattern(config);
        // group variables by sensors
        Map<String, List<Variable>> sensorMap = createSensorMap(mmdVariables);
        final Set<String> keys = sensorMap.keySet();
        final String[] sensorNames = keys.toArray(new String[keys.size()]);
        Arrays.sort(sensorNames);
        // loop over sensors, matchups ordered by sensor files, variables of sensor
        DataFile previousDataFile = null;
        for (String sensorName : sensorNames) {
            String queryString;
            if ("history".equals(sensorName)) {
                // second part of union returns matchups that do not have a history observation and shall read in-situ from context MD
                queryString = "select u.id from (" +
                        // matchup (here coincidence) with history observation uses history file
                        "(select r.id id, f.path p, r.time t " +
                        "from mm_matchup m, mm_observation r, mm_coincidence c, mm_observation o, mm_datafile f " +
                        "where r.time >= ?2 and r.time < ?3 " +
                        "and m.id = r.id " +
                        "and c.matchup_id = r.id " +
                        "and c.observation_id = o.id " +
                        "and o.sensor = ?1 " +
                        "and o.datafile_id = f.id " +
                        ") union (" +
                        // matchup without history uses file of reference observation
                        "select r.id id, f.path p, r.time t " +
                        "from mm_matchup m, mm_observation r, mm_datafile f " +
                        "where r.time >= ?2 and r.time < ?3 " +
                        "and m.id = r.id " +
                        "and f.id = r.datafile_id " +
                        "and not exists ( select o.id from mm_coincidence c, mm_observation o " +
                        "where c.matchup_id = m.id " +
                        "and c.observation_id = o.id " +
                        "and o.sensor = ?1 ) " +
                        ") " +
                        "order by p, t, id) as u";

            } else if ("atsr_md".equals(sensorName) || "metop".equals(sensorName) || "avhrr_md".equals(sensorName)) {
                // second part of union introduced to access data for metop variables via refobs observation if metop is primary
                queryString = "select u.id from (" +
                        // matchup with sensor as related observation uses related observation file
                        "(select r.id id, f.path p, r.time t " +
                        "from mm_matchup m, mm_observation r, mm_coincidence c, mm_observation o, mm_datafile f " +
                        "where r.time >= ?2 and r.time < ?3 " +
                        "and m.id = r.id " +
                        "and c.matchup_id = r.id " +
                        "and c.observation_id = o.id " +
                        "and o.sensor = ?1 " +
                        "and o.datafile_id = f.id " +
                        ") union (" +
                        // matchup with sensor as reference uses refobs file
                        "select r.id id, f.path p, r.time t " +
                        "from mm_matchup m, mm_observation r, mm_datafile f " +
                        "where r.time >= ?2 and r.time < ?3 " +
                        "and r.sensor = ?1 " +
                        "and m.id = r.id " +
                        "and f.id = r.datafile_id) " +
                        "order by p, t, id) as u";

            } else if (!"Implicit".equals(sensorName)) {
                // satellite observations use related observation file
                queryString = "select r.id " +
                        "from mm_matchup m, mm_observation r, mm_coincidence c, mm_observation o, mm_datafile f " +
                        "where r.time >= ?2 and r.time < ?3 " +
                        "and m.id = r.id " +
                        "and c.matchup_id = r.id " +
                        "and c.observation_id = o.id " +
                        "and o.sensor = ?1 " +
                        "and o.datafile_id = f.id " +
                        "order by f.path, r.time, r.id";

            } else {
                // implicit rules use reference observation file
                queryString = "select r.id " +
                        "from mm_matchup m, mm_observation r, mm_datafile f " +
                        "where r.time >= ?2 and r.time < ?3 " +
                        "and m.id = r.id " +
                        "and f.id = r.datafile_id " +
                        "order by f.path, r.time, r.id";

            }
            if (condition != null) {
                if (pattern != 0) {
                    queryString = queryString.replaceAll("where r.time",
                            "where pattern & ?4 = ?4 and " + condition + " and r.time");
                } else {
                    queryString = queryString.replaceAll("where r.time", "where " + condition + " and r.time");
                }
            } else if (pattern != 0) {
                queryString = queryString.replaceAll("where r.time", "where pattern & ?4 = ?4 and r.time");
            }
            getLogger().info(String.format("going to retrieve matchups for %s", sensorName));
            final Query query = getPersistenceManager().createNativeQuery(queryString, Matchup.class);
            query.setParameter(1, sensorName);
            query.setParameter(2, getStartTime(config));
            query.setParameter(3, getStopTime(config));
            if (pattern != 0) {
                query.setParameter(4, pattern);
            }
            @SuppressWarnings("unchecked")
            List<Matchup> matchups = query.getResultList();
            getLogger().info(String.format("%d matchups retrieved for %s", matchups.size(), sensorName));
            for (final Matchup matchup : matchups) {
                try {
                    final Integer recordNo = recordOfMatchup.get(matchup.getId());
                    if (recordNo == null) {
                        getLogger().warning(
                                String.format("skipping matchup %s for update - not found in MMD", matchup.getId()));
                        continue;
                    }
                    final int targetRecordNo = recordNo;
                    final ReferenceObservation referenceObservation = matchup.getRefObs();
                    final Observation observation = findObservation(sensorName, matchup);
                    if (observation != null && observation.getDatafile() != null &&
                            !observation.getDatafile().equals(previousDataFile)) {
                        if (previousDataFile != null) {
                            readerCache.closeReader(previousDataFile);
                        }
                        previousDataFile = observation.getDatafile();
                    }
                    for (final Variable variable : sensorMap.get(sensorName)) {
                        if (observation != null) {
                            if (testCoincidenceAccurately(referenceObservation, observation)) {
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
                                    .build();
                            writeImplicitColumn(mmd, variable, targetRecordNo, targetColumn, context);
                        } else {
                            if (observation != null) {
                                writeColumn(mmd, variable, targetRecordNo, targetColumn, sourceColumn, observation,
                                        referenceObservation);
                            }
                        }
                    }
                    getPersistenceManager().detach(matchup);
                    if (observation != null) {
                        getPersistenceManager().detach(observation);
                    }
                    if (referenceObservation != null) {
                        getPersistenceManager().detach(referenceObservation);
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

    private Map<Integer, Integer> createInvertedIndexOfMatchups() {
        final Map<Integer, Integer> recordOfMatchup = new HashMap<>();

        final Configuration config = getConfig();
        final PersistenceManager persistenceManager = getPersistenceManager();
        final MatchupStorage matchupStorage = persistenceManager.getMatchupStorage();
        final MatchupQueryParameter queryParameter = createMatchupQueryParameter(config);
        final List<Matchup> matchups = matchupStorage.get(queryParameter);

        getLogger().info(String.format("%d matchups retrieved", matchups.size()));

        for (int i = 0; i < matchups.size(); ++i) {
            recordOfMatchup.put(matchups.get(i).getId(), i);
        }
        return recordOfMatchup;
    }

    private Map<String, List<Variable>> createSensorMap(List<Variable> mmdVariables) {
        Map<String, List<Variable>> sensorMap = new HashMap<>();
        for (final Variable variable : mmdVariables) {
            final Item targetColumn = columnRegistry.getColumn(variable.getShortName());
            final String sensorName = targetColumn.getSensor().getName();
            List<Variable> variables = sensorMap.get(sensorName);
            if (variables == null) {
                variables = new ArrayList<>();
                sensorMap.put(sensorName, variables);
            }
            variables.add(variable);
        }
        return sensorMap;
    }

    private void initializeColumns() {
        final ColumnStorage columnStorage = getPersistenceManager().getColumnStorage();
        final ColumnRegistryInitializer columnRegistryInitializer = new ColumnRegistryInitializer(columnRegistry, columnStorage);
        columnRegistryInitializer.initialize();

        registerTargetColumns();
    }

    private boolean testCoincidenceAccurately(ReferenceObservation refObs, Observation observation) throws IOException {
        final Reader observationReader = readerCache.getReader(observation.getDatafile(), true);
        final GeoCoding geoCoding;
        try {
            geoCoding = observationReader.getGeoCoding(observation.getRecordNo());
        } catch (IOException e) {
            throw new ToolException("Unable to get geo coding.", e, ToolException.TOOL_ERROR);
        }
        if (geoCoding == null) {
            return false;
        }
        final Point location = refObs.getPoint().getGeometry().getFirstPoint();
        final GeoPos geoPos = new GeoPos((float) location.y, (float) location.x);
        final PixelPos pixelPos = new PixelPos();
        geoCoding.getPixelPos(geoPos, pixelPos);
        if (pixelPos.x < 0 || pixelPos.y < 0) {
            final String msg = String.format(
                    "Observation (id=%d) does not contain reference observation and is ignored.", observation.getId());
            getLogger().warning(msg);
            return true;
        }
        if (pixelPos.x >= observationReader.getElementCount() || pixelPos.y >= observationReader.getScanLineCount()) {
            final String msg = String.format(
                    "Observation (id=%d) does not contain reference observation and is ignored.", observation.getId());
            getLogger().warning(msg);
            return true;
        }

        return false;
    }

    private void writeImplicitColumn(NetcdfFileWriter mmd, Variable variable, int targetRecordNo, Item targetColumn,
                                     Context context) {
        try {
            final Converter converter = columnRegistry.getConverter(targetColumn);
            converter.setContext(context);
            final Array targetArray = converter.apply(null);
            if (targetArray != null) {
                final int[] targetStart = new int[variable.getRank()];
                targetStart[0] = targetRecordNo;
                mmd.write(variable, targetStart, targetArray);
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


    private void writeColumn(NetcdfFileWriter mmd, Variable variable, int i, Item targetColumn, Item sourceColumn,
                             Observation observation, ReferenceObservation refObs) {
        try {
            final Reader reader = readerCache.getReader(observation.getDatafile(), true);
            final String role = sourceColumn.getRole();
            final ExtractDefinition extractDefinition =
                    new ExtractDefinitionBuilder()
                            .referenceObservation(refObs)
                            .recordNo(observation.getRecordNo())
                            .shape(variable.getShape())
                            .fillValue(targetColumn.getFillValue())
                            .build();
            final Array sourceArray = reader.read(role, extractDefinition);
            if (sourceArray != null) {
                if (getLogger().isLoggable(Level.FINE)) {
                    getLogger().fine(MessageFormat.format("source column: {0}, {1}", sourceColumn.getName(),
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
                mmd.write(variable, targetStart, targetArray);
            }
        } catch (IOException e) {
            final String message = MessageFormat.format("observation {0}: {1}", observation.getId(), e.getMessage());
            getLogger().warning(message);
        } catch (RuleException | InvalidRangeException e) {
            final String message = MessageFormat.format("observation {0}: {1}", observation.getId(), e.getMessage());
            throw new ToolException(message, e, ToolException.TOOL_ERROR);
        }
    }

    // package access for testing only tb 2014-03-10
    static NetcdfFileWriter createNetCDFWriter(Configuration config) throws IOException {
        final String mmdDirPath = config.getStringValue(Configuration.KEY_MMS_MMD_TARGET_DIR, ".");
        final String mmdFileName = config.getStringValue(Configuration.KEY_MMS_MMD_TARGET_FILENAME, "mmd.nc");
        final String mmdFilePath = new File(mmdDirPath, mmdFileName).getPath();

        final NetcdfFileWriter mmd = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, mmdFilePath);
        mmd.setLargeFile(true);

        return mmd;
    }


    private MmdWriter prepareMmdWriter(NetcdfFileWriter fileWriter) throws IOException {

        final List<Item> variableList = extractVariableList(targetColumnNames, columnRegistry);
        final MmdWriter mmdWriter = new MmdWriter(fileWriter);
        mmdWriter.initialize(matchupCount, dimensionConfiguration, variableList);

        return mmdWriter;
    }

    private int getMatchupCount() {
        final Configuration config = getConfig();
        final PersistenceManager persistenceManager = getPersistenceManager();
        final MatchupStorage matchupStorage = persistenceManager.getMatchupStorage();

        final MatchupQueryParameter parameter = createMatchupQueryParameter(config);
        final int matchupCount = matchupStorage.getCount(parameter);

        getLogger().info(String.format("%d matchups in time interval", matchupCount));

        return matchupCount;
    }

    // package access for testing only tb 2014-03-11
    static String getCondition(Configuration config) {
        return config.getStringValue("mms.target.condition", null);
    }

    // package access for testing only tb 2014-03-10
    static int getPattern(Configuration config) {
        try {
            return Integer.parseInt(config.getStringValue("mms.target.pattern", "0"), 16);
        } catch (NumberFormatException e) {
            throw new ToolException("Property 'mms.target.pattern' must be set to an integral number.", e,
                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    // package access for testing only tb 2014-03-11
    static Date getStartTime(Configuration configuration) {
        return configuration.getDateValue(Constants.PROPERTY_TARGET_START_TIME);
    }

    // package access for testing only tb 2014-03-11
    static Date getStopTime(Configuration configuration) {
        return configuration.getDateValue(Constants.PROPERTY_TARGET_STOP_TIME);
    }

    private void registerTargetColumns() {
        // @todo 3 tb/tb move to initializer class - need to discuss. tb 2014-03-10
        final String configFilePath = getConfig().getStringValue("mms.target.variables");
        try {
            targetColumnNames.addAll(columnRegistry.registerColumns(new File(configFilePath)));
        } catch (FileNotFoundException e) {
            throw new ToolException(e.getMessage(), e, ToolException.CONFIGURATION_FILE_NOT_FOUND_ERROR);
        } catch (ParseException e) {
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    private Observation findObservation(String sensorName, Matchup matchup) {
        final ReferenceObservation referenceObservation = matchup.getRefObs();
        if (sensorName.equals(referenceObservation.getSensor())) {
            return referenceObservation;
        }
        for (final Coincidence coincidence : matchup.getCoincidences()) {
            final Observation observation = coincidence.getObservation();
            if (sensorName.equals(observation.getSensor())) {
                return observation;
            } else {
                getPersistenceManager().detach(observation);
                getPersistenceManager().detach(coincidence);
            }
        }
        return null;
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
        final ArrayList<Item> variableList = new ArrayList<>();

        for (String targetName : targetNames) {
            variableList.add(columnRegistry.getColumn(targetName));
        }

        return variableList;
    }
}
