/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.util.accumulators;

import org.esa.cci.sst.util.NumberAccumulator;

/**
 * An {@link org.esa.cci.sst.util.NumberAccumulator} used for weighted, random uncertainty averaging.
 *
 * @author Norman Fomferra
 */
public class RandomUncertaintyAccumulator extends NumberAccumulator {

    private double sumXX;
    private double sumWW;
    private long sampleCount;

    @Override
    public long getSampleCount() {
        return sampleCount;
    }

    @Override
    protected void accumulateSample(double sample, double weight) {
        final double x = weight * sample;
        sumXX += x * x;
        sumWW += weight * weight;
        sampleCount++;
    }

    @Override
    public double computeAverage() {
        if (sampleCount == 0) {
            return Double.NaN;
        }
        if (sumXX == 0.0) {
            return 0.0;
        }
        if (sumWW == 0.0) {
            return Double.NaN;
        }
        final double meanSqr = sumXX / sumWW;
        return meanSqr > 0.0 ? Math.sqrt(meanSqr) : 0.0;
    }

}
