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

package org.esa.cci.sst.regavg;

import org.esa.cci.sst.common.calculator.CoverageUncertainty;
import org.esa.cci.sst.regrid.SpatialResolution;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Access to coverage uncertainties.
 *
 * @author Norman Fomnferra
 */
abstract class AveragingCoverageUncertainty implements CoverageUncertainty {

    private final int month;
    private final SpatialResolution spatialResolution;

    protected AveragingCoverageUncertainty(int month) {
        this(month, SpatialResolution.DEGREE_5_00);
    }

    protected AveragingCoverageUncertainty(int month, SpatialResolution spatialResolution) {
        this.month = month;
        this.spatialResolution = spatialResolution;
    }

    /**
     * Returns the coverage uncertainty for a 5° or 90° cell. Returns 0.0 if another resolution is demanded.
     *
     * @param cellX      The cell X index.
     * @param cellY      The cell Y index.
     * @param n          The number of observations contributing to a cell.
     * @param resolution The resolution of the cell grid (either 5° or 90°).
     *
     * @return The coverage uncertainty for a cell.
     */
    @Override
    public double calculate(int cellX, int cellY, long n, double resolution) {
        if (resolution == 5.0) {
            return calculateCoverageUncertainty5(cellX, cellY, n);
        } else if (resolution == 90.0) {
            return calculateCoverageUncertainty90(cellX, cellY, n);
        } else {
            return 0.0;
        }
    }

    protected abstract double getMagnitude90(int cellX, int cellY, int month);

    protected abstract double getMagnitude5(int cellX, int cellY);

    protected abstract double getExponent5(int cellX, int cellY);

    private double calculateCoverageUncertainty5(int cellX, int cellY, long n) {
        if (n == 0L) {
            return Double.NaN;
        }
        final double s0 = getMagnitude5(cellX, cellY);
        final double p = getExponent5(cellX, cellY);
        final double f = n / 77500.0;

        return s0 * (1.0 - pow(f, p));
    }

    private double calculateCoverageUncertainty90(int cellX, int cellY, long n) {
        if (n == 0L) {
            return Double.NaN;
        }
        final double s = getMagnitude90(cellX, cellY, month);

        return s / sqrt(n);
    }
}
