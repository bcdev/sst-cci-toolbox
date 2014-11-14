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

package org.esa.cci.sst.aggregate;

/**
 * An object that represents an aggregation of some accumulated source "samples" and returns the aggregation results
 * as a vector of numbers.
 *
 * @author Norman Fomferra
 */
public interface Aggregation {

    final int SST = 0;
    final int SST_ANOMALY = 1;
    final int RANDOM_UNCERTAINTY = 2;
    final int COVERAGE_UNCERTAINTY = 3;
    final int LARGE_SCALE_UNCERTAINTY = 4;
    final int ADJUSTMENT_UNCERTAINTY = 5;
    final int SYNOPTIC_UNCERTAINTY = 6;
    final int SEA_ICE_FRACTION = 7;

    long getSampleCount();

    Number[] getResults();

    public double getSeaSurfaceTemperature();

    public double getSeaSurfaceTemperatureAnomaly();

    public double getRandomUncertainty();

    public double getLargeScaleUncertainty();

    public double getCoverageUncertainty();

    public double getAdjustmentUncertainty();

    public double getSynopticUncertainty();

    public double getSeaIceFraction();
}
