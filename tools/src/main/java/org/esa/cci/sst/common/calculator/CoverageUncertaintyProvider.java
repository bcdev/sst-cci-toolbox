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

import org.esa.cci.sst.regrid.SpatialResolution;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Access to coverage uncertainties.
 *
 * @author Norman Fomnferra
 */
public abstract class CoverageUncertaintyProvider {

    private final int month;
    private final SpatialResolution spatialResolution;
    //todo Coverage Uncertainty for other target resolutions than 5 or 90 °

    protected CoverageUncertaintyProvider(int month, SpatialResolution spatialResolution) {
        this.month = month;
        this.spatialResolution = spatialResolution;
    }

    protected CoverageUncertaintyProvider(int month) {
        this(month, SpatialResolution.DEGREE_5_00);
    }

    /**
     * Returns the coverage uncertainty for a 90° cell.
     *
     * @param cellX The 90° cell X index.
     * @param cellY The 90° cell Y index.
     * @param n     The number of 5° grid boxes contributing to the 90° cell.
     * @return The coverage uncertainty for a 90° cell
     */
    public double calculateCoverageUncertainty90(int cellX, int cellY, long n) {
        if (n == 0L) {
            return Double.NaN;
        }
        final double s = getMagnitude90(cellX, cellY, month);
        return s / sqrt(n);
    }

    protected abstract double getMagnitude90(int cellX, int cellY, int month);

    /**
     * Returns the coverage uncertainty for a 5° cell.
     *
     * @param cellX The 5° cell X index.
     * @param cellY The 5° cell Y index.
     * @param n     The number of observations contributing to the 5° cell.
     * @return The coverage uncertainty for a 5° cell
     */
    public double calculateCoverageUncertainty5(int cellX, int cellY, long n) {
        if (!SpatialResolution.DEGREE_5_00.equals(this.spatialResolution)) {
            return 0.0;
        }

        if (n == 0L) {
            return Double.NaN;
        }
        final double s0 = getMagnitude5(cellX, cellY);
        final double p = getExponent5(cellX, cellY);
        final double f = n / 77500.0;
        return s0 * (1.0 - pow(f, p));
    }

    protected abstract double getMagnitude5(int cellX, int cellY);

    protected abstract double getExponent5(int cellX, int cellY);
}
