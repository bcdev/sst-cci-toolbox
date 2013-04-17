package org.esa.cci.sst.common.file;/*
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
import org.esa.cci.sst.common.cell.AbstractAggregationCell;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellAggregationCell;

class DefaultCellAggregationCell extends AbstractAggregationCell implements CellAggregationCell<AggregationCell> {

    private final NumberAccumulator sstAccumulator;
    private final NumberAccumulator sstAnomalyAccumulator;
    private final NumberAccumulator randomUncertaintyAccumulator;
    private final NumberAccumulator coverageUncertaintyAccumulator;
    private final NumberAccumulator largeScaleUncertaintyAccumulator;
    private final NumberAccumulator adjustmentUncertaintyAccumulator;
    private final NumberAccumulator synopticUncertaintyAccumulator;
    private final NumberAccumulator seaIceFractionAccumulator;

    DefaultCellAggregationCell(AggregationContext aggregationContext, int x, int y) {
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
    public void accumulate(AggregationCell cell, double weight) {
        sstAccumulator.accumulate(cell.getSeaSurfaceTemperature());
        sstAnomalyAccumulator.accumulate(cell.getSeaSurfaceTemperatureAnomaly());
        randomUncertaintyAccumulator.accumulate(cell.getRandomUncertainty());
        coverageUncertaintyAccumulator.accumulate(cell.getCoverageUncertainty());
        largeScaleUncertaintyAccumulator.accumulate(cell.getLargeScaleUncertainty());
        synopticUncertaintyAccumulator.accumulate(cell.getSynopticUncertainty());
        adjustmentUncertaintyAccumulator.accumulate(cell.getAdjustmentUncertainty());
        seaIceFractionAccumulator.accumulate(cell.getSeaIceFraction());
    }

    @Override
    public double getSeaSurfaceTemperature() {
        return sstAccumulator.combine();
    }

    @Override
    public double getSeaSurfaceTemperatureAnomaly() {
        return sstAnomalyAccumulator.combine();
    }

    @Override
    public double getRandomUncertainty() {
        return randomUncertaintyAccumulator.combine();
    }

    @Override
    public double getLargeScaleUncertainty() {
        return largeScaleUncertaintyAccumulator.combine();
    }

    @Override
    public double getCoverageUncertainty() {
        return coverageUncertaintyAccumulator.combine();
    }

    @Override
    public double getAdjustmentUncertainty() {
        return adjustmentUncertaintyAccumulator.combine();
    }

    @Override
    public double getSynopticUncertainty() {
        return synopticUncertaintyAccumulator.combine();
    }

    @Override
    public double getSeaIceFraction() {
        return seaIceFractionAccumulator.combine();
    }
}
