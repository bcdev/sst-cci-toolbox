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
import org.esa.cci.sst.common.calculator.WeightedUncertaintyAccumulator;
import org.esa.cci.sst.common.cell.AbstractAggregationCell;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.Grid;

import java.awt.Rectangle;

public class SingleDayAggregationCell extends AbstractAggregationCell implements SpatialAggregationCell {

    private final NumberAccumulator sstAccumulator;
    private final NumberAccumulator randomUncertaintyAccumulator;
    private final NumberAccumulator largeScaleUncertaintyAccumulator;
    private final NumberAccumulator adjustmentUncertaintyAccumulator;
    private final NumberAccumulator synopticUncertaintyAccumulator;
    private final NumberAccumulator seaIceFractionAccumulator;

    SingleDayAggregationCell(AggregationContext aggregationContext, int x, int y) {
        super(aggregationContext, x, y);

        sstAccumulator = new ArithmeticMeanAccumulator();
        randomUncertaintyAccumulator = new WeightedUncertaintyAccumulator();

        final Grid largeScaleUncertaintyGrid = aggregationContext.getLargeScaleUncertaintyGrid();
        if (largeScaleUncertaintyGrid != null) {
            largeScaleUncertaintyAccumulator = new ArithmeticMeanAccumulator();
        } else {
            largeScaleUncertaintyAccumulator = null;
        }
        final Grid adjustmentUncertaintyGrid = aggregationContext.getAdjustmentUncertaintyGrid();
        if (adjustmentUncertaintyGrid != null) {
            adjustmentUncertaintyAccumulator = new WeightedUncertaintyAccumulator();
        } else {
            adjustmentUncertaintyAccumulator = null;
        }
        final Grid synopticUncertaintyGrid = aggregationContext.getSynopticUncertaintyGrid();
        if (synopticUncertaintyGrid != null) {
            synopticUncertaintyAccumulator = new WeightedUncertaintyAccumulator();
        } else {
            synopticUncertaintyAccumulator = null;
        }
        final Grid seaIceFractionGrid = aggregationContext.getSeaIceFractionGrid();
        if (seaIceFractionGrid != null) {
            seaIceFractionAccumulator = new ArithmeticMeanAccumulator();
        } else {
            seaIceFractionAccumulator = null;
        }
    }

    @Override
    public final long getSampleCount() {
        return sstAccumulator.getSampleCount();
    }

    @Override
    public final void accumulate(AggregationContext aggregationContext, Rectangle rectangle) {
        final Grid sstGrid = aggregationContext.getSstGrid();
        final Grid qualityGrid = aggregationContext.getQualityGrid();
        final Grid randomUncertaintyGrid = aggregationContext.getRandomUncertaintyGrid();
        final Grid largeScaleUncertaintyGrid = aggregationContext.getLargeScaleUncertaintyGrid();
        final Grid adjustmentUncertaintyGrid = aggregationContext.getAdjustmentUncertaintyGrid();
        final Grid synopticUncertaintyGrid = aggregationContext.getSynopticUncertaintyGrid();

        final Grid seaCoverageGrid = aggregationContext.getSeaCoverageGrid();
        final Grid seaIceFractionGrid = aggregationContext.getSeaIceFractionGrid();

        final int minX = rectangle.x;
        final int minY = rectangle.y;
        final int maxX = minX + rectangle.width - 1;
        final int maxY = minY + rectangle.height - 1;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                final double seaCoverage = seaCoverageGrid.getSampleDouble(x, y);
                final double sst = sstGrid.getSampleDouble(x, y);
                if (isValid(x, y, seaCoverage, sst, qualityGrid)) {
                    final double randomUncertainty = randomUncertaintyGrid.getSampleDouble(x, y);

                    sstAccumulator.accumulate(sst, seaCoverage);
                    randomUncertaintyAccumulator.accumulate(randomUncertainty, seaCoverage);

                    if (largeScaleUncertaintyAccumulator != null) {
                        final double sample = largeScaleUncertaintyGrid.getSampleDouble(x, y);
                        largeScaleUncertaintyAccumulator.accumulate(sample, seaCoverage);
                    }
                    if (adjustmentUncertaintyAccumulator != null) {
                        final double sample = adjustmentUncertaintyGrid.getSampleDouble(x, y);
                        adjustmentUncertaintyAccumulator.accumulate(sample, seaCoverage);
                    }
                    if (synopticUncertaintyAccumulator != null) {
                        final double sample = synopticUncertaintyGrid.getSampleDouble(x, y);
                        synopticUncertaintyAccumulator.accumulate(sample, seaCoverage);
                    }
                }
                if (seaIceFractionAccumulator != null) {
                    final double sample = seaIceFractionGrid.getSampleDouble(x, y);
                    seaIceFractionAccumulator.accumulate(sample);
                }
            }
        }
    }

    @Override
    public final double getSeaSurfaceTemperature() {
        return sstAccumulator.combine();
    }

    @Override
    public final double getSeaSurfaceTemperatureAnomaly() {
        return Double.NaN;
    }

    @Override
    public final double getRandomUncertainty() {
        return randomUncertaintyAccumulator.combine();
    }

    @Override
    public final double getLargeScaleUncertainty() {
        if (largeScaleUncertaintyAccumulator != null) {
            return largeScaleUncertaintyAccumulator.combine();
        }
        return Double.NaN;
    }

    @Override
    public double getCoverageUncertainty() {
        return Double.NaN;
    }

    @Override
    public final double getAdjustmentUncertainty() {
        if (adjustmentUncertaintyAccumulator != null) {
            return adjustmentUncertaintyAccumulator.combine();
        }
        return Double.NaN;
    }

    @Override
    public final double getSynopticUncertainty() {
        if (synopticUncertaintyAccumulator != null) {
            return synopticUncertaintyAccumulator.combine();
        }
        return Double.NaN;
    }

    @Override
    public final double getSeaIceFraction() {
        if (seaIceFractionAccumulator != null) {
            return seaIceFractionAccumulator.combine();
        }
        return Double.NaN;
    }

    private boolean isValid(int x, int y, double seaCoverage, double sst, Grid qualityGrid) {
        return seaCoverage > 0.0 && sst > 0.0 && (qualityGrid == null || qualityGrid.getSampleInt(x, y) == 5);
    }
}
