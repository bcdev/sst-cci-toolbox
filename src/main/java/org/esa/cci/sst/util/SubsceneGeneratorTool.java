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

package org.esa.cci.sst.util;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.io.CsvReader;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Tool responsible for cutting out subscenes from a number of files given in a csv file.
 * <p/>
 * For each file <code>f</code> do:
 * <ol>
 * <li>Get geographic boundaries from <code>f</code>
 * <li>Get time stamp from <code>f</code>
 * <li>Perform database query for matchup files with fitting geo boundaries and time
 * <li>Get matchup id from query
 * <li>Create new Netcdf file, add all variables from input product
 * <li>Set dimensions to shape given by subscene definition
 * <li>Add matchup dimension
 * <li>Copy values from input product to netcdf
 * </ol>
 *
 * @author Thomas Storm
 */
public class SubsceneGeneratorTool {

    private static final String GENERATE_SUBSCENES_FILE = "generate_subscenes.csv";

    public static void main(String[] args) throws IOException {
        for (String filename : getFilenames(GENERATE_SUBSCENES_FILE)) {
            SubsceneGenerator generator = getSubsceneGenerator(filename);
        }
    }

    static String[] getFilenames(String filename) throws IOException {
        CsvReader csvReader = new CsvReader(new FileReader(filename), new char[]{' ', '\n', '\t'}, true, "#");
        final List<String[]> records = csvReader.readStringRecords();
        String[] filenames = new String[records.size()];
        for (int i = 0; i < records.size(); i++) {
            final String[] record = records.get(i);
            filenames[i] = record[0];
        }
        return filenames;
    }

    static SubsceneGenerator getSubsceneGenerator(final String filename) throws IOException {
        if (ProductIO.getProductReaderForFile(new File(filename)) != null) {
            return new ProductSubsceneGenerator();
        } else if (NetcdfFile.canOpen(filename)) {
            return new NetcdfSubsceneGenerator();
        }
        throw new IllegalArgumentException("No subscene generator found for file '" + filename + "'.");
    }


    interface SubsceneGenerator {

        ProductSubsetDef createSubsetDef();

        Product createSubscene(ProductSubsetDef subsetDef);

    }

}
