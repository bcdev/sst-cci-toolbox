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

package org.esa.cci.sst.common;

/**
 * An object that represents an aggregation of some accumulated source "samples" and returns the aggregation results
 * as a vector of numbers.
 *
 * @author Norman Fomferra
 */
public interface Aggregation {

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
