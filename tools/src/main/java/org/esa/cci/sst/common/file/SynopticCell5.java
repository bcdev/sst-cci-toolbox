package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.AggregationContext;
import org.esa.cci.sst.common.calculator.ArithmeticMeanAccumulator;
import org.esa.cci.sst.common.calculator.NumberAccumulator;
import org.esa.cci.sst.common.calculator.SynopticUncertaintyProvider;
import org.esa.cci.sst.common.calculator.WeightedUncertaintyAccumulator;
import org.esa.cci.sst.common.cell.AbstractAggregationCell;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.Grid;

import java.awt.Rectangle;

final class SynopticCell5 extends AbstractAggregationCell implements SpatialAggregationCell {

    private final NumberAccumulator sstAccumulator;
    private final NumberAccumulator sstAnomalyAccumulator;
    private final NumberAccumulator randomUncertaintyAccumulator;
    private final NumberAccumulator largeScaleUncertaintyAccumulator;
    private final NumberAccumulator adjustmentUncertaintyAccumulator5;
    private final NumberAccumulator synopticUncertaintyAccumulator5;
    private final NumberAccumulator seaIceFractionAccumulator;

    SynopticCell5(AggregationContext aggregationContext, int x, int y) {
        super(aggregationContext, x, y);

        sstAccumulator = new ArithmeticMeanAccumulator();
        sstAnomalyAccumulator = new ArithmeticMeanAccumulator();
        randomUncertaintyAccumulator = new WeightedUncertaintyAccumulator();
        largeScaleUncertaintyAccumulator = new ArithmeticMeanAccumulator();
        if (aggregationContext.getAdjustmentUncertaintyGrid() != null) {
            adjustmentUncertaintyAccumulator5 = new WeightedUncertaintyAccumulator();
        } else {
            adjustmentUncertaintyAccumulator5 = null;
        }
        if (aggregationContext.getSynopticUncertaintyGrid() != null) {
            synopticUncertaintyAccumulator5 = new WeightedUncertaintyAccumulator();
        } else {
            synopticUncertaintyAccumulator5 = null;
        }
        if (aggregationContext.getSeaIceFractionGrid() != null) {
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
    public void accumulate(AggregationContext aggregationContext, Rectangle rectangle) {
        final Grid sstGrid = aggregationContext.getSstGrid();
        final Grid qualityGrid = aggregationContext.getQualityGrid();
        final Grid randomUncertaintyGrid = aggregationContext.getRandomUncertaintyGrid();
        final Grid largeScaleUncertaintyGrid = aggregationContext.getLargeScaleUncertaintyGrid();
        final Grid adjustmentUncertaintyGrid = aggregationContext.getAdjustmentUncertaintyGrid();
        final Grid synopticUncertaintyGrid = aggregationContext.getSynopticUncertaintyGrid();

        final Grid climatologySstGrid = aggregationContext.getClimatologySstGrid();
        final Grid seaCoverageGrid = aggregationContext.getSeaCoverageGrid();
        final Grid seaIceFractionGrid = aggregationContext.getSeaIceFractionGrid();

        final int minX = rectangle.x;
        final int minY = rectangle.y;

        for (int y1 = 0; y1 < 5; y1++) { // 1° loop
            for (int x1 = 0; x1 < 5; x1++) { // 1° loop
                final NumberAccumulator adjustmentUncertaintyAccumulator1 = new ArithmeticMeanAccumulator();
                final NumberAccumulator synopticUncertaintyAccumulator1 = new ArithmeticMeanAccumulator();

                for (int y0 = 0; y0 < 20; y0++) { // 0.05° loop
                    for (int x0 = 0; x0 < 20; x0++) { // 0.05° loop
                        final int y = minY + 5 * y1 + y0;
                        final int x = minX + 5 * x1 + x0;

                        final double seaCoverage = seaCoverageGrid.getSampleDouble(x, y);
                        final double sst = sstGrid.getSampleDouble(x, y);

                        if (isValid(x, y, seaCoverage, sst, qualityGrid)) {
                            final double climatologySst = climatologySstGrid.getSampleDouble(x, y);
                            final double randomUncertainty = randomUncertaintyGrid.getSampleDouble(x, y);

                            sstAccumulator.accumulate(sst, seaCoverage);
                            sstAnomalyAccumulator.accumulate(sst - climatologySst, seaCoverage);
                            randomUncertaintyAccumulator.accumulate(randomUncertainty, seaCoverage);
                            if (largeScaleUncertaintyAccumulator != null) {
                                final double sample = largeScaleUncertaintyGrid.getSampleDouble(x, y);
                                largeScaleUncertaintyAccumulator.accumulate(sample, seaCoverage);
                            }
                            if (adjustmentUncertaintyAccumulator5 != null) {
                                final double sample = adjustmentUncertaintyGrid.getSampleDouble(x, y);
                                adjustmentUncertaintyAccumulator1.accumulate(sample);
                            }
                            if (synopticUncertaintyAccumulator5 != null) {
                                final double sample = synopticUncertaintyGrid.getSampleDouble(x, y);
                                synopticUncertaintyAccumulator1.accumulate(sample);
                            }
                        }
                        if (seaIceFractionAccumulator != null) {
                            final double sample = seaIceFractionGrid.getSampleDouble(x, y);
                            seaIceFractionAccumulator.accumulate(sample);
                        }
                    }
                }
                if (adjustmentUncertaintyAccumulator5 != null) {
                    adjustmentUncertaintyAccumulator5.accumulate(adjustmentUncertaintyAccumulator1.combine(), 0.04);
                }
                if (synopticUncertaintyAccumulator5 != null) {
                    synopticUncertaintyAccumulator5.accumulate(synopticUncertaintyAccumulator1.combine(), 0.04);
                }
            }
        }
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
        return getAggregationContext().getCoverageUncertaintyProvider().calculate(this, 5.0);
    }

    @Override
    public double getAdjustmentUncertainty() {
        if (adjustmentUncertaintyAccumulator5 != null) {
            final double result = adjustmentUncertaintyAccumulator5.combine();
            final SynopticUncertaintyProvider provider = getAggregationContext().getSynopticUncertaintyProvider();
            if (provider == null) {
                throw new IllegalStateException(
                        "Cannot compute adjustment uncertainty because 'synopticUncertaintyProvider' has not been set in 'aggregationContext'.");
            }
            return provider.calculate(this, result);
        }
        return Double.NaN;
    }

    @Override
    public double getSynopticUncertainty() {
        if (synopticUncertaintyAccumulator5 != null) {
            final double result = synopticUncertaintyAccumulator5.combine();
            final SynopticUncertaintyProvider provider = getAggregationContext().getSynopticUncertaintyProvider();
            if (provider == null) {
                throw new IllegalStateException(
                        "Cannot compute synoptic uncertainty because 'synopticUncertaintyProvider' has not been set in 'aggregationContext'.");
            }
            return provider.calculate(this, result);
        }
        return Double.NaN;
    }

    @Override
    public double getSeaIceFraction() {
        if (seaIceFractionAccumulator != null) {
            return seaIceFractionAccumulator.combine();
        }
        return Double.NaN;
    }

    private boolean isValid(int x, int y, double seaCoverage, double sst, Grid qualityGrid) {
        return seaCoverage > 0.0 && sst > 0.0 && (qualityGrid == null || qualityGrid.getSampleInt(x, y) == 5);
    }
}
