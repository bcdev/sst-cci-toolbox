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
import org.esa.cci.sst.Queries;
import org.esa.cci.sst.common.ExtractDefinition;
import org.esa.cci.sst.common.ExtractDefinitionBuilder;
import org.esa.cci.sst.data.*;
import org.esa.cci.sst.orm.Storage;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.rules.Context;
import org.esa.cci.sst.rules.Converter;
import org.esa.cci.sst.rules.RuleException;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.IoUtil;
import org.esa.cci.sst.util.ReaderCache;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import javax.persistence.Query;
import java.io.File;
import java.io.FileInputStream;
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
    private Set<String> dimensionNames;
    private final Map<String, Integer> dimensionConfiguration;
    private final List<String> targetColumnNames ;

    private ReaderCache readerCache;
    private int matchupCount;

    public MmdTool() {
        super("mmd-tool.sh", "0.1");

        columnRegistry = new ColumnRegistry();
        dimensionConfiguration = new HashMap<>(50);
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

        dimensionNames = initializeDimensionNames(targetColumnNames, columnRegistry);
        readDimensionConfiguration(dimensionNames);

        final Configuration config = getConfig();
        final int readerCacheSize = config.getIntValue(Constants.PROPERTY_TARGET_READERCACHESIZE, 10);
        readerCache = new ReaderCache(readerCacheSize, config, getLogger());
    }

    private void run(String[] args) {
        NetcdfFileWriter fileWriter = null;

        try {
            final boolean performWork = setCommandLineArgs(args);
            if (!performWork) {
                return;
            }
            initialize();

            fileWriter = createNetCDFWriter(getConfig());
            fileWriter = defineMmd(fileWriter);
            final Map<Integer, Integer> recordOfMatchupMap = createInvertedIndexOfMatchups(getCondition(),
                    getPattern());
            writeMmdShuffled(fileWriter, fileWriter.getNetcdfFile().getVariables(), recordOfMatchupMap);
        } catch (ToolException e) {
            getErrorHandler().terminate(e);
        } catch (Throwable t) {
            getErrorHandler().terminate(new ToolException(t.getMessage(), t, ToolException.UNKNOWN_ERROR));
        } finally {
            if (fileWriter != null) {
                try {
                    readerCache.clear();
                    fileWriter.close();
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

    private void initializeColumns() {
        final Storage toolStorage = getPersistenceManager().getToolStorage();
        final ColumnRegistryInitializer columnRegistryInitializer = new ColumnRegistryInitializer(columnRegistry, toolStorage);
        columnRegistryInitializer.initialize();

        registerTargetColumns();
    }

    /**
     * Writes MMD by having the input files in the outermost loop to avoid re-opening them.
     *
     * @param mmd
     * @param mmdVariables
     * @param recordOfMatchup inverted index of matchups and their (foreseen or existing) record numbers in the mmd
     */
    void writeMmdShuffled(NetcdfFileWriter mmd, List<Variable> mmdVariables, Map<Integer, Integer> recordOfMatchup) {
        final String condition = getCondition();
        final int pattern = getPattern();
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
            query.setParameter(2, getTime(Constants.PROPERTY_TARGET_START_TIME));
            query.setParameter(3, getTime(Constants.PROPERTY_TARGET_STOP_TIME));
            if (pattern != 0) {
                query.setParameter(4, pattern);
            }
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

    private Map<Integer, Integer> createInvertedIndexOfMatchups(String condition, int pattern) {
        final Map<Integer, Integer> recordOfMatchup = new HashMap<Integer, Integer>();
        {
            final List<Matchup> matchups = Queries.getMatchups(getPersistenceManager(),
                    getTime(Constants.PROPERTY_TARGET_START_TIME),
                    getTime(Constants.PROPERTY_TARGET_STOP_TIME),
                    condition, pattern);
            getLogger().info(String.format("%d matchups retrieved", matchups.size()));
            for (int i = 0; i < matchups.size(); ++i) {
                recordOfMatchup.put(matchups.get(i).getId(), i);
            }
        }
        return recordOfMatchup;
    }

    private Map<String, List<Variable>> createSensorMap(List<Variable> mmdVariables) {
        Map<String, List<Variable>> sensorMap = new HashMap<String, List<Variable>>();
        for (final Variable variable : mmdVariables) {
            final Item targetColumn = columnRegistry.getColumn(variable.getShortName());
            final String sensorName = targetColumn.getSensor().getName();
            List<Variable> variables = sensorMap.get(sensorName);
            if (variables == null) {
                variables = new ArrayList();
                sensorMap.put(sensorName, variables);
            }
            variables.add(variable);
        }
        return sensorMap;
    }

    private Date getTime(String key) {
        return getConfig().getDateValue(key);
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

    static NetcdfFileWriter createNetCDFWriter(Configuration config) throws IOException {
        final String mmdDirPath = config.getStringValue(Configuration.KEY_MMS_MMD_TARGET_DIR, ".");
        final String mmdFileName = config.getStringValue(Configuration.KEY_MMS_MMD_TARGET_FILENAME, "mmd.nc");
        final String mmdFilePath = new File(mmdDirPath, mmdFileName).getPath();

        final NetcdfFileWriter mmd = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, mmdFilePath);
        mmd.setLargeFile(true);

        return mmd;
    }


    private NetcdfFileWriter defineMmd(NetcdfFileWriter mmd) throws IOException {
        defineDimensions(mmd);
        defineVariables(mmd);
        defineGlobalAttributes(mmd);

        mmd.create();

        return mmd;
    }

    private void defineDimensions(NetcdfFileWriter mmdFile) {
        matchupCount = Queries.getMatchupCount(getPersistenceManager(),
                getTime(Constants.PROPERTY_TARGET_START_TIME),
                getTime(Constants.PROPERTY_TARGET_STOP_TIME),
                getCondition(), getPattern());
        getLogger().info(String.format("%d matchups in time interval", matchupCount));
        if (matchupCount == 0) {
            mmdFile.addUnlimitedDimension(Constants.DIMENSION_NAME_MATCHUP);
        } else {
            mmdFile.addDimension(null, Constants.DIMENSION_NAME_MATCHUP, matchupCount);
        }
        for (final String dimensionName : dimensionNames) {
            if (Constants.DIMENSION_NAME_MATCHUP.equals(dimensionName)) {
                continue;
            }
            if (!dimensionConfiguration.containsKey(dimensionName)) {
                throw new ToolException(
                        MessageFormat.format("Length of dimension ''{0}'' is not configured.", dimensionName),
                        ToolException.TOOL_CONFIGURATION_ERROR);
            }
            mmdFile.addDimension(null, dimensionName, dimensionConfiguration.get(dimensionName));
        }
    }

    private String getCondition() {
        return getConfig().getStringValue("mms.target.condition", null);
    }

    // e.g. "m.pattern & ?2 = ?2"  (select matchups with certain sensors)
    private int getPattern() {
        try {
            return Integer.parseInt(getConfig().getStringValue("mms.target.pattern", "0"), 16);
        } catch (NumberFormatException e) {
            throw new ToolException("Property 'mms.target.pattern' must be set to an integral number.", e,
                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    private void defineVariables(NetcdfFileWriter mmdFile) {
        for (final String name : targetColumnNames) {
            final Item column = columnRegistry.getColumn(name);
            IoUtil.addVariable(mmdFile, column);
        }
    }

    private void defineGlobalAttributes(NetcdfFileWriter file) {
        file.addGroupAttribute(null, new Attribute("title", "SST CCI multi-sensor match-up dataset (MMD) template"));
        file.addGroupAttribute(null, new Attribute("institution", "Brockmann Consult"));
        file.addGroupAttribute(null, new Attribute("contact", "Ralf Quast (ralf.quast@brockmann-consult.de)"));
        file.addGroupAttribute(null, new Attribute("creation_date", Calendar.getInstance().getTime().toString()));
        file.addGroupAttribute(null, new Attribute("total_number_of_matchups", matchupCount));
    }

    private void readDimensionConfiguration(Collection<String> dimensionNames) {
        final String configFilePath = getConfig().getStringValue(Configuration.KEY_MMS_MMD_DIMENSIONS);
        if (configFilePath == null) {
            throw new ToolException("No target dimensions specified.", ToolException.TOOL_CONFIGURATION_ERROR);
        }
        final Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(configFilePath));
            for (final String dimensionName : dimensionNames) {
                if (Constants.DIMENSION_NAME_MATCHUP.equals(dimensionName)) {
                    continue;
                }
                final String dimensionLength = properties.getProperty(dimensionName);
                if (!properties.containsKey(dimensionName)) {
                    throw new ToolException(
                            MessageFormat.format("Length of dimension ''{0}'' is not configured.", dimensionName),
                            ToolException.TOOL_CONFIGURATION_ERROR);
                }
                try {
                    dimensionConfiguration.put(dimensionName, Integer.parseInt(dimensionLength));
                } catch (NumberFormatException e) {
                    throw new ToolException(
                            MessageFormat.format("Cannot parse length of dimension ''{0}''.", dimensionName),
                            ToolException.TOOL_CONFIGURATION_ERROR);
                }
            }
        } catch (FileNotFoundException e) {
            throw new ToolException(e.getMessage(), e, ToolException.CONFIGURATION_FILE_NOT_FOUND_ERROR);
        } catch (IOException e) {
            throw new ToolException(e.getMessage(), e, ToolException.CONFIGURATION_FILE_IO_ERROR);
        }
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
}
