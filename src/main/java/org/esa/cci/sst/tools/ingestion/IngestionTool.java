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

package org.esa.cci.sst.tools.ingestion;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.reader.ReaderFactory;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.ToolException;

import javax.persistence.Query;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Tool to ingest new input files containing records of observations into the MMS database.
 *
 * @author Martin Boettcher
 * @author Norman Fomferra
 */
public class IngestionTool extends BasicTool {

    // todo - invert dependency: the ingester uses the tool as strategy for persistence, logging etc. (rq-20110507)
    // todo - strategy pattern: the tool shall use the ingester as strategy for ingesting (rq-20110507)
    private Ingester ingester;

    public static void main(String[] args) {
        final IngestionTool tool = new IngestionTool();
        try {
            final boolean performWork = tool.setCommandLineArgs(args);
            if (!performWork) {
                return;
            }
            tool.initialize();
            final boolean doCleanup = Boolean.parseBoolean(tool.getConfiguration().getProperty("mms.initialcleanup"));
            if (doCleanup) {
                tool.cleanup();
            }
            tool.ingest();
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Throwable t) {
            tool.getErrorHandler().terminate(new ToolException(t.getMessage(), t, ToolException.UNKNOWN_ERROR));
        }
    }

    IngestionTool() {
        super("mmsingest.sh", "0.1");
    }

    /**
     * Ingests one input file and creates observation entries in the database
     * for all records contained in input file. Further creates the data file
     * entry, and the schema entry unless it exists, <p>
     * <p/>
     * For METOP MD files two observations are created, one reference observation
     * with a single pixel coordinate and one common observation with a sub-scene.
     * This is achieved by using both factory methods of the reader, readRefObs
     * and readObservation. For other readers only one of them returns an
     * observation. <p>
     * <p/>
     * In order to avoid large transactions a database checkpoint is inserted
     * every 65536 records. If ingestion fails rollback is only performed to
     * the respective checkpoint.
     *
     * @param path            The input file with records to be read and made persistent as observations.
     * @param archiveRoot     The directory prefix to be used for relative paths
     * @param readerSpec      The specification string for the reader.
     * @param sensorName      The sensor name.
     * @param observationType The observation type (simple name of observation class)
     * @param pattern         The sensor pattern.
     */
    private void ingest(String path, File archiveRoot, String readerSpec, String sensorName, String observationType, long pattern) {
        getLogger().info(MessageFormat.format("Ingesting file ''{0}''.", path));
        final PersistenceManager persistenceManager = getPersistenceManager();
        final Reader reader = getReader(readerSpec, sensorName);
        try {
            // open database
            persistenceManager.transaction();

            Sensor sensor = getSensor(sensorName);
            boolean addVariables = false;
            if (sensor == null) {
                addVariables = true;
                sensor = ingester.createSensor(sensorName, observationType, pattern);
            }
            final DataFile dataFile = new DataFile(path, sensor);
            reader.init(dataFile, archiveRoot);

            persistenceManager.persist(dataFile);
            if (addVariables) {
                ingester.persistColumns(sensorName, reader);
            }

            int recordsInTimeInterval = persistObservations(sensorName, reader);
            // make changes in database
            persistenceManager.commit();
            getLogger().info(MessageFormat.format("{0} {1} records in time interval.", sensorName,
                                                  recordsInTimeInterval));
        } catch (Exception e) {
            // do not make any change in case of errors
            try {
                persistenceManager.rollback();
            } catch (Exception ignored) {
                // ignored, because surrounding exception is propagated
            }
            getErrorHandler().warn(e, MessageFormat.format("Failed to ingest file ''{0}''.", path));
        } finally {
            reader.close();
        }
    }

