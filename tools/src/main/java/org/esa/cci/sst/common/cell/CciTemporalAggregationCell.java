package org.esa.cci.sst.common.cell;/*
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

import org.esa.cci.sst.common.AggregationContext;
import org.esa.cci.sst.common.calculator.ArithmeticMeanAccumulator;
import org.esa.cci.sst.common.calculator.NumberAccumulator;
import org.esa.cci.sst.common.calculator.RandomUncertaintyAccumulator;

public class CciTemporalAggregationCell extends AbstractAggregationCell implements CellAggregationCell<CciSpatialAggregationCell> {

    private final NumberAccumulator sstAccumulator;
    private final NumberAccumulator sstAnomalyAccumulator;
    private final NumberAccumulator randomUncertaintyAccumulator;
    private final NumberAccumulator coverageUncertaintyAccumulator;
    private final NumberAccumulator largeScaleUncertaintyAccumulator;
    private final NumberAccumulator adjustmentUncertaintyAccumulator;
    private final NumberAccumulator synopticUncertaintyAccumulator;
    private final NumberAccumulator seaIceFractionAccumulator;

    public CciTemporalAggregationCell(AggregationContext aggregationContext, int x, int y) {
        super(aggregationContext, x, y);

        sstAccumulator = new ArithmeticMeanAccumulator();
        sstAnomalyAccumulator = new ArithmeticMeanAccumulator();
        randomUncertaintyAccumulator = new RandomUncertaintyAccumulator();
        coverageUncertaintyAccumulator = new RandomUncertaintyAccumulator(); // TODO - check
        largeScaleUncertaintyAccumulator = new ArithmeticMeanAccumulator();  // TODO - check
        adjustmentUncertaintyAccumulator = new RandomUncertaintyAccumulator(); // TODO - check
        synopticUncertaintyAccumulator = new RandomUncertaintyAccumulator(); // TODO - check
        seaIceFractionAccumulator = new ArithmeticMeanAccumulator();
    }

    @Override
    public final long getSampleCount() {
        return sstAccumulator.getSampleCount();
    }

    @Override
    public final Double[] getResults() {
        return new Double[]{
                getSeaSurfaceTemperature(),
                getSeaSurfaceTemperatureAnomaly(),
                getRandomUncertainty(),
                getLargeScaleUncertainty(),
                getCoverageUncertainty(),
                getAdjustmentUncertainty(),
                getSynopticUncertainty(),
                getSeaIceFraction()
        };
    }

    @Override
    public void accumulate(CciSpatialAggregationCell cell, double weight) {
        sstAccumulator.accumulate(cell.getSeaSurfaceTemperature());
        sstAnomalyAccumulator.accumulate(cell.getSeaSurfaceTemperatureAnomaly());
        randomUncertaintyAccumulator.accumulate(cell.getRandomUncertainty());
        coverageUncertaintyAccumulator.accumulate(cell.getCoverageUncertainty());
        largeScaleUncertaintyAccumulator.accumulate(cell.getLargeScaleUncertainty());
        synopticUncertaintyAccumulator.accumulate(cell.getSynopticUncertainty());
        adjustmentUncertaintyAccumulator.accumulate(cell.getAdjustmentUncertainty());
        seaIceFractionAccumulator.accumulate(cell.getSeaIceFraction());
    }

    public final double getSeaSurfaceTemperature() {
        return sstAccumulator.combine();
    }

    public final double getSeaSurfaceTemperatureAnomaly() {
        return sstAnomalyAccumulator.combine();
    }

    public final double getRandomUncertainty() {
        return randomUncertaintyAccumulator.combine();
    }

    public double getLargeScaleUncertainty() {
        return largeScaleUncertaintyAccumulator.combine();
    }

    public final double getCoverageUncertainty() {
        return coverageUncertaintyAccumulator.combine();
    }

    public double getAdjustmentUncertainty() {
        return adjustmentUncertaintyAccumulator.combine();
    }

    public double getSynopticUncertainty() {
        return synopticUncertaintyAccumulator.combine();
    }

    public double getSeaIceFraction() {
        return seaIceFractionAccumulator.combine();
    }
}
