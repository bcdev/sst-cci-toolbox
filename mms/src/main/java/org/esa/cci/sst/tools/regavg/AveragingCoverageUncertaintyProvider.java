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

package org.esa.cci.sst.tools.regavg;

import org.esa.cci.sst.common.CoverageUncertaintyProvider;
import org.esa.cci.sst.common.AggregationCell;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Access to coverage uncertainties.
 *
 * @author Norman Fomnferra
 */
abstract class AveragingCoverageUncertaintyProvider implements CoverageUncertaintyProvider {

    private final int month;

    protected AveragingCoverageUncertaintyProvider(int month) {
        this.month = month;
    }

    @Override
    public final double calculate(AggregationCell cell, double spatialResolution) {
        return calculate(cell.getX(), cell.getY(), cell.getSampleCount(), spatialResolution);
    }

    final double calculate(int cellX, int cellY, long sampleCount, double spatialResolution) {
        if (spatialResolution == 5.0) {
            return calculateCoverageUncertainty5(cellX, cellY, sampleCount);
        } else if (spatialResolution == 90.0) {
            return calculateCoverageUncertainty90(cellX, cellY, sampleCount);
        } else {
            return Double.NaN;
        }
    }

    protected abstract double getMagnitude90(int cellX, int cellY, int month);

    protected abstract double getMagnitude5(int cellX, int cellY);

    protected abstract double getExponent5(int cellX, int cellY);

    private double calculateCoverageUncertainty5(int cellX, int cellY, long sampleCount) {
        if (sampleCount == 0L) {
            return Double.NaN;
        }
        final double s0 = getMagnitude5(cellX, cellY);
        final double p = getExponent5(cellX, cellY);
        final double f = sampleCount / 77500.0;

        return s0 * (1.0 - pow(f, p));
    }

    private double calculateCoverageUncertainty90(int cellX, int cellY, long sampleCount) {
        if (sampleCount == 0L) {
            return Double.NaN;
        }
        final double s = getMagnitude90(cellX, cellY, month);

        return s / sqrt(sampleCount);
    }
}
