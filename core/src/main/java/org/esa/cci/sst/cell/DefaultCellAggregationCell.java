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

package org.esa.cci.sst.cell;/*
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

import org.esa.cci.sst.accumulate.ArithmeticMeanAccumulator;
import org.esa.cci.sst.accumulate.NumberAccumulator;
import org.esa.cci.sst.accumulate.WeightedUncertaintyAccumulator;
import org.esa.cci.sst.aggregate.AggregationCell;
import org.esa.cci.sst.aggregate.AggregationContext;

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
        // TODO - create accumulators depending on grids set in context
        sstAccumulator = new ArithmeticMeanAccumulator();
        sstAnomalyAccumulator = new ArithmeticMeanAccumulator();
        randomUncertaintyAccumulator = new WeightedUncertaintyAccumulator();
        coverageUncertaintyAccumulator = new WeightedUncertaintyAccumulator(); // TODO - check
        largeScaleUncertaintyAccumulator = new ArithmeticMeanAccumulator();  // TODO - check
        adjustmentUncertaintyAccumulator = new WeightedUncertaintyAccumulator(); // TODO - check
        synopticUncertaintyAccumulator = new WeightedUncertaintyAccumulator(); // TODO - check
        seaIceFractionAccumulator = new ArithmeticMeanAccumulator();
    }

    @Override
    public final long getSampleCount() {
        return sstAccumulator.getSampleCount();
    }

    @Override
    public final void accumulate(AggregationCell cell, double weight) {
        sstAccumulator.accumulate(cell.getSeaSurfaceTemperature(), weight);
        sstAnomalyAccumulator.accumulate(cell.getSeaSurfaceTemperatureAnomaly(), weight);
        randomUncertaintyAccumulator.accumulate(cell.getRandomUncertainty(), weight);
        coverageUncertaintyAccumulator.accumulate(cell.getCoverageUncertainty(), weight);
        largeScaleUncertaintyAccumulator.accumulate(cell.getLargeScaleUncertainty(), weight);
        synopticUncertaintyAccumulator.accumulate(cell.getSynopticUncertainty(), weight);
        adjustmentUncertaintyAccumulator.accumulate(cell.getAdjustmentUncertainty(), weight);
        seaIceFractionAccumulator.accumulate(cell.getSeaIceFraction());
    }

    @Override
    public final double getSeaSurfaceTemperature() {
        return sstAccumulator.combine();
    }

    @Override
    public final double getSeaSurfaceTemperatureAnomaly() {
        return sstAnomalyAccumulator.combine();
    }

    @Override
    public final double getRandomUncertainty() {
        return randomUncertaintyAccumulator.combine();
    }

    @Override
    public final double getLargeScaleUncertainty() {
        return largeScaleUncertaintyAccumulator.combine();
    }

    @Override
    public double getCoverageUncertainty() {
        return coverageUncertaintyAccumulator.combine();
    }

    @Override
    public final double getAdjustmentUncertainty() {
        return adjustmentUncertaintyAccumulator.combine();
    }

    @Override
    public final double getSynopticUncertainty() {
        return synopticUncertaintyAccumulator.combine();
    }

    @Override
    public final double getSeaIceFraction() {
        return seaIceFractionAccumulator.combine();
    }
}
