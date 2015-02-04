/*
 * Copyright (c) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 * @author Ralf Quast
 */
final class InternalTargetTemperatureBandReader implements BandReader {

    private static final int INTERNAL_TARGET_TEMPERATURE_COUNT_OFFSET = 26602;

    private static final double SCALING_FACTOR = 0.002;
    private static final double SCALING_OFFSET = 260.0;

    private final MetopFile metopFile;
    private final ImageInputStream inputStream;
    private final GiadrRadiance giadrRadiance;

    public InternalTargetTemperatureBandReader(MetopFile metopFile,
                                               ImageInputStream inputStream,
                                               GiadrRadiance giadrRadiance) {
        this.metopFile = metopFile;
        this.inputStream = inputStream;
        this.giadrRadiance = giadrRadiance;
    }

    @Override
    public String getBandName() {
        return "internal_target_temperature";
    }

    @Override
    public String getBandUnit() {
        return "K";
    }

    @Override
    public String getBandDescription() {
        return "Temperature derived from readings of internal platinum resistance thermometers (PRTs)";
    }

    @Override
    public double getScalingFactor() {
        return SCALING_FACTOR;
    }

    public double getScalingOffset() { return SCALING_OFFSET; }

    @Override
    public int getDataType() {
        return ProductData.TYPE_INT16;
    }

    @Override
    public void readBandRasterData(int sourceOffsetX,
                                   int sourceOffsetY,
                                   int sourceWidth, int sourceHeight,
                                   int sourceStepX, int sourceStepY,
                                   ProductData destBuffer,
                                   ProgressMonitor pm) throws IOException {
        final AvhrrFile.RawCoordinates rawCoord =
                metopFile.getRawCoordinates(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight);
        final short[] targetData = (short[]) destBuffer.getElems();

        final int minY = Math.max(rawCoord.minY - 4, 0);
        final int maxY = Math.min(rawCoord.maxY + 4, metopFile.getProductHeight() - 1);
        final short[] samples = readTemperatureSamples(minY, maxY);
        final int[] thermometerIndices = getThermometerIndices(samples);

        pm.beginTask(MessageFormat.format("Reading AVHRR band ''{0}''...", getBandName()),
                     rawCoord.maxY - rawCoord.minY);

        int targetIdx = rawCoord.targetStart;
        for (int sourceY = rawCoord.minY; sourceY <= rawCoord.maxY; sourceY += sourceStepY) {
            if (pm.isCanceled()) {
                break;
            }
            final int sampleY = sourceY - minY;
            final int thermometerIndex = thermometerIndices[sampleY];
            final short sample;
            if (thermometerIndex != -1) {
                final double temperature = computeInternalTargetTemperature(thermometerIndex, 3 * sampleY, samples);
                sample = (short) Math.round((temperature - SCALING_OFFSET) / SCALING_FACTOR);
            } else {
                sample = Short.MIN_VALUE;
            }
            for (int sourceX = rawCoord.minX; sourceX <= rawCoord.maxX; sourceX += sourceStepX) {
                targetData[targetIdx] = sample;
                targetIdx += rawCoord.targetIncrement;
            }
            pm.worked(1);
        }
        pm.done();
    }

    private short[] readTemperatureSamples(int minY, int maxY) throws IOException {
        final short[] targetData = new short[3 * maxY];

        for (int y = minY, targetOffset = 0; y < maxY; y++) {
            final int sourceOffset = metopFile.getScanLineOffset(y) + INTERNAL_TARGET_TEMPERATURE_COUNT_OFFSET;
            synchronized (inputStream) {
                inputStream.seek(sourceOffset);
                inputStream.readFully(targetData, targetOffset, 3);
            }
            targetOffset += 3;
        }

        return targetData;
    }

    /*
    Jon's explanation of calculating PRT temps from scratch:

    having looked at the EUMETSAT format regarding the ICT temperature you have to do some calculation to get the values (the same is true of the NOAA Level 1B format).  So you need from GIADR

    IR_TEMPERATURE(1-4)_COEFFICIENT(1-6)

    which are the coefficients to turn the stored count value to a temperature (4 PRTs, 6 coefficients though at the moment I only use the first 5 since I've never seen coefficient6 at anything other than zero - might be worth putting it in in case MetOp-B/C have it filled).

    You then need from the MDR

    INTERNAL_TARGET_TEMPERATURE_COUNT

    which is three readings of the same PRT which cycles through the 4 PRTs scan line by scan line with a zero (0) for the fifth in the sequence.  Then

    PRTN = Sum(1-6) IR_TEMPERATURE(N)_COEFFICIENTS(I) *  INTERNAL_TARGET_TEMPERATURE_COUNT^(I-1)

    for the Nth PRT (1-4).

    So to get the PRT temperature for a given scan line you need the INTERNAL_TARGET_TEMPERATURE_COUNT value (3 measurements per scane line) for the previous 4 scan lines plus the one of interest so you can work out where you are in the sequence of PRTs and you can then get the temperatures.
     */

    private double computeInternalTargetTemperature(int thermometerIndex, int sampleIndex, short[] samples) {
        return (giadrRadiance.getIrTempCoeffSum(thermometerIndex) * (samples[sampleIndex] + samples[sampleIndex + 1] + samples[sampleIndex + 2])) / 18.0;
    }

    // not private for the purpose of testing only
    static int[] getThermometerIndices(short[] samples) {
        final int[] indices = new int[samples.length / 3];

        int zeroIndex = 0;
        for (int i = 0, y = 0; y < 5; i += 3, y++) {
            if (samples[i] != 0 || samples[i + 1] != 0 || samples[i + 2] != 0) {
                zeroIndex++;
            } else {
                break;
            }
        }
        for (int i = 0, y = zeroIndex; y-- > 0; i++) {
            indices[y] = 3 - i;
        }
        for (int i = 0, y = zeroIndex; y < indices.length; i++, y++) {
            if (i % 5 == 0) {
                indices[y] = -1;
            } else {
                indices[y] = indices[y - 1] + 1;
            }
        }

        return indices;
    }
}
