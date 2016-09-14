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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.AvhrrFile;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Reads radiances directly from the METOP prodcuts.
 *
 * @author Marco Zühlke
 */
class PlainBandReader implements BandReader {

    private static final String VIS_RADIANCE_UNIT = "W / (m^2 sr)";
    private static final String IR_RADIANCE_UNIT = "mW / (m^2 sr cm)";

    protected int channel;

    protected MetopFile metopFile;

    protected final ImageInputStream inputStream;

    PlainBandReader(int channel, AvhrrFile metopFile,
                    ImageInputStream inputStream) {
        this.channel = channel;
        this.metopFile = (MetopFile) metopFile;
        this.inputStream = inputStream;
    }

    @Override
    public String getBandName() {
        return AvhrrConstants.RADIANCE_BAND_NAME_PREFIX
               + AvhrrConstants.CH_STRINGS[channel];
    }

    @Override
    public String getBandUnit() {
        if (isVisibleBand()) {
            return VIS_RADIANCE_UNIT;
        } else {
            return IR_RADIANCE_UNIT;
        }
    }

    @Override
    public String getBandDescription() {
        if (isVisibleBand()) {
            return format(AvhrrConstants.RADIANCE_DESCRIPTION_VIS,
                          AvhrrConstants.CH_STRINGS[channel]);
        } else {
            return format(AvhrrConstants.RADIANCE_DESCRIPTION_IR,
                          AvhrrConstants.CH_STRINGS[channel]);
        }
    }

    @Override
    public double getScalingFactor() {
        if (channel == AvhrrConstants.CH_3A || channel == AvhrrConstants.CH_3B) {
            return 1E-4;
        } else {
            return 1E-2;
        }
    }

    @Override
    public int getDataType() {
        return ProductData.TYPE_INT16;
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
        final AvhrrFile.RawCoordinates rawCoord = metopFile.getRawCoordinates(
                sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight);
        final short[] targetData = (short[]) destBuffer.getElems();

        pm.beginTask(MessageFormat.format("Reading AVHRR band ''{0}''...", getBandName()),
                     rawCoord.maxY - rawCoord.minY);

        int targetIdx = rawCoord.targetStart;
        for (int sourceY = rawCoord.minY; sourceY <= rawCoord.maxY; sourceY += sourceStepY) {
            if (pm.isCanceled()) {
                break;
            }

            if (hasData(sourceY)) {
                final int dataOffset = getDataOffset(sourceOffsetX, sourceY);
                synchronized (inputStream) {
                    inputStream.seek(dataOffset);
                    inputStream.readFully(targetData, targetIdx, sourceWidth);
                }
                targetIdx += sourceWidth;
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

    protected int getDataOffset(int sourceOffsetX, int sourceY) {
        return metopFile.getScanLineOffset(sourceY)
               + 24
               + (AvhrrConstants.RAW_SCENE_RASTER_WIDTH * AvhrrConstants.CH_DATASET_INDEXES[channel] * 2)
               + sourceOffsetX * 2;
    }

    protected boolean isVisibleBand() {
        return channel == AvhrrConstants.CH_1 || channel == AvhrrConstants.CH_2
               || channel == AvhrrConstants.CH_3A;
    }

    protected boolean hasData(int rawY) throws IOException {
        if (channel != AvhrrConstants.CH_3A && channel != AvhrrConstants.CH_3B) {
            return true;
        }
        final int bitField = metopFile.readFrameIndicator(rawY);
        final int channel3ab = bitField & 1;

        return (channel3ab == 1 && channel == AvhrrConstants.CH_3A)
               || (channel3ab == 0 && channel == AvhrrConstants.CH_3B);
    }

    private static String format(String pattern, String arg) {
        return new MessageFormat(pattern).format(new Object[]{arg});
    }

}