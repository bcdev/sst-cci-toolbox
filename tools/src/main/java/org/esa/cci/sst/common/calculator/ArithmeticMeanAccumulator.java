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

package org.esa.cci.sst.common.calculator;

/**
 * An {@link NumberAccumulator} used for weighted, arithmetic mean averaging.
 * (See Eq. 1.2 in Nicks RegionalAverageTool spec, draft5)
 *
 * @author Norman Fomferra
 */
public final class ArithmeticMeanAccumulator extends NumberAccumulator {

    private double sumX;
    private double sumW;
    protected int sampleCount;

    @Override
    public int getSampleCount() {
        return sampleCount;
    }

    @Override
    protected void accumulateSample(double sample, double weight) {
        sumX += weight * sample;
        sumW += weight;
        sampleCount++;
    }

    @Override
    public double combine() {
        if (sampleCount == 0) {
            return Double.NaN;
        }
        if (sumX == 0.0) {
            return 0.0;
        }
        if (sumW == 0.0) {
            return Double.NaN;
        }
        return sumX / sumW;
    }
}
