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
import org.esa.beam.dataio.avhrr.AvhrrFile;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Reads cloud information directly from the METOP product.
 *
 * @author Marco Zühlke
 */
class CloudBandReader implements BandReader {

    protected MetopFile metopFile;

    protected final ImageInputStream inputStream;

    CloudBandReader(MetopFile metopFile,
                           ImageInputStream inputStream) {
        this.metopFile = metopFile;
        this.inputStream = inputStream;
    }

    @Override
    public String getBandDescription() {
        return "CLAVR-x cloud mask";
    }

    @Override
    public String getBandName() {
        return "cloud_flags";
    }

    @Override
    public String getBandUnit() {
        return null;
    }

    @Override
    public int getDataType() {
        return ProductData.TYPE_UINT16;
    }

    @Override
    public double getScalingFactor() {
        return 1.0;
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
        final short[] targetData = (short[]) destBuffer.getElems();

        pm.beginTask(MessageFormat.format("Reading AVHRR band ''{0}''...", getBandName()),
                     rawCoord.maxY - rawCoord.minY);

        int targetIdx = rawCoord.targetStart;
        for (int sourceY = rawCoord.minY; sourceY <= rawCoord.maxY; sourceY += sourceStepY) {
            if (pm.isCanceled()) {
                break;
            }

            final int dataOffset = getDataOffset(sourceOffsetX, sourceY);
            synchronized (inputStream) {
                inputStream.seek(dataOffset);
                inputStream.readFully(targetData, targetIdx, sourceWidth);
            }
            targetIdx += sourceWidth;
            pm.worked(1);
        }
        pm.done();

    }

    protected int getDataOffset(int sourceOffsetX, int sourceY) {
        return metopFile.getScanLineOffset(sourceY) + 22472  + sourceOffsetX * 2;
    }
}