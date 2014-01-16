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

package org.esa.cci.sst.rules;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.common.ExtractDefinition;
import org.esa.cci.sst.reader.InsituSource;
import org.esa.cci.sst.reader.Reader;
import org.junit.Ignore;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class AtsrCalibrationFilenameTest {

    @Test
    @Ignore
    public void testV1cAndG1cFilename() throws Exception {
        // @todo 2 tb/** file is not checked into git - tb 2014-01-16
        final String file = getClass().getResource("atsr_metadata.dim").getFile();
        final Product product = ProductIO.readProduct(file);

        final G1cFilename g1cFilename = new G1cFilename();
        final V1cFilename v1cFilename = new V1cFilename();
        final Context context = new Context() {
            @Override
            public Matchup getMatchup() {
                return null;
            }

            @Override
            public Reader getObservationReader() {
                return new Reader() {
                    @Override
                    public void init(DataFile dataFile, File archiveRoot) throws IOException {
                    }

                    @Override
                    public void close() {
                    }

                    @Override
                    public int getNumRecords() {
                        return 0;
                    }

                    @Override
                    public Observation readObservation(int recordNo) throws IOException {
                        return null;
                    }

                    @Override
                    public Array read(String role, ExtractDefinition extractDefinition) throws IOException {
                        return null;
                    }

                    @Override
                    public Item getColumn(String role) {
                        return null;
                    }

                    @Override
                    public Item[] getColumns() {
                        return new Item[0];
                    }

                    @Override
                    public DataFile getDatafile() {
                        return null;
                    }

                    @Override
                    public GeoCoding getGeoCoding(int recordNo) throws IOException {
                        return null;
                    }

                    @Override
                    public double getDTime(int recordNo, int scanLine) throws IOException {
                        return 0;
                    }

                    @Override
                    public long getTime(int recordNo, int scanLine) throws IOException {
                        return 0;
                    }

                    @Override
                    public int getLineSkip() {
                        return 0;
                    }

                    @Override
                    public InsituSource getInsituSource() {
                        return null;
                    }

                    @Override
                    public int getScanLineCount() {
                        return 0;
                    }

                    @Override
                    public int getElementCount() {
                        return 0;
                    }

                    @Override
                    public Product getProduct() {
                        return product;
                    }
                };
            }

            @Override
            public Reader getReferenceObservationReader() {
                return null;
            }

            @Override
            public Observation getObservation() {
                return null;
            }

            @Override
            public Variable getTargetVariable() {
                return null;
            }

            @Override
            public Map<String, Integer> getDimensionConfiguration() {
                return null;
            }
        };
        g1cFilename.setContext(context);
        v1cFilename.setContext(context);

        Array array = g1cFilename.apply(null, null);
        String expectedName = "ATS_GC1_AXVIEC20070720_093834_20020301_000000_20200101_000000";
        for (int i = 0; i < Math.min(expectedName.length(), array.getSize()); i++) {
            assertEquals(expectedName.charAt(i), array.getChar(i));
        }

        array = v1cFilename.apply(null, null);
        expectedName = "ATS_VC1_AXVIEC20100603_140547_20100601_214150_20100602_010310";
        for (int i = 0; i < Math.min(expectedName.length(), array.getSize()); i++) {
            assertEquals(expectedName.charAt(i), array.getChar(i));
        }


    }
}
