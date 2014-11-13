package org.esa.cci.sst.common;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.cci.sst.aggregate.Aggregation;

public abstract class AbstractAggregation implements Aggregation {

    @Override
    public final Number[] getResults() {
        return new Double[]{
                getSeaSurfaceTemperature(),
                getSeaSurfaceTemperatureAnomaly(),
                getRandomUncertainty(),
                getCoverageUncertainty(),
                getLargeScaleUncertainty(),
                getAdjustmentUncertainty(),
                getSynopticUncertainty(),
                getSeaIceFraction()
        };
    }

    @Override
    public double getAdjustmentUncertainty() {
        return Double.NaN;
    }

    @Override
    public double getCoverageUncertainty() {
        return Double.NaN;
    }

    @Override
    public double getLargeScaleUncertainty() {
        return Double.NaN;
    }

    @Override
    public double getSeaIceFraction() {
        return Double.NaN;
    }

    @Override
    public double getSynopticUncertainty() {
        return Double.NaN;
    }
}
