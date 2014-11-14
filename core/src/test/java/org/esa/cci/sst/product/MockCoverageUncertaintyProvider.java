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

package org.esa.cci.sst.product;

import org.esa.cci.sst.aggregate.CoverageUncertaintyProvider;
import org.esa.cci.sst.aggregate.AggregationCell;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

final class MockCoverageUncertaintyProvider implements CoverageUncertaintyProvider {

    private final double magnitude90;
    private final double magnitude5;
    private final double exponent5;

    MockCoverageUncertaintyProvider(double magnitude90, double magnitude5, double exponent5) {
        this.magnitude90 = magnitude90;
        this.magnitude5 = magnitude5;
        this.exponent5 = exponent5;
    }

    @Override
    public double calculate(AggregationCell cell, double resolution) {
        return calculate(cell.getX(), cell.getY(), cell.getSampleCount(), resolution);
    }

    double calculate(int cellX, int cellY, long sampleCount, double resolution) {
        if (resolution == 5.0) {
            if (sampleCount == 0L) {
                return Double.NaN;
            } else {
                return magnitude5 * (1.0 - pow(sampleCount / 77500.0, exponent5));
            }
        } else if (resolution == 90.0) {
            if (sampleCount == 0L) {
                return Double.NaN;
            } else {
                return magnitude90 / sqrt(sampleCount);
            }
        } else {
            return 0.0;
        }
    }
}
