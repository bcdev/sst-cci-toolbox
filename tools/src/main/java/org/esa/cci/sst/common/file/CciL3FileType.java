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

package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.AbstractAggregation;
import org.esa.cci.sst.common.Aggregation;
import org.esa.cci.sst.common.AggregationContext;
import org.esa.cci.sst.common.AggregationFactory;
import org.esa.cci.sst.common.RegionalAggregation;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.calculator.ArithmeticMeanAccumulator;
import org.esa.cci.sst.common.calculator.NumberAccumulator;
import org.esa.cci.sst.common.calculator.RandomUncertaintyAccumulator;
import org.esa.cci.sst.common.cell.AbstractAggregationCell;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellAggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.regavg.MultiMonthAggregation;
import org.esa.cci.sst.regavg.SameMonthAggregation;
import org.esa.cci.sst.util.NcUtils;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.awt.Rectangle;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Norman Fomferra
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public class CciL3FileType extends AbstractCciFileType {

    public final static CciL3FileType INSTANCE = new CciL3FileType();
    public static final String OUT_VAR_TOTAL_UNCERTAINTY = "total_uncertainty";

    @Override
    public AggregationContext readSourceGrids(NetcdfFile dataFile, SstDepth sstDepth, AggregationContext context) throws
                                                                                                                  IOException {
        final GridDef gridDef = getGridDef();

        switch (sstDepth) {
            case depth_20:
            case depth_100:
                context.setSstGrid(NcUtils.readGrid(dataFile, "sea_surface_temperature_depth", gridDef, 0));
                break;
            case skin:
                context.setSstGrid(NcUtils.readGrid(dataFile, "sea_surface_temperature", gridDef, 0));
                break;
            default:
                throw new IllegalArgumentException(MessageFormat.format("sstDepth = {0}", sstDepth));
        }
        context.setQualityGrid(NcUtils.readGrid(dataFile, "quality_level", gridDef, 0));
        context.setRandomUncertaintyGrid(NcUtils.readGrid(dataFile, "uncorrelated_uncertainty", gridDef, 0));
        context.setLargeScaleUncertaintyGrid(
                NcUtils.readGrid(dataFile, "large_scale_correlated_uncertainty", gridDef, 0));
        context.setSynopticUncertaintyGrid(
                NcUtils.readGrid(dataFile, "synoptically_correlated_uncertainty", gridDef, 0));

        if (NcUtils.hasVariable(dataFile, "adjustment_uncertainty")) {
            context.setAdjustmentUncertaintyGrid(NcUtils.readGrid(dataFile, "adjustment_uncertainty", gridDef, 0));
        }
        return context;
    }

    @Override
    public Variable[] createOutputVariables(NetcdfFileWriteable file, SstDepth sstDepth, boolean totalUncertainty,
                                            Dimension[] dims) {
        Variable[] variables = new Variable[9];

        Variable sstVar = file.addVariable(String.format("sst_%s", sstDepth), DataType.FLOAT, dims);
        sstVar.addAttribute(new Attribute("units", "kelvin"));
        sstVar.addAttribute(new Attribute("long_name", String.format("mean of sst %s in kelvin", sstDepth)));
        sstVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        Variable sstAnomalyVar = file.addVariable(String.format("sst_%s_anomaly", sstDepth), DataType.FLOAT, dims);
        sstAnomalyVar.addAttribute(new Attribute("units", "kelvin"));
        sstAnomalyVar.addAttribute(
                new Attribute("long_name", String.format("mean of sst %s anomaly in kelvin", sstDepth)));
        sstAnomalyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        if (totalUncertainty) {
            Variable totalUncertaintyVar = file.addVariable(OUT_VAR_TOTAL_UNCERTAINTY, DataType.FLOAT, dims);
            totalUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
            totalUncertaintyVar.addAttribute(new Attribute("long_name", "the total uncertainty in kelvin"));
            totalUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

            variables[Aggregation.SST] = sstVar;
            variables[Aggregation.SST_ANOMALY] = sstAnomalyVar;
            variables[8] = totalUncertaintyVar; // TODO - do this in Writer
        } else {
            Variable coverageUncertaintyVar = file.addVariable("coverage_uncertainty", DataType.FLOAT, dims);
            coverageUncertaintyVar.addAttribute(new Attribute("units", "1"));
            coverageUncertaintyVar.addAttribute(new Attribute("long_name", "mean of sampling/coverage uncertainty"));
            coverageUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

            Variable uncorrelatedUncertaintyVar = file.addVariable("uncorrelated_uncertainty", DataType.FLOAT, dims);
            uncorrelatedUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
            uncorrelatedUncertaintyVar.addAttribute(
                    new Attribute("long_name", "mean of uncorrelated uncertainty in kelvin"));
            uncorrelatedUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

            Variable largeScaleCorrelatedUncertaintyVar = file.addVariable("large_scale_correlated_uncertainty",
                                                                           DataType.FLOAT, dims);
            largeScaleCorrelatedUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
            largeScaleCorrelatedUncertaintyVar.addAttribute(
                    new Attribute("long_name", "mean of large scale correlated uncertainty in kelvin"));
            largeScaleCorrelatedUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

            Variable synopticallyCorrelatedUncertaintyVar = file.addVariable("synoptically_correlated_uncertainty",
                                                                             DataType.FLOAT, dims);
            synopticallyCorrelatedUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
            synopticallyCorrelatedUncertaintyVar.addAttribute(
                    new Attribute("long_name", "mean of synoptically correlated uncertainty in kelvin"));
            synopticallyCorrelatedUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

            Variable adjustmentUncertaintyVar = file.addVariable("adjustment_uncertainty", DataType.FLOAT, dims);
            adjustmentUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
            adjustmentUncertaintyVar.addAttribute(
                    new Attribute("long_name", "mean of adjustment uncertainty in kelvin"));
            adjustmentUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

            variables[Aggregation.SST] = sstVar;
            variables[Aggregation.SST_ANOMALY] = sstAnomalyVar;
            variables[Aggregation.RANDOM_UNCERTAINTY] = uncorrelatedUncertaintyVar;
            variables[Aggregation.COVERAGE_UNCERTAINTY] = coverageUncertaintyVar;
            variables[Aggregation.LARGE_SCALE_UNCERTAINTY] = largeScaleCorrelatedUncertaintyVar;
            variables[Aggregation.SYNOPTIC_UNCERTAINTY] = synopticallyCorrelatedUncertaintyVar;
            variables[Aggregation.ADJUSTMENT_UNCERTAINTY] = adjustmentUncertaintyVar;
        }
        return variables;
    }

    @Override
    public AggregationFactory<SameMonthAggregation<AggregationCell>> getSameMonthAggregationFactory() {
        return new AggregationFactory<SameMonthAggregation<AggregationCell>>() {
            @Override
            public SameMonthAggregation<AggregationCell> createAggregation() {
                return new MultiPurposeAggregation();
            }
        };
    }

    @Override
    public AggregationFactory<MultiMonthAggregation<RegionalAggregation>> getMultiMonthAggregationFactory() {
        return new AggregationFactory<MultiMonthAggregation<RegionalAggregation>>() {
            @Override
            public MultiMonthAggregation<RegionalAggregation> createAggregation() {
                return new MultiPurposeAggregation();
            }
        };
    }

    @Override
    public CellFactory<SpatialAggregationCell> getCellFactory5(final AggregationContext context) {
        return new CellFactory<SpatialAggregationCell>() {
            @Override
            public Cell5 createCell(int cellX, int cellY) {
                return new Cell5(context, cellX, cellY);
            }
        };
    }

    @Override
    public CellFactory<CellAggregationCell<AggregationCell>> getCellFactory90(final AggregationContext context) {
        return new CellFactory<CellAggregationCell<AggregationCell>>() {
            @Override
            public Cell90 createCell(int cellX, int cellY) {
                return new Cell90(context, cellX, cellY);
            }
        };
    }

    @Override
    public String getFilenameRegex() {
//        return "\\d{14}-" + getRdac() + "-" + "L3[CU]{1}" + "_GHRSST-SST[a-z]{3,7}-[A-Z1-2_]{3,10}-[DMLT]{2}-v\\d{1,2}\\.\\d{1}-fv\\d{1,2}\\.\\d{1}.nc";
        return "\\d{14}-" + getRdac() + "-L3[CU]{1}_GHRSST-SST((skin)|(subskin)|(depth)|(fnd))[-]" +
               "((ATSR1)|(ATSR2)|(AATSR)|(AMSRE)|(SEVIRI_SST)|(TMI))[-]((LT)|(DM))-" +
               "v\\d{1,2}\\.\\d{1}-fv\\d{1,2}\\.\\d{1}.nc";
    }

    static class Cell5 extends AbstractAggregationCell implements SpatialAggregationCell {

        private final NumberAccumulator sstAccumulator;
        private final NumberAccumulator sstAnomalyAccumulator;
        private final NumberAccumulator randomUncertaintyAccumulator;
        private final NumberAccumulator largeScaleUncertaintyAccumulator;
        private final NumberAccumulator adjustmentUncertaintyAccumulator5;
        private final NumberAccumulator synopticUncertaintyAccumulator5;
        private final NumberAccumulator seaIceFractionAccumulator;

        Cell5(AggregationContext aggregationContext, int x, int y) {
            super(aggregationContext, x, y);

            sstAccumulator = new ArithmeticMeanAccumulator();
            sstAnomalyAccumulator = new ArithmeticMeanAccumulator();
            randomUncertaintyAccumulator = new RandomUncertaintyAccumulator();
            largeScaleUncertaintyAccumulator = new ArithmeticMeanAccumulator();
            if (aggregationContext.getAdjustmentUncertaintyGrid() != null) {
                adjustmentUncertaintyAccumulator5 = new RandomUncertaintyAccumulator();
            } else {
                adjustmentUncertaintyAccumulator5 = null;
            }
            if (aggregationContext.getSynopticUncertaintyGrid() != null) {
                synopticUncertaintyAccumulator5 = new RandomUncertaintyAccumulator();
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
                return getAggregationContext().getSynopticUncertaintyProvider().calculate(this, result);
            }
            return Double.NaN;
        }

        @Override
        public double getSynopticUncertainty() {
            if (synopticUncertaintyAccumulator5 != null) {
                final double result = synopticUncertaintyAccumulator5.combine();
                return getAggregationContext().getSynopticUncertaintyProvider().calculate(this, result);
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

    private static final class Cell90 extends DefaultCellAggregationCell {

        private Cell90(AggregationContext context, int cellX, int cellY) {
            super(context, cellX, cellY);
        }

        @Override
        public double getCoverageUncertainty() {
            final double uncertainty5 = super.getCoverageUncertainty();
            final double uncertainty90 = getAggregationContext().getCoverageUncertaintyProvider().calculate(this, 90.0);
            return Math.sqrt(uncertainty5 * uncertainty5 + uncertainty90 * uncertainty90);
        }
    }

    private static final class MultiPurposeAggregation extends AbstractAggregation implements RegionalAggregation,
                                                                                              SameMonthAggregation<AggregationCell>,
                                                                                              MultiMonthAggregation<RegionalAggregation> {

        private final NumberAccumulator sstAccumulator = new ArithmeticMeanAccumulator();
        private final NumberAccumulator sstAnomalyAccumulator = new ArithmeticMeanAccumulator();
        private final NumberAccumulator coverageUncertaintyAccumulator = new RandomUncertaintyAccumulator();
        private final NumberAccumulator randomUncertaintyAccumulator = new RandomUncertaintyAccumulator();
        private final NumberAccumulator largeScaleUncertaintyAccumulator = new ArithmeticMeanAccumulator();
        private final NumberAccumulator synopticUncertaintyAccumulator = new RandomUncertaintyAccumulator();
        private final NumberAccumulator adjustmentUncertaintyAccumulator = new RandomUncertaintyAccumulator();

        @Override
        public long getSampleCount() {
            return sstAccumulator.getSampleCount();
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
            return Double.NaN;
        }

        @Override
        public void accumulate(AggregationCell cell, double seaCoverage) {
            sstAccumulator.accumulate(cell.getSeaSurfaceTemperature(), seaCoverage);
            sstAnomalyAccumulator.accumulate(cell.getSeaSurfaceTemperatureAnomaly(), seaCoverage);
            coverageUncertaintyAccumulator.accumulate(cell.getCoverageUncertainty(), seaCoverage);
            randomUncertaintyAccumulator.accumulate(cell.getRandomUncertainty(), seaCoverage);
            largeScaleUncertaintyAccumulator.accumulate(cell.getLargeScaleUncertainty(), seaCoverage);
            synopticUncertaintyAccumulator.accumulate(cell.getSynopticUncertainty(), seaCoverage);
            adjustmentUncertaintyAccumulator.accumulate(cell.getAdjustmentUncertainty(), seaCoverage);
        }

        @Override
        public void accumulate(RegionalAggregation aggregation) {
            sstAccumulator.accumulate(aggregation.getSeaSurfaceTemperature());
            sstAnomalyAccumulator.accumulate(aggregation.getSeaSurfaceTemperatureAnomaly());
            coverageUncertaintyAccumulator.accumulate(aggregation.getCoverageUncertainty());
            randomUncertaintyAccumulator.accumulate(aggregation.getRandomUncertainty());
            largeScaleUncertaintyAccumulator.accumulate(aggregation.getLargeScaleUncertainty());
            synopticUncertaintyAccumulator.accumulate(aggregation.getSynopticUncertainty());
            adjustmentUncertaintyAccumulator.accumulate(aggregation.getAdjustmentUncertainty());
        }
    }

    @Override
    public boolean hasSynopticUncertainties() {
        return true;
    }
}
