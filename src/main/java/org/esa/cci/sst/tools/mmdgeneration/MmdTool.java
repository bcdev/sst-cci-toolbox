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

import org.esa.cci.sst.ColumnRegistry;
import org.esa.cci.sst.Queries;
import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.reader.ExtractDefinition;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.reader.ReaderFactory;
import org.esa.cci.sst.rules.Context;
import org.esa.cci.sst.rules.Converter;
import org.esa.cci.sst.rules.RuleException;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.Cache;
import org.esa.cci.sst.util.ExtractDefinitionBuilder;
import org.esa.cci.sst.util.IoUtil;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import javax.persistence.Query;

/**
 * Tool for writing the matchup data file.
 *
 * @author Ralf Quast
 */
public class MmdTool extends BasicTool {

    private final ColumnRegistry columnRegistry = new ColumnRegistry();

    private final Set<String> dimensionNames = new TreeSet<String>();
    private final Map<String, Integer> dimensionConfiguration = new HashMap<String, Integer>(50);
    private final List<String> targetColumnNames = new ArrayList<String>(500);

    private final Cache<String, Reader> readerCache = new Cache<String, Reader>(50);

    private int matchupCount;

    public MmdTool() {
        super("mmsmmd.sh", "0.1");
    }

    /**
     * Main method. Generates a matchup data file based on the MMDB contents. Configured by the file
     * <code>mms-config.properties</code>.
     *
     * @param args The usual command line arguments.
     */
    public static void main(String[] args) {
        final MmdTool tool = new MmdTool();

        NetcdfFileWriteable mmd = null;
        try {
            final boolean performWork = tool.setCommandLineArgs(args);
            if (!performWork) {
                return;
            }
            tool.initialize();

            mmd = tool.createMmd();
            mmd = tool.defineMmd(mmd);
            if (Boolean.valueOf((String) tool.getConfiguration().get("mms.target.shuffled"))) {
                tool.writeMmdShuffled(mmd);
            } else {
                tool.writeMmd(mmd);
            }
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Throwable t) {
            tool.getErrorHandler().terminate(new ToolException(t.getMessage(), t, ToolException.UNKNOWN_ERROR));
        } finally {
            if (mmd != null) {
                try {
                    tool.closeReaders();
                    mmd.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void closeReaders() {
        final Collection<Reader> removedReaders = readerCache.clear();
        for (final Reader reader : removedReaders) {
            reader.close();
        }
    }

    /**
     * Writes MMD by having the input files in the outermost loop to avoid re-opening them.
     * @param mmd
     */
    private void writeMmdShuffled(NetcdfFileWriteable mmd) {
        // group variables by sensors
        Map<String,List<Variable>> variablesOfSensors = new HashMap<String,List<Variable>>();
        for (final Variable variable : mmd.getVariables()) {
            final Item targetColumn = columnRegistry.getColumn(variable.getName());
            final String sensorName = targetColumn.getSensor().getName();
            List<Variable> variables = variablesOfSensors.get(sensorName);
            if (variables == null) {
                variables = new ArrayList();
                variablesOfSensors.put(sensorName, variables);
            }
            variables.add(variable);
        }
        // create inverted index of matchups
        final Map<Integer,Integer> recordOfMatchup = new HashMap<Integer,Integer>();
        {
            final List<Matchup> matchups = Queries.getMatchups(getPersistenceManager(),
                                                               getSourceStartTime(),
                                                               getSourceStopTime(),
                                                               getTargetPattern());
            for (int i=0; i<matchups.size(); ++i) {
                recordOfMatchup.put(matchups.get(i).getId(), i);
            }
        }
        // memorise previous datafile
        DataFile previousDataFile = null;
        // loop over sensors, matchups ordered by sensor files, variables of sensor
        for (String sensorName : variablesOfSensors.keySet()) {
            final Query query;
            if (! "Implicit".equals(sensorName)) {
                query =
                    getPersistenceManager().createNativeQuery("select m.id " +
                                                 "from mm_datafile f, mm_datafile g, mm_observation o, mm_matchup m, mm_observation r " +
                                                 "where o.sensor = ?1 and m.pattern & ?2 = ?2 " +
                                                 "and m.refobs_id = r.id and o.datafile_id = f.id and r.datafile_id = g.id " +
                                                 "and r.time >= ?3 and r.time < ?4 " +
                                                 "and ( o.id = m.refobs_id " +
                                                 "or exists(select * from mm_coincidence c where o.id = c.observation_id and c.matchup_id = m.id ) ) " +
                                                 "order by f.name, g.name, r.time", Matchup.class);
            } else {
                query =
                    getPersistenceManager().createNativeQuery("select m.id " +
                                                 "from mm_datafile g, mm_matchup m, mm_observation r " +
                                                 "where m.pattern & ?2 = ?2 " +
                                                 "and m.refobs_id = r.id and r.datafile_id = g.id " +
                                                 "and r.time >= ?3 and r.time < ?4 " +
                                                 "order by g.name, r.time", Matchup.class);

            }
            query.setParameter(1, sensorName);
            query.setParameter(2, getTargetPattern());
            query.setParameter(3, getSourceStartTime());
            query.setParameter(4, getSourceStopTime());
            List<Matchup> matchups = query.getResultList();
            for (final Matchup matchup : matchups) {
                try {
                    final int targetRecordNo = recordOfMatchup.get(matchup.getId());
                    final ReferenceObservation referenceObservation = matchup.getRefObs();
                    final Observation observation = findObservation(sensorName, matchup);
                    if (observation != null && observation.getDatafile() != null && !observation.getDatafile().equals(previousDataFile)) {
                        if (previousDataFile != null) {

                            closeReader(previousDataFile);
                        }
                        previousDataFile = observation.getDatafile();
                    }
                    for (final Variable variable : variablesOfSensors.get(sensorName)) {
                        final Item targetColumn = columnRegistry.getColumn(variable.getName());
                        final Item sourceColumn = columnRegistry.getSourceColumn(targetColumn);
                        if ("Implicit".equals(sourceColumn.getName())) {
                            Reader observationReader = null;
                            if (observation != null) {
                                observationReader = getReader(observation.getDatafile());
                            }
                            final Reader referenceObservationReader = getReader(referenceObservation.getDatafile());
                            final Context context = new ContextBuilder()
                                    .matchup(matchup)
                                    .observation(observation)
                                    .observationReader(observationReader)
                                    .referenceObservationReader(referenceObservationReader)
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
                } catch (IOException e) {
                    final String message = MessageFormat.format("matchup {0}: {1}",
                                                                matchup.getId(),
                                                                e.getMessage());
                    throw new ToolException(message, e, ToolException.TOOL_IO_ERROR);
                }
            }
        }


    }

    private void writeMmd(NetcdfFileWriteable mmd) {
        final List<Matchup> matchupList = Queries.getMatchups(getPersistenceManager(),
                                                              getSourceStartTime(),
                                                              getSourceStopTime(),
                                                              getTargetPattern(),
                                                              getDuplicateFlag());

        for (int targetRecordNo = 0, matchupListSize = matchupList.size(); targetRecordNo < matchupListSize; targetRecordNo++) {
            final Matchup matchup = matchupList.get(targetRecordNo);
            final ReferenceObservation referenceObservation = matchup.getRefObs();

            if (getLogger().isLoggable(Level.INFO)) {
                getLogger().info(MessageFormat.format(
                        "writing data for matchup {0} ({1}/{2})", matchup.getId(), targetRecordNo + 1,
                        matchupListSize));
            }

            for (final Variable variable : mmd.getVariables()) {
                final Item targetColumn = columnRegistry.getColumn(variable.getName());
                final Item sourceColumn = columnRegistry.getSourceColumn(targetColumn);
                if ("Implicit".equals(sourceColumn.getName())) {
                    final String sensorName = targetColumn.getSensor().getName();
                    final Observation observation = findObservation(sensorName, matchup);
                    Reader observationReader = null;
                    if (observation != null) {
                        try {
                            observationReader = getReader(observation.getDatafile());
                        } catch (IOException e) {
                            final String message = MessageFormat.format("observation {0}: {1}",
                                                                        observation.getId(),
                                                                        e.getMessage());
                            getLogger().warning(message);
                        }
                    }
                    final Reader referenceObservationReader;
                    try {
                        referenceObservationReader = getReader(referenceObservation.getDatafile());
                    } catch (IOException e) {
                        final String message = MessageFormat.format("observation {0}: {1}",
                                                                    referenceObservation.getId(),
                                                                    e.getMessage());
                        throw new ToolException(message, e, ToolException.TOOL_IO_ERROR);
                    }
                    final Context context = new ContextBuilder()
                            .matchup(matchup)
                            .observation(observation)
                            .observationReader(observationReader)
                            .referenceObservationReader(referenceObservationReader)
                            .targetVariable(variable)
                            .dimensionConfiguration(dimensionConfiguration)
                            .build();
                    writeImplicitColumn(mmd, variable, targetRecordNo, targetColumn, context);
                } else {
                    final String sensorName = targetColumn.getSensor().getName();
                    final Observation observation = findObservation(sensorName, matchup);
                    if (observation != null) {
                        writeColumn(mmd, variable, targetRecordNo, targetColumn, sourceColumn, observation,
                                    referenceObservation);
                    }
                }
            }
        }
    }

    private void writeImplicitColumn(NetcdfFileWriteable mmd, Variable variable, int targetRecordNo, Item targetColumn,
                                     Context context) {
        try {
            final Converter converter = columnRegistry.getConverter(targetColumn);
            converter.setContext(context);
            final Array targetArray = converter.apply(null);
            if (targetArray != null) {
                final int[] targetStart = new int[variable.getRank()];
                targetStart[0] = targetRecordNo;
                mmd.write(variable.getNameEscaped(), targetStart, targetArray);
            }
        } catch (IOException e) {
            final String message = MessageFormat.format("matchup {0}: {1}", context.getMatchup().getId(),
                                                        e.getMessage());
            throw new ToolException(message, e, ToolException.TOOL_IO_ERROR);
        } catch (RuleException e) {
            final String message = MessageFormat.format("matchup {0}: {1}", context.getMatchup().getId(),
                                                        e.getMessage());
            throw new ToolException(message, e, ToolException.TOOL_ERROR);
        } catch (InvalidRangeException e) {
            final String message = MessageFormat.format("matchup {0}: {1}", context.getMatchup().getId(),
                                                        e.getMessage());
            throw new ToolException(message, e, ToolException.TOOL_ERROR);
        }
    }

    private void writeColumn(NetcdfFileWriteable mmd, Variable variable, int i, Item targetColumn, Item sourceColumn,
                             Observation observation, ReferenceObservation refObs) {
        try {
            final Reader reader = getReader(observation.getDatafile());
            final String role = sourceColumn.getRole();
            final ExtractDefinition extractDefinition =
                    new ExtractDefinitionBuilder()
                            .referenceObservation(refObs)
                            .recordNo(observation.getRecordNo())
                            .shape(variable.getShape())
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
                mmd.write(variable.getNameEscaped(), targetStart, targetArray);
            }
        } catch (IOException e) {
            final String message = MessageFormat.format("observation {0}: {1}", observation.getId(), e.getMessage());
            getLogger().warning(message);
        } catch (RuleException e) {
            final String message = MessageFormat.format("observation {0}: {1}", observation.getId(), e.getMessage());
            throw new ToolException(message, e, ToolException.TOOL_ERROR);
        } catch (InvalidRangeException e) {
            final String message = MessageFormat.format("observation {0}: {1}", observation.getId(), e.getMessage());
            throw new ToolException(message, e, ToolException.TOOL_ERROR);
        }
    }

    private Reader getReader(DataFile datafile) throws IOException {
        final String path = datafile.getPath();
        if (!readerCache.contains(path)) {
            final Reader reader;
            try {
                final String message = MessageFormat.format("opening input file {0}", datafile.getPath());
                getLogger().warning(message);
                reader = ReaderFactory.open(datafile, getConfiguration());
            } catch (Exception e) {
                throw new IOException(MessageFormat.format("Unable to open file ''{0}''.", path), e);
            }
            final Reader removedReader = readerCache.add(path, reader);
            if (removedReader != null) {
                removedReader.close();
            }
        }
        return readerCache.get(path);
    }

    private void closeReader(DataFile datafile) {
        final Reader removedReader = readerCache.remove(datafile.getPath());
        if (removedReader != null) {
            removedReader.close();
            final String message = MessageFormat.format("closing input file {0}", datafile.getPath());
            getLogger().warning(message);
        } else {
            final String message = MessageFormat.format("failed closing input file {0}", datafile.getPath());
            getLogger().warning(message);
        }
    }

    @Override
    public void initialize() {
        super.initialize();

        registerSourceColumns();
        registerImplicitColumn();
        registerTargetColumns();

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

        readDimensionConfiguration(dimensionNames);
    }

    private NetcdfFileWriteable createMmd() throws IOException {
        final Properties configuration = getConfiguration();
        final String mmdDirPath = configuration.getProperty("mms.target.dir", ".");
        final String mmdFileName = configuration.getProperty("mms.target.filename", "mmd.nc");
        final String mmdFilePath = new File(mmdDirPath, mmdFileName).getPath();

        final NetcdfFileWriteable mmd = NetcdfFileWriteable.createNew(mmdFilePath, true);
        mmd.setLargeFile(true);

        return mmd;
    }


    private NetcdfFileWriteable defineMmd(NetcdfFileWriteable mmd) throws IOException {
        defineDimensions(mmd);
        defineVariables(mmd);
        defineGlobalAttributes(mmd);

        mmd.create();

        return mmd;
    }

    private void defineDimensions(NetcdfFileWriteable mmdFile) {
        matchupCount = Queries.getMatchupCount(getPersistenceManager(), getSourceStartTime(), getSourceStopTime(),
                                               getTargetPattern());
        if (matchupCount == 0) {
            mmdFile.addUnlimitedDimension(Constants.DIMENSION_NAME_MATCHUP);
        } else {
            mmdFile.addDimension(Constants.DIMENSION_NAME_MATCHUP, matchupCount);
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
            mmdFile.addDimension(dimensionName, dimensionConfiguration.get(dimensionName));
        }
    }

    private int getTargetPattern() {
        try {
            return Integer.parseInt(getConfiguration().getProperty("mms.target.pattern", "0"));
        } catch (NumberFormatException e) {
            throw new ToolException("Property 'mms.target.pattern' must be set to an integral number.", e,
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    private int getDuplicateFlag() {
        final String duplicateFlagProperty = getConfiguration().getProperty("mms.target.duplicateFlag", "-1");
        return Integer.parseInt(duplicateFlagProperty);
    }

    private void defineVariables(NetcdfFileWriteable mmdFile) {
        for (final String name : targetColumnNames) {
            final Item column = columnRegistry.getColumn(name);
            IoUtil.addVariable(mmdFile, column);
        }
    }

    private void defineGlobalAttributes(NetcdfFileWriteable file) {
        file.addGlobalAttribute("title", "SST CCI multi-sensor match-up dataset (MMD) template");
        file.addGlobalAttribute("institution", "Brockmann Consult");
        file.addGlobalAttribute("contact", "Ralf Quast (ralf.quast@brockmann-consult.de)");
        file.addGlobalAttribute("creation_date", Calendar.getInstance().getTime().toString());
        file.addGlobalAttribute("total_number_of_matchups", matchupCount);
    }

    private void readDimensionConfiguration(Collection<String> dimensionNames) {
        final String configFilePath = getConfiguration().getProperty("mms.target.dimensions");
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

    private void registerSourceColumns() {
        final List<? extends Item> columns = Queries.getAllColumns(getPersistenceManager());
        for (final Item column : columns) {
            columnRegistry.register(column);
        }
    }

    private void registerTargetColumns() {
        final String configFilePath = getConfiguration().getProperty("mms.target.variables");
        if (configFilePath == null) {
            throw new ToolException("No target variables specified.", ToolException.TOOL_CONFIGURATION_ERROR);
        }
        try {
            targetColumnNames.addAll(columnRegistry.registerColumns(new File(configFilePath)));
        } catch (FileNotFoundException e) {
            throw new ToolException(e.getMessage(), e, ToolException.CONFIGURATION_FILE_NOT_FOUND_ERROR);
        } catch (ParseException e) {
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    private void registerImplicitColumn() {
        columnRegistry.register(new ColumnBuilder().build());
    }

    private static Observation findObservation(String sensorName, Matchup matchup) {
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
}
