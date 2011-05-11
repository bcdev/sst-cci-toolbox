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
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.util.IoUtil;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFileWriteable;

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

        NetcdfFileWriteable mmdFile = null;
        try {
            final boolean performWork = tool.setCommandLineArgs(args);
            if (!performWork) {
                return;
            }
            tool.initialize();

            mmdFile = tool.newMmdFile();
            mmdFile = tool.defineContents(mmdFile);

            writeMatchups(mmdFile);
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Throwable t) {
            tool.getErrorHandler().terminate(new ToolException(t.getMessage(), t, ToolException.UNKNOWN_ERROR));
        } finally {
            if (mmdFile != null) {
                try {
                    mmdFile.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void writeMatchups(NetcdfFileWriteable mmdFile) {
        // todo - implement
    }

    @Override
    public void initialize() {
        super.initialize();

        registerSourceColumns();
        registerImplicitColumns();
        registerTargetColumns();

        for (final String name : targetColumnNames) {
            final Item column = columnRegistry.getColumn(name);
            dimensionNames.addAll(Arrays.asList(column.getDimensions().split("\\s")));
        }

        readDimensionConfiguration(dimensionNames);
    }

    private NetcdfFileWriteable newMmdFile() throws IOException {
        final Properties configuration = getConfiguration();
        final String mmdDirPath = configuration.getProperty("mms.target.dir", ".");
        final String mmdFileName = configuration.getProperty("mms.target.filename", "mmd.nc");
        final String mmdFilePath = new File(mmdDirPath, mmdFileName).getPath();

        final NetcdfFileWriteable mmdFile = NetcdfFileWriteable.createNew(mmdFilePath, true);
        mmdFile.setLargeFile(true);

        return mmdFile;
    }


    private NetcdfFileWriteable defineContents(NetcdfFileWriteable mmdFile) throws IOException {
        defineDimensions(mmdFile);
        defineVariables(mmdFile);
        defineGlobalAttributes(mmdFile);

        mmdFile.create();

        return mmdFile;
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
                if (!Constants.DIMENSION_NAME_MATCHUP.equals(dimensionName)) {
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

    private void registerImplicitColumns() {
        final List<Item> implicitColumns = new ArrayList<Item>(10);
        final ColumnBuilder builder = new ColumnBuilder();

        builder.dimensions(Constants.DIMENSION_NAME_MATCHUP);

        final Item matchupId =
                builder.name(Constants.COLUMN_NAME_MATCHUP_ID).
                        type(DataType.INT).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP).
                        unit(Constants.UNITLESS).
                        build();
        implicitColumns.add(matchupId);

        final Item matchupTime =
                builder.name(Constants.COLUMN_NAME_MATCHUP_TIME).
                        type(DataType.INT).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP).
                        unit(Constants.UNIT_TIME).
                        build();
        implicitColumns.add(matchupTime);

        final Item matchupLon =
                builder.name(Constants.COLUMN_NAME_MATCHUP_LON).
                        type(DataType.FLOAT).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP).
                        unit(Constants.UNIT_LON).
                        build();
        implicitColumns.add(matchupLon);

        final Item matchupLat =
                builder.name(Constants.COLUMN_NAME_MATCHUP_LAT).
                        type(DataType.FLOAT).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP).
                        unit(Constants.UNIT_LAT).
                        build();
        implicitColumns.add(matchupLat);

        final Item matchupInsituCallsign =
                builder.name(Constants.COLUMN_NAME_MATCHUP_INSITU_CALLSIGN).
                        type(DataType.CHAR).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP + " " + Constants.DIMENSION_NAME_CALLSIGN_LENGTH).
                        unit(Constants.UNITLESS).
                        build();
        implicitColumns.add(matchupInsituCallsign);

        final Item matchupInsituDataset =
                builder.name(Constants.COLUMN_NAME_MATCHUP_INSITU_DATASET).
                        type(DataType.BYTE).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP).
                        unit(Constants.UNITLESS).
                        build();
        implicitColumns.add(matchupInsituDataset);

        final Item matchupReferenceFlag =
                builder.name(Constants.COLUMN_NAME_MATCHUP_REFERENCE_FLAG).
                        type(DataType.BYTE).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP).
                        unit(Constants.UNITLESS).
                        build();
        implicitColumns.add(matchupReferenceFlag);

        final Item matchupValidFlag =
                builder.name(Constants.COLUMN_NAME_MATCHUP_VALID).
                        type(DataType.BYTE).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP).
                        unit(Constants.UNITLESS).
                        build();
        implicitColumns.add(matchupValidFlag);

        final Item matchupPrimarySensor =
                builder.name(Constants.COLUMN_NAME_MATCHUP_PRIMARY_SENSOR).
                        type(DataType.BYTE).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP).
                        unit(Constants.UNITLESS).
                        build();
        implicitColumns.add(matchupPrimarySensor);

        final Item matchupPrimaryFilename =
                builder.name(Constants.COLUMN_NAME_MATCHUP_PRIMARY_FILENAME).
                        type(DataType.CHAR).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP + " " + Constants.DIMENSION_NAME_FILENAME_LENGTH).
                        unit(Constants.UNITLESS).
                        build();
        implicitColumns.add(matchupPrimaryFilename);

        final Item matchupSensorList =
                builder.name(Constants.COLUMN_NAME_MATCHUP_SENSOR_LIST).
                        type(DataType.CHAR).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP).
                        unit(Constants.UNITLESS).
                        build();
        implicitColumns.add(matchupSensorList);

        final Item insituTime =
                builder.name(Constants.COLUMN_NAME_INSITU_TIME).
                        type(DataType.INT).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP + " " + Constants.DIMENSION_NAME_INSITU_TIME).
                        unit(Constants.UNIT_TIME).
                        build();
        implicitColumns.add(insituTime);

        final Item insituLon =
                builder.name(Constants.COLUMN_NAME_INSITU_LON).
                        type(DataType.FLOAT).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP + " " + Constants.DIMENSION_NAME_INSITU_TIME).
                        unit(Constants.UNIT_LON).
                        build();
        implicitColumns.add(insituLon);

        final Item insituLat =
                builder.name(Constants.COLUMN_NAME_INSITU_LAT).
                        type(DataType.FLOAT).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP + " " + Constants.DIMENSION_NAME_INSITU_TIME).
                        unit(Constants.UNIT_LAT).
                        build();
        implicitColumns.add(insituLat);

        final Item insituSst =
                builder.name(Constants.COLUMN_NAME_INSITU_SST).
                        type(DataType.SHORT).
                        dimensions(Constants.DIMENSION_NAME_MATCHUP + " " + Constants.DIMENSION_NAME_INSITU_TIME).
                        unit(Constants.UNIT_SST).
                        build();
        implicitColumns.add(insituSst);

        // todo - add NWP etc.

        for (final Item column : implicitColumns) {
            if (columnRegistry.hasColumn(column.getName())) {
                throw new ToolException(MessageFormat.format("Column ''{0}'' is already defined.", column.getName()),
                                        ToolException.UNKNOWN_ERROR);
            }
            columnRegistry.register(column);
        }
    }
}
