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

import org.esa.beam.common.PixelLocator;
import org.esa.beam.common.PixelLocatorAdapter;
import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.AvhrrReader;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.PixelLocatorFactory;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;


/**
 * A reader for METOP-AVHRR/3 Level-1b data products.
 *
 * @author Marco Zühlke
 */
class MetopReader extends AvhrrReader implements AvhrrConstants {

    MetopReader(ProductReaderPlugIn metopReaderPlugIn) {
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

        final MetopFile metopFile = (MetopFile) this.avhrrFile;
        addInternalTargetTemperatureBand(product, metopFile);
        addQualityIndicatorFlags(product, metopFile);
        addScanlineQualityFlags(product, metopFile);

        return product;
    }

    private void addInternalTargetTemperatureBand(Product product, MetopFile metopFile) {
        final InternalTargetTemperatureBandReader bandReader = metopFile.createInternalTargetTemperatureReader();
        final Band band = product.addBand(bandReader.getBandName(), bandReader.getDataType());
        band.setScalingFactor(bandReader.getScalingFactor());
        band.setScalingOffset(bandReader.getScalingOffset());
        band.setUnit(bandReader.getBandUnit());
        band.setDescription(bandReader.getBandDescription());
        band.setNoDataValue(Short.MIN_VALUE);
        band.setNoDataValueUsed(true);
        bandReaders.put(band, bandReader);
    }

    private void addQualityIndicatorFlags(Product product, MetopFile metopFile) {
        final ScanlineValueIntReader bandReader = metopFile.createQualityIndicatorReader();

        final Band band = product.addBand(bandReader.getBandName(), bandReader.getDataType());
        band.setScalingFactor(bandReader.getScalingFactor());
        band.setUnit(bandReader.getBandUnit());
        band.setDescription(bandReader.getBandDescription());
        band.setNoDataValue(Integer.MIN_VALUE);
        band.setNoDataValueUsed(true);

        final FlagCoding flagCoding = createQualityIndicatorFlagCoding(bandReader.getBandName());
        band.setSampleCoding(flagCoding);
        product.getFlagCodingGroup().add(flagCoding);

        bandReaders.put(band, bandReader);
    }

    private void addScanlineQualityFlags(Product product, MetopFile metopFile) {
        final ScanlineValueIntReader bandReader = metopFile.createScanlineQualityReader();

        final Band band = product.addBand(bandReader.getBandName(), bandReader.getDataType());
        band.setScalingFactor(bandReader.getScalingFactor());
        band.setUnit(bandReader.getBandUnit());
        band.setDescription(bandReader.getBandDescription());
        band.setNoDataValue(Integer.MIN_VALUE);
        band.setNoDataValueUsed(true);

        final FlagCoding flagCoding = createScanlineQualityFlagCoding(bandReader.getBandName());
        band.setSampleCoding(flagCoding);
        product.getFlagCodingGroup().add(flagCoding);

        bandReaders.put(band, bandReader);
    }

