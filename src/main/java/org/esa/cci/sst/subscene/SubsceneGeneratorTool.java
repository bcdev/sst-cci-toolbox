/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.subscene;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.util.io.CsvReader;
import org.esa.cci.sst.Constants;
import org.esa.cci.sst.SensorName;
import org.esa.cci.sst.ToolException;
import org.esa.cci.sst.orm.PersistenceManager;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Tool responsible for cutting out subscenes from a number of files given in a csv file.
 *
 * @author Thomas Storm
 */
public class SubsceneGeneratorTool {

    private static final String GENERATE_SUBSCENES_FILE = "generate_subscenes.csv";

    public static void main(String[] args) throws IOException, ToolException {
        final Properties properties = new Properties();
        properties.load(new FileReader("mms-config.properties"));
        PersistenceManager persistenceManager = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME, properties);
        for (SubsceneIO subsceneIO : getFilenames(GENERATE_SUBSCENES_FILE)) {
            SubsceneGenerator generator = getSubsceneGenerator(subsceneIO.inputFilename, persistenceManager);
            generator.createSubscene(subsceneIO);
        }
    }

    static SubsceneIO[] getFilenames(String filename) throws IOException {
        CsvReader csvReader = new CsvReader(new FileReader(filename), new char[]{' ', ',', '\n', '\t'}, true, "#");
        final List<String[]> records = csvReader.readStringRecords();
        SubsceneIO[] files = new SubsceneIO[records.size()];
        for (int i = 0; i < records.size(); i++) {
            final String[] record = records.get(i);
            if (record.length == 1) {
                files[i] = new SubsceneIO(record[0], createDefaultSubsceneName(record[0]));
            } else if (record.length == 2) {
                files[i] = new SubsceneIO(record[0], record[1]);
            } else {
                throw new IllegalStateException(
                        "CSV-file '" + filename + "' has wrong format at record '" + record[0] + "'.");
            }
        }
        return files;
    }

    static String createDefaultSubsceneName(String filename) {
        StringBuilder builder = new StringBuilder(filename);
        int offset = filename.lastIndexOf('.');
        if (offset == -1) {
            offset = filename.length();
        }
        builder.insert(offset, "_subscene");
        return builder.toString();
    }

    static SubsceneGenerator getSubsceneGenerator(final String filename, PersistenceManager persistenceManager) throws
                                                                                                                IOException {
        if (ProductIO.getProductReaderForFile(new File(filename)) != null) {
            if (SensorName.SENSOR_NAME_AATSR.getSensor().equals(getSensorFromFilename(filename))) {
                return new AtsrSubsceneGenerator(persistenceManager);
            } else if (SensorName.SENSOR_NAME_AMSRE.getSensor().equals(getSensorFromFilename(filename))) {
                return new AmsreSubsceneGenerator(persistenceManager);
            }
        } else if (NetcdfFile.canOpen(filename)) {
            // todo - ts - check if needed or if we have sufficient beam product readers already
            return new NetcdfSubsceneGenerator(persistenceManager);
        }
        throw new IllegalArgumentException("No subscene generator found for file '" + filename + "'.");
    }

    private static String getSensorFromFilename(String filename) {
        filename = filename.toLowerCase();
        if (filename.contains("atsr") || filename.contains("ats_toa")) {
            return SensorName.SENSOR_NAME_AATSR.getSensor();
        } else if (filename.contains("-amsre-")) {
            return SensorName.SENSOR_NAME_AMSRE.getSensor();
        }
        throw new IllegalArgumentException("No subscene generator found for file '" + filename + "'.");
    }


    interface SubsceneGenerator {

        /**
         * Creates the subscene for the given combination of input and output filename.
         * @param subsceneIO An object containing input and output filename.
         * @throws IOException If some error occurs (any error is considered an IO error).
         */
        void createSubscene(SubsceneIO subsceneIO) throws IOException;

    }

    static class SubsceneIO {

        private String inputFilename;
        private String outputFilename;

        private SubsceneIO(String inputFilename, String outputFilename) {
            this.inputFilename = inputFilename;
            this.outputFilename = outputFilename;
        }

        public String getInputFilename() {
            return inputFilename;
        }

        public String getOutputFilename() {
            return outputFilename;
        }
    }

}