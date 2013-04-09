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
import org.esa.cci.sst.common.calculator.SynopticUncertaintyAccumulator;
import org.esa.cci.sst.common.cellgrid.Grid;

import java.awt.Rectangle;

public class DefaultSpatialAggregationCell extends AbstractAggregationCell implements SpatialAggregationCell {

    private final NumberAccumulator sstAccumulator;
    private final NumberAccumulator sstAnomalyAccumulator;
    private final NumberAccumulator randomUncertaintyAccumulator;
    private final NumberAccumulator varianceAccumulator;
    private final NumberAccumulator largeScaleUncertaintyAccumulator;
    private final NumberAccumulator adjustmentUncertaintyAccumulator;
    private final NumberAccumulator synopticUncertaintyAccumulator;
    private final NumberAccumulator seaIceFractionAccumulator;

    private boolean enoughSamples;

    public DefaultSpatialAggregationCell(AggregationContext aggregationContext, int x, int y) {
        super(aggregationContext, x, y);

        sstAccumulator = new ArithmeticMeanAccumulator();
        sstAnomalyAccumulator = new ArithmeticMeanAccumulator();
        randomUncertaintyAccumulator = new RandomUncertaintyAccumulator();
        varianceAccumulator = new ArithmeticMeanAccumulator();
        largeScaleUncertaintyAccumulator = new ArithmeticMeanAccumulator();
        adjustmentUncertaintyAccumulator = new SynopticUncertaintyAccumulator();
        synopticUncertaintyAccumulator = new SynopticUncertaintyAccumulator();
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
    public void accumulate(AggregationContext aggregationContext, Rectangle rectangle) {
        final Grid sstGrid = aggregationContext.getSstGrid();
        final Grid qualityGrid = aggregationContext.getQualityGrid();
        final Grid randomUncertaintyGrid = aggregationContext.getRandomUncertaintyGrid();
        final Grid largeScaleUncertaintyGrid = aggregationContext.getLargeScaleUncertaintyGrid();
        final Grid adjustmentUncertaintyGrid = aggregationContext.getAdjustmentUncertaintyGrid();
        final Grid synopticUncertaintyGrid = aggregationContext.getSynopticUncertaintyGrid();

        final Grid climatologySstGrid = aggregationContext.getClimatologySstGrid();
        final Grid seaCoverageGrid = aggregationContext.getSeaCoverageGrid();
        final Grid standardDeviationGrid = aggregationContext.getStandardDeviationGrid();
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
                    final double climatologySst = climatologySstGrid.getSampleDouble(x, y);
                    final double randomUncertainty = randomUncertaintyGrid.getSampleDouble(x, y);
                    final double standardDeviation = standardDeviationGrid.getSampleDouble(x, y);

                    sstAccumulator.accumulate(sst, seaCoverage);
                    sstAnomalyAccumulator.accumulate(sst - climatologySst, seaCoverage);
                    randomUncertaintyAccumulator.accumulate(randomUncertainty, seaCoverage);
                    varianceAccumulator.accumulate(standardDeviation * standardDeviation, seaCoverage);

                    if (largeScaleUncertaintyGrid != null) {
                        final double largeScaleUncertainty = largeScaleUncertaintyGrid.getSampleDouble(x, y);
                        largeScaleUncertaintyAccumulator.accumulate(largeScaleUncertainty, seaCoverage);
                    }
                    if (adjustmentUncertaintyGrid != null) {
                        final double adjustmentUncertainty = adjustmentUncertaintyGrid.getSampleDouble(x, y);
                        adjustmentUncertaintyAccumulator.accumulate(adjustmentUncertainty);
                    }
                    if (synopticUncertaintyGrid != null) {
                        final double synopticUncertainty = synopticUncertaintyGrid.getSampleDouble(x, y);
                        synopticUncertaintyAccumulator.accumulate(synopticUncertainty);
                    }
                }
                if (seaIceFractionGrid != null) {
                    final double seaIceFraction = seaIceFractionGrid.getSampleDouble(x, y);
                    seaIceFractionAccumulator.accumulate(seaIceFraction);
                }
            }
        }
        final int maxSampleCount = rectangle.height * rectangle.width;
        enoughSamples = getSampleCount() > getAggregationContext().getMinCoverage() * maxSampleCount;
    }

    public final double getSeaSurfaceTemperature() {
        if (enoughSamples) {
            return sstAccumulator.combine();
        }
        return Double.NaN;
    }

    public final double getSeaSurfaceTemperatureAnomaly() {
        if (enoughSamples) {
            return sstAnomalyAccumulator.combine();
        }
        return Double.NaN;
    }

    public final double getRandomUncertainty() {
        if (enoughSamples) {
            return randomUncertaintyAccumulator.combine();
        }
        return Double.NaN;
    }

    public double getLargeScaleUncertainty() {
        if (enoughSamples) {
            return largeScaleUncertaintyAccumulator.combine();
        }
        return Double.NaN;
    }

    public final double getCoverageUncertainty() {
        if (enoughSamples) {
            final double result = varianceAccumulator.combine();
            return getAggregationContext().getCoverageUncertaintyProvider().calculate(this, result);
        }
        return Double.NaN;
    }

    public double getAdjustmentUncertainty() {
        if (enoughSamples) {
            final double result = adjustmentUncertaintyAccumulator.combine();
            return getAggregationContext().getSynopticUncertaintyProvider().calculate(this, result);
        }
        return Double.NaN;
    }

    public double getSynopticUncertainty() {
        if (enoughSamples) {
            final double result = synopticUncertaintyAccumulator.combine();
            return getAggregationContext().getSynopticUncertaintyProvider().calculate(this, result);
        }
        return Double.NaN;
    }

    public double getSeaIceFraction() {
        return seaIceFractionAccumulator.combine();
    }

    private boolean isValid(int x, int y, double seaCoverage, double sst, Grid qualityGrid) {
        return seaCoverage > 0.0 && sst > 0.0 && (qualityGrid == null || qualityGrid.getSampleInt(x, y) == 5);
    }
}
