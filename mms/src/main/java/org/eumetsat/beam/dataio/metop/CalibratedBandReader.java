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

package org.eumetsat.beam.dataio.metop;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.AvhrrFile;
import org.esa.beam.dataio.avhrr.calibration.RadianceCalibrator;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Reads radiances from the METOP prodcuts and transforms them into
 * a reflectance factor or a temperature.
 *
 * @author marcoz
 * @version $Revision: 1.1.1.1 $ $Date: 2007/03/22 11:12:51 $
 */
class CalibratedBandReader extends PlainBandReader {

    private final RadianceCalibrator calibrator;

    public CalibratedBandReader(int channel, MetopFile metopFile,
                                ImageInputStream inputStream, RadianceCalibrator radianceCalibrator) {
        super(channel, metopFile, inputStream);
        calibrator = radianceCalibrator;
    }

    @Override
    public String getBandName() {
        if (isVisibleBand()) {
            return AvhrrConstants.REFLECTANCE_BAND_NAME_PREFIX
                    + AvhrrConstants.CH_STRINGS[channel];
        } else {
            return AvhrrConstants.TEMPERATURE_BAND_NAME_PREFIX
                    + AvhrrConstants.CH_STRINGS[channel];
        }
    }

    @Override
    public String getBandUnit() {
        if (isVisibleBand()) {
            return AvhrrConstants.REFLECTANCE_UNIT;
        } else {
            return AvhrrConstants.TEMPERATURE_UNIT;
        }
    }

    @Override
    public String getBandDescription() {
        if (isVisibleBand()) {
            return format(AvhrrConstants.REFLECTANCE_FACTOR_DESCRIPTION, AvhrrConstants.CH_STRINGS[channel]);
        } else {
            return format(AvhrrConstants.TEMPERATURE_DESCRIPTION, AvhrrConstants.CH_STRINGS[channel]);
        }
    }

    @Override
    public double getScalingFactor() {
        return 1.0;
    }

    @Override
    public int getDataType() {
        return ProductData.TYPE_FLOAT32;
    }

    @Override
    public void readBandRasterData(int sourceOffsetX,
                                   int sourceOffsetY,
                                   int sourceWidth,
                                   int sourceHeight,
                                   int sourceStepX,
                                   int sourceStepY,
                                   final ProductData destBuffer,
                                   final ProgressMonitor pm) throws IOException {

        AvhrrFile.RawCoordinates rawCoord = metopFile.getRawCoordinates(
                sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight);
        final float[] targetData = (float[]) destBuffer.getElems();
        final float scalingFactor = (float) super.getScalingFactor();

        pm.beginTask(MessageFormat.format("Reading AVHRR band ''{0}''...", getBandName()), rawCoord.maxY - rawCoord.minY);

        int targetIdx = rawCoord.targetStart;
        for (int sourceY = rawCoord.minY; sourceY <= rawCoord.maxY; sourceY += sourceStepY) {
            if (pm.isCanceled()) {
                break;
            }

            if (hasData(sourceY)) {
                final int dataOffset = getDataOffset(sourceOffsetX, sourceY);
                synchronized (inputStream) {
                    final short[] radianceScanLine = new short[metopFile.getProductWidth()];
                    inputStream.seek(dataOffset);
                    inputStream.readFully(radianceScanLine, 0, sourceWidth);

                    for (int sourceX = 0; sourceX <= sourceWidth - 1; sourceX++) {
                        targetData[targetIdx] = calibrator
                                .calibrate(radianceScanLine[sourceX] * scalingFactor);
                        targetIdx += rawCoord.targetIncrement;
                    }
                }
            } else {
                for (int sourceX = rawCoord.minX; sourceX <= rawCoord.maxX; sourceX += sourceStepX) {
                    targetData[targetIdx] = AvhrrConstants.NO_DATA_VALUE;
                    targetIdx += rawCoord.targetIncrement;
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    private static String format(String pattern, String arg) {
        return new MessageFormat(pattern).format(new Object[]{arg});
    }

}