    private FlagCoding createQualityIndicatorFlagCoding(String bandName) {
        FlagCoding fc = new FlagCoding(bandName);
        fc.setDescription("Flag coding for " + bandName);

        addFlagAndBitmaskDef(fc, "DO_NOT_USE_SCAN", "Do not use scan for product generation", 31);
        addFlagAndBitmaskDef(fc, "TIME_SEQ_ERR", "Time sequence error detected with this scan", 30);
        addFlagAndBitmaskDef(fc, "DATA_GAP_PRE", "Data gap precedes this scan", 29);
        addFlagAndBitmaskDef(fc, "INSUFF_DATA_CAL", "Insufficient data for calibration", 28);
        addFlagAndBitmaskDef(fc, "LOC_DATA_MISS", "Earth location data not available", 27);
        addFlagAndBitmaskDef(fc, "CLOCK_UPDATE", "First good time following a clock update (nominally 0)", 26);
        addFlagAndBitmaskDef(fc, "STATUS_CHANGE", "Instrument status changed with this scan", 25);
        addFlagAndBitmaskDef(fc, "SYNC_LOCK_DROP", "Sync lock dropped during this frame - DEFAULT TO ZERO", 24);
        addFlagAndBitmaskDef(fc, "FRAME_SYNC_ERR", "Frame sync word error greater than zero- DEFAULT TO ZERO", 23);
        addFlagAndBitmaskDef(fc, "FRAME_SYNC_DROP", "Frame sync previously dropped lock- DEFAULT TO ZERO", 22);
        addFlagAndBitmaskDef(fc, "FLYWHEEL", "Flywheeling detected during this frame- DEFAULT TO ZERO", 21);
        addFlagAndBitmaskDef(fc, "BIT_SLIP", "Bit slippage detected during this frame- DEFAULT TO ZERO", 20);
        // eleven unused bits follow tb 2016-06-15
        addFlagAndBitmaskDef(fc, "TIP_PARITY", "TIP parity error detected- DEFAULT TO ZERO", 8);
        addFlagAndBitmaskDef(fc, "REFL_SUN_3b_I", "Reflected sunlight detected ch 3b (0 = no anomaly; 1 = anomaly; 3 = unsure) - part 1", 7);
        addFlagAndBitmaskDef(fc, "REFL_SUN_3b_II", "Reflected sunlight detected ch 3b (0 = no anomaly; 1 = anomaly; 3 = unsure) - part 2", 6);
        addFlagAndBitmaskDef(fc, "REFL_SUN_4_I", "Reflected sunlight detected ch 4 (0 = no anomaly; 1 = anomaly; 3 = unsure) - part 1", 5);
        addFlagAndBitmaskDef(fc, "REFL_SUN_4_II", "Reflected sunlight detected ch 4 (0 = no anomaly; 1 = anomaly; 3 = unsure) - part 2", 4);
        addFlagAndBitmaskDef(fc, "REFL_SUN_5_I", "Reflected sunlight detected ch 5 (0 = no anomaly; 1 = anomaly; 3 = unsure) - part 1", 3);
        addFlagAndBitmaskDef(fc, "REFL_SUN_5_II", "Reflected sunlight detected ch 5 (0 = no anomaly; 1 = anomaly; 3 = unsure) - part 2", 2);
        addFlagAndBitmaskDef(fc, "RESYNC", "Resync occurred on this frame- DEFAULT TO ZERO", 1);
        addFlagAndBitmaskDef(fc, "PSEUDO_NOISE", "Pseudo noise occurred on this frame", 0);

        return fc;
    }