    private Reader getReader(final String readerSpec, final String sensor) {
        final Reader reader;
        try {
            reader = ReaderFactory.createReader(readerSpec, sensor);
        } catch (Exception e) {
            final String message = MessageFormat.format("Cannot create reader for sensor ''{0}''.", sensor);
            throw new ToolException(message, e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
        return reader;
    }

    /**
     * Ingests all input files and creates observation entries in the database
     * for all records contained in input file.
     */
    private void ingest() {
        ingester = new Ingester(this);
        final Properties configuration = getConfiguration();
        final String archiveRootPath = configuration.getProperty("mms.archive.rootdir");
        final File archiveRoot = new File(archiveRootPath);
        int directoryCount = 0;
        for (int i = 0; i < 100; i++) {
            final String inputDirPath = configuration.getProperty(
                    String.format("mms.source.%d.inputDirectory", i));
            final String sensor = configuration.getProperty(
                    String.format("mms.source.%d.sensor", i));
            final String readerSpec = configuration.getProperty(
                    String.format("mms.reader.%s", sensor), ReaderFactory.DEFAULT_READER_SPEC);
            final String patternString = configuration.getProperty(
                    String.format("mms.pattern.%s", sensor), "0");
            final String observationType = configuration.getProperty(
                    String.format("mms.observationType.%s", sensor), "RelatedObservation");
            final long pattern = Long.parseLong(patternString, 16);
            if (readerSpec == null || inputDirPath == null || sensor == null) {
                continue;
            }
            getLogger().info("Looking for " + sensor + " files");
            final String filenamePattern = configuration.getProperty(
                    String.format("mms.source.%d.filenamePattern", i), ".*");
            final File inputDir = new File(archiveRoot, inputDirPath);
            List<File> inputFileList = getInputFiles(filenamePattern, inputDir);
            if (inputFileList.isEmpty()) {
                inputFileList = getInputFiles(filenamePattern + "\\.gz", inputDir);
                if (inputFileList.isEmpty()) {
                    getLogger().warning(MessageFormat.format("No matching input files found in directory ''{0}/{1}''.",
                                                             archiveRootPath, inputDirPath));
                }
            }
            for (final File inputFile : inputFileList) {
                final String path;
                // TODO: relative path to archive root would be better than File to avoid subtraction here [Boe]
                if (inputFile.getPath().startsWith(archiveRootPath + File.separator)) {
                    path = inputFile.getPath().substring(archiveRootPath.length() + 1);
                } else {
                    path = inputFile.getPath();
                }
                ingest(path, archiveRoot, readerSpec, sensor, observationType, pattern);
                directoryCount++;
            }
        }
        validateInputSet(directoryCount);
        getLogger().info(MessageFormat.format("{0} input set(s) ingested.", directoryCount));
    }

    private int persistObservations(final String sensorName, final Reader reader) {
        final PersistenceManager persistenceManager = getPersistenceManager();
        int recordsInTimeInterval = 0;

        // loop over records
        for (int recordNo = 0; recordNo < reader.getNumRecords(); ++recordNo) {
            if (recordNo % 65536 == 0 && recordNo > 0) {
                getLogger().info(MessageFormat.format("Reading record {0} {1}.", sensorName, recordNo));
            }
            try {
                final Observation observation = reader.readObservation(recordNo);
                if (ingester.persistObservation(observation, recordNo)) {
                    recordsInTimeInterval++;
                }
            } catch (ToolException e) {
                throw e;
            } catch (IOException e) {
                throw new ToolException(e.getMessage(), e, ToolException.TOOL_IO_ERROR);
            } catch (IllegalArgumentException e) {
                getLogger().warning(e.getMessage());
            } catch (Exception e) {
                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append(MessageFormat.format("Ignoring observation for record number {0}: {1}.\n",
                                                           recordNo, e.getMessage()));
                for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                    messageBuilder.append(stackTraceElement.toString());
                    messageBuilder.append('\n');
                }
                getLogger().warning(messageBuilder.toString());
            }
            if (recordNo % 65536 == 65535) {
                persistenceManager.commit();
                persistenceManager.transaction();
            }
        }
        return recordsInTimeInterval;
    }

    private void cleanup() {
        getLogger().info("Cleaning up database.");
        final PersistenceManager persistenceManager = getPersistenceManager();
        persistenceManager.transaction();
        Query statement = persistenceManager.createQuery("delete from DataFile f");
        statement.executeUpdate();
        statement = persistenceManager.createQuery("delete from Observation o");
        statement.executeUpdate();
        statement = persistenceManager.createQuery("delete from Column c");
        statement.executeUpdate();
        statement = persistenceManager.createQuery("delete from Sensor s");
        statement.executeUpdate();
        statement = persistenceManager.createQuery("delete from Coincidence c");
        statement.executeUpdate();
        statement = persistenceManager.createQuery("delete from Matchup m");
        statement.executeUpdate();
//        try {
//            statement = persistenceManager.createNativeQuery("drop index geo");
//            statement.executeUpdate();
//        } catch (Exception e) {
//            System.err.format("geo index dropping failed: %s\n%s\n", e.toString(), "drop index geo");
//        }
//        try {
//            statement = persistenceManager.createNativeQuery("create index geo on mm_observation using gist(location)");
//            statement.executeUpdate();
//        } catch (Exception e) {
//            System.err.format("geo index creation failed: %s\n%s\n", e.toString(),
//                              "create index geo on mm_observation using gist(location)");
//        }
        persistenceManager.commit();
    }

    private void validateInputSet(final int directoryCount) {
        if (directoryCount == 0) {
            throw new ToolException("No input sets given.\n" +
                                    "Input sets are specified as configuration properties as follows:\n" +
                                    "\tmms.source.<i>.inputDirectory = <inputDirectory>\n" +
                                    "\tmms.source.<i>.filenamePattern = <filenamePattern> (opt)" +
                                    "\tmms.source.<i>.sensor = <sensor>\n" +
                                    "\tmms.source.<i>.reader = <ReaderClass>",
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    private List<File> getInputFiles(final String filenamePattern, final File inputDir) {
        final List<File> inputFileList = new ArrayList<File>(0);
        collectInputFiles(inputDir, filenamePattern, inputFileList);
        return inputFileList;
    }

    private void collectInputFiles(File inputDir, final String filenamePattern, List<File> inputFileList) {
        if (inputDir.isDirectory()) {
            final File[] inputFiles = inputDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isFile() && file.getName().matches(filenamePattern);
                }
            });
            Arrays.sort(inputFiles);
            Collections.addAll(inputFileList, inputFiles);
            final File[] subDirs = inputDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
            Arrays.sort(subDirs);
            for (final File subDir : subDirs) {
                collectInputFiles(subDir, filenamePattern, inputFileList);
            }
        }
    }

}
