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

package org.esa.cci.sst.accumulate;

/**
 * For (non-weighted) accumulating of synoptic and adjustment uncertainties.
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public final class UncertaintyAccumulator extends NumberAccumulator {

    private double sumXX;
    private int sampleCount;

    @Override
    public int getSampleCount() {
        return sampleCount;
    }

    @Override
    protected void accumulateSample(double sample, double weight) {
        if (weight != 1.0) {
            throw new IllegalArgumentException("weight != 1.0");
        }
        sumXX += sample * sample;
        sampleCount++;
    }

    @Override
    public double combine() {
        if (sampleCount == 0) {
            return Double.NaN;
        }
        if (sumXX == 0.0) {
            return 0.0;
        }

        return Math.sqrt(sumXX);
    }
}