    private FlagCoding createScanlineQualityFlagCoding(String bandName) {
        final FlagCoding fc = new FlagCoding(bandName);
        fc.setDescription("Flag coding for " + bandName);

        // starts with 8 unused bit fields tb 2016-06-15
        addFlagAndBitmaskDef(fc, "TIME_FIELD_INF", "Time field is bad but can probably be inferred from the previous good time", 23);
        addFlagAndBitmaskDef(fc, "TIME_FIELD_NOT_INF", "Time field is bad and can’t be inferred from the previous good time", 22);
        addFlagAndBitmaskDef(fc, "TIME_INCONSISTENT", "This record starts a sequence that is inconsistent with previous times (i.e., there is a time discontinuity). This may or may not be associated with a spacecraft clock update (See bit 26 in QUALITY_INDICATOR Field)", 21);
        addFlagAndBitmaskDef(fc, "SCAN_TIME_REPEAT", "Start of a sequence that apparently repeats scan times that have been previously accepted", 20);
        // 4 unused bit fields tb 2016-06-15
        addFlagAndBitmaskDef(fc, "SCAN_UNCALIB_TIME", "Scan line was not calibrated because of bad time", 15);
        addFlagAndBitmaskDef(fc, "SCAN_CALIB_DATA_GAP", "Scan line was calibrated using fewer than the preferred number of scan lines because of proximity to start or end of data set or to a data gap", 14);
        addFlagAndBitmaskDef(fc, "SCAN_UNCALIB_BAD_PRT", "Scan line was not calibrated because of bad or insufficient PRT data", 13);
        addFlagAndBitmaskDef(fc, "SCAN_CALIB_MARG_PRT", "Scan line was calibrated but with marginal PRT data", 12);
        addFlagAndBitmaskDef(fc, "SCAN_UNCALIB_CHAN", "Some uncalibrated channels on this scan. (See channel indicators.)", 11);
        addFlagAndBitmaskDef(fc, "SCAN_UNCALIB_INSTR", "Uncalibrated due to instrument mode.", 10);
        addFlagAndBitmaskDef(fc, "SCAN_CALIB_SPACE_VIEW", "Questionable calibration because of antenna position error of space view", 9);
        addFlagAndBitmaskDef(fc, "SCAN_CALIB_BB_VIEW", "Questionable calibration because of antenna position error of black body", 8);
        addFlagAndBitmaskDef(fc, "MISS_EARTH_VIEW_TIME", "Not earth located because of bad time; earth location fields zero filled", 7);
        addFlagAndBitmaskDef(fc, "BAD_EARTH_LOC_TIME", "Earth location questionable because of questionable time code. (See time problem flags above.)", 6);
        addFlagAndBitmaskDef(fc, "BAD_EARTH_LOC_MARG", "Earth location questionable - only marginal agreement with reasonableness check.", 5);
        addFlagAndBitmaskDef(fc, "BAD_EARTH_LOC_REAS", "Earth location questionable - fails reasonableness check", 4);
        addFlagAndBitmaskDef(fc, "BAD_EARTH_LOC_ANT", "Earth location questionable because of antenna position check", 3);
        // 3 unused bit fields tb 2016-06-16

        return fc;
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

        final TiePointGrid lonGrid = grid[numGrids - 1];
        final TiePointGrid latGrid = grid[numGrids - 2];
        final PixelLocator pixelLocator = PixelLocatorFactory.forSwath(lonGrid, latGrid, 17);
        final GeoCoding geoCoding = new PixelLocatorAdapter(pixelLocator);

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

            addFlagAndBitmaskDef(fc, "uniformity_test2", "Uniformity test (0='test failed' or 'clear'; 1='cloudy')", 15);
            addFlagAndBitmaskDef(fc, "uniformity_test1", "Uniformity test (0 ='test failed' or 'cloudy', 1='clear')", 14);
            addFlagAndBitmaskDef(fc, "t3_t5_test2", "T3-T5 test (0='test failed' or 'clear'; 1='cloudy')", 13);
            addFlagAndBitmaskDef(fc, "t3_t5_test1", "T3-T5 test (0 ='test failed' or 'cloudy', 1='clear')", 12);
            addFlagAndBitmaskDef(fc, "t4_t3_test2", "T4-T3 test (0='test failed' or 'clear'; 1='cloudy')", 11);
            addFlagAndBitmaskDef(fc, "t4_t3_test1", "T4-T3 test (0 ='test failed' or 'cloudy', 1='clear')", 10);
            addFlagAndBitmaskDef(fc, "t4_t5_test2", "T4-T5 test (0='test failed' or 'clear'; 1='cloudy')", 9);
            addFlagAndBitmaskDef(fc, "t4_t5_test1", "T4-T5 test (0 ='test failed' or 'cloudy', 1='clear')", 8);
            addFlagAndBitmaskDef(fc, "albedo_test2", "Albedo test (0='test failed' or 'clear'; 1='cloudy' or 'snow/ice covered')", 7);
            addFlagAndBitmaskDef(fc, "albedo_test1", "Albedo test (0 ='test failed' or 'cloudy', 1='clear' or 'snow/ice covered')", 6);
            addFlagAndBitmaskDef(fc, "t4_test2", "T4 test (0='test failed' or 'clear'; 1='cloudy' or 'snow/ice covered')", 5);
            addFlagAndBitmaskDef(fc, "t4_test1", "T4 test (0 ='test failed' or 'cloudy', 1='clear' or 'snow/ice covered')", 4);

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

    static boolean canOpenFile(File file) {
        try {
            return MetopFile.canOpenFile(file);
        } catch (IOException e) {
            return false;
        }
    }

}