/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.metop;

import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.AvhrrReader;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.dataop.maptransf.Datum;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;


/**
 * A reader for METOP-AVHRR/3 Level-1b data products.
 *
 * @author Marco Zühlke
 */
public class MetopReader extends AvhrrReader implements AvhrrConstants {

    public MetopReader(ProductReaderPlugIn metopReaderPlugIn) {
        super(metopReaderPlugIn);
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code>
     * interface method. Clients implementing this method can be sure that the
     * input object and eventually the subset information has already been set.
     * <p/>
     * <p/>
     * This method is called as a last step in the
     * <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File dataFile = MetopReaderPlugIn.getInputFile(getInput());

        try {
            ImageInputStream imageInputStream = new FileImageInputStream(dataFile);
            avhrrFile = new MetopFile(imageInputStream);
            avhrrFile.readHeader();
            createProduct();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                close();
            } catch (IOException ignored) {
                // ignore
            }
            throw e;
        }
        product.setFileLocation(dataFile);
        product.setPreferredTileSize(product.getSceneRasterWidth(), 512);

        return product;
    }

    @Override
    protected void addTiePointGrids() throws IOException {
        final MetopFile metopFile = (MetopFile) avhrrFile;
        final int tiePointSampleRate = metopFile.getNavSampleRate();
        final int tiePointGridHeight = metopFile.getProductHeight();
        final int tiePointGridWidth = metopFile.getNumNavPoints();

        final String[] tiePointNames = avhrrFile.getTiePointNames();
        final float[][] tiePointData = avhrrFile.getTiePointData();

        final int numGrids = tiePointNames.length;
        TiePointGrid grid[] = new TiePointGrid[numGrids];

        for (int i = 0; i < grid.length; i++) {
            grid[i] = createTiePointGrid(tiePointNames[i],
                                         tiePointGridWidth,
                                         tiePointGridHeight,
                                         TP_OFFSET_X,
                                         TP_OFFSET_Y,
                                         tiePointSampleRate,
                                         1,
                                         tiePointData[i]);
            grid[i].setUnit(UNIT_DEG);
            product.addTiePointGrid(grid[i]);
        }
        addDeltaAzimuth(tiePointGridWidth, tiePointGridHeight, tiePointSampleRate);

        final GeoCoding geoCoding = new MetopGeoCoding(grid[numGrids - 2], grid[numGrids - 1]);
        product.setGeoCoding(geoCoding);
    }

    @Override
    protected void addCloudBand() {
        if (avhrrFile.hasCloudBand()) {
            BandReader cloudReader = avhrrFile.createCloudBandReader();
            Band cloudBand = new Band(cloudReader.getBandName(),
                                      cloudReader.getDataType(), avhrrFile.getProductWidth(),
                                      avhrrFile.getProductHeight());

            FlagCoding fc = new FlagCoding(cloudReader.getBandName());
            fc.setDescription("Flag coding for CLOUD_INFORMATION");

            addFlagAndBitmaskDef(fc, "uniformity_test2", "Uniformity test (0='test failed' or 'clear'; 1='cloudy')",
                                 15);
            addFlagAndBitmaskDef(fc, "uniformity_test1", "Uniformity test (0 ='test failed' or 'cloudy', 1='clear')",
                                 14);
            addFlagAndBitmaskDef(fc, "t3_t5_test2", "T3-T5 test (0='test failed' or 'clear'; 1='cloudy')", 13);
            addFlagAndBitmaskDef(fc, "t3_t5_test1", "T3-T5 test (0 ='test failed' or 'cloudy', 1='clear')", 12);
            addFlagAndBitmaskDef(fc, "t4_t3_test2", "T4-T3 test (0='test failed' or 'clear'; 1='cloudy')", 11);
            addFlagAndBitmaskDef(fc, "t4_t3_test1", "T4-T3 test (0 ='test failed' or 'cloudy', 1='clear')", 10);
            addFlagAndBitmaskDef(fc, "t4_t5_test2", "T4-T5 test (0='test failed' or 'clear'; 1='cloudy')", 9);
            addFlagAndBitmaskDef(fc, "t4_t5_test1", "T4-T5 test (0 ='test failed' or 'cloudy', 1='clear')", 8);
            addFlagAndBitmaskDef(fc, "albedo_test2",
                                 "Albedo test (0='test failed' or 'clear'; 1='cloudy' or 'snow/ice covered')", 7);
            addFlagAndBitmaskDef(fc, "albedo_test1",
                                 "Albedo test (0 ='test failed' or 'cloudy', 1='clear' or 'snow/ice covered')", 6);
            addFlagAndBitmaskDef(fc, "t4_test2",
                                 "T4 test (0='test failed' or 'clear'; 1='cloudy' or 'snow/ice covered')", 5);
            addFlagAndBitmaskDef(fc, "t4_test1",
                                 "T4 test (0 ='test failed' or 'cloudy', 1='clear' or 'snow/ice covered')", 4);

            cloudBand.setSampleCoding(fc);
            product.getFlagCodingGroup().add(fc);
            product.addBand(cloudBand);
            bandReaders.put(cloudBand, cloudReader);
        }
    }

    private void addDeltaAzimuth(int tiePointGridWidth, int tiePointGridHeight, int tiePointSampleRate) {
        float[] sunAzimuthTiePointData = product.getTiePointGrid(SAA_DS_NAME).getTiePoints();
        float[] viewAzimuthTiePointData = product.getTiePointGrid(VAA_DS_NAME).getTiePoints();
        final int numTiePoints = viewAzimuthTiePointData.length;
        float[] deltaAzimuthData = new float[numTiePoints];

        for (int i = 0; i < numTiePoints; i++) {
            deltaAzimuthData[i] = (float) computeAda(viewAzimuthTiePointData[i], sunAzimuthTiePointData[i]);
        }

        TiePointGrid grid = createTiePointGrid(DAA_DS_NAME, tiePointGridWidth,
                                               tiePointGridHeight, TP_OFFSET_X, TP_OFFSET_Y, tiePointSampleRate,
                                               tiePointSampleRate, deltaAzimuthData);
        grid.setUnit(UNIT_DEG);
        product.addTiePointGrid(grid);
    }

    /**
     * Computes the azimuth difference from the given
     *
     * @param vaa viewing azimuth angle [degree]
     * @param saa sun azimuth angle [degree]
     *
     * @return the azimuth difference [degree]
     */
    private static double computeAda(double vaa, double saa) {
        double ada = saa - vaa;
        if (ada <= -180.0) {
            ada += 360.0;
        } else if (ada > +180.0) {
            ada -= 360.0;
        }
        return ada;
    }

    public static boolean canOpenFile(File file) {
        try {
            return MetopFile.canOpenFile(file);
        } catch (IOException e) {
            return false;
        }
    }

}