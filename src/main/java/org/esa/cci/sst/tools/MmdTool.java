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

package org.esa.cci.sst.tools;

import org.esa.cci.sst.ColumnRegistry;
import org.esa.cci.sst.Queries;
import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.reader.ExtractDefinition;
import org.esa.cci.sst.reader.IOHandler;
import org.esa.cci.sst.reader.IOHandlerFactory;
import org.esa.cci.sst.rules.Converter;
import org.esa.cci.sst.rules.RuleException;
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

    private final Cache<String, IOHandler> readerCache = new Cache<String, IOHandler>(100);

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
            tool.writeMmd(mmd);
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
        final Collection<IOHandler> removedReaders = readerCache.clear();
        for (final IOHandler reader : removedReaders) {
            reader.close();
        }
    }

    private void writeMmd(NetcdfFileWriteable mmd) {
        final List<Matchup> matchupList = Queries.getMatchups(getPersistenceManager(),
                                                              getSourceStartTime(),
                                                              getSourceStopTime());

        for (int i = 0, matchupListSize = matchupList.size(); i < matchupListSize; i++) {
            final Matchup matchup = matchupList.get(i);
            final List<Coincidence> coincidenceList = matchup.getCoincidences();

            if (getLogger().isLoggable(Level.INFO)) {
                getLogger().info(MessageFormat.format(
                        "writing data for matchup {0} ({1}/{2})", matchup.getId(), i, matchupListSize));
            }

            for (final Variable variable : mmd.getVariables()) {
                final Item targetColumn = columnRegistry.getColumn(variable.getName());
                final Item sourceColumn = columnRegistry.getSourceColumn(targetColumn);

                if ("Implicit".equals(sourceColumn.getName())) {
                    // todo - implement
                } else {
                    final String sensorName = targetColumn.getSensor().getName();
                    final Coincidence coincidence = findCoincidence(sensorName, coincidenceList);
                    if (coincidence != null) {
                        writeColumn(mmd, variable, i, targetColumn, sourceColumn, coincidence);
                    }
                }
            }
        }
    }

    private void writeColumn(NetcdfFileWriteable mmd, Variable variable, int i, Item targetColumn, Item sourceColumn,
                             Coincidence coincidence) {
        try {
            final IOHandler reader = getReader(coincidence.getObservation().getDatafile());
            final String role = sourceColumn.getRole();
            final ExtractDefinition extractDefinition =
                    new ExtractDefinitionBuilder()
                            .coincidence(coincidence)
                            .recordNo(coincidence.getObservation().getRecordNo())
                            .shape(variable.getShape())
                            .build();
            final Array sourceArray = reader.read(role, extractDefinition);
            if (sourceArray != null) {
                getLogger().fine(
                        MessageFormat.format("source column: {0}, {1}", sourceColumn.getName(),
                                             sourceColumn.getRole()));
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
            final String message = MessageFormat.format("coincidence {0}: {1}", coincidence.getId(), e.getMessage());
            throw new ToolException(message, e, ToolException.TOOL_IO_ERROR);
        } catch (RuleException e) {
            final String message = MessageFormat.format("coincidence {0}: {1}", coincidence.getId(), e.getMessage());
            throw new ToolException(message, e, ToolException.TOOL_ERROR);
        } catch (InvalidRangeException e) {
            final String message = MessageFormat.format("coincidence {0}: {1}", coincidence.getId(), e.getMessage());
            throw new ToolException(message, e, ToolException.TOOL_ERROR);
        }
    }

    private IOHandler getReader(DataFile datafile) throws IOException {
        final String path = datafile.getPath();
        if (!readerCache.contains(path)) {
            final IOHandler reader = IOHandlerFactory.open(datafile, getConfiguration());
            final IOHandler removedReader = readerCache.add(path, reader);
            if (removedReader != null) {
                removedReader.close();
            }
        }
        return readerCache.get(path);
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
        matchupCount = Queries.getMatchupCount(getPersistenceManager(), getSourceStartTime(), getSourceStopTime());
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

    private static Coincidence findCoincidence(String sensorName, List<Coincidence> coincidenceList) {
        for (final Coincidence coincidence : coincidenceList) {
            if (sensorName.equals(coincidence.getObservation().getSensor())) {
                return coincidence;
            }
        }
        return null;
    }

}
