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
import java.util.Arrays;

/**
 * @author Norman Fomferra
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public class CciL3FileType extends AbstractCciFileType {

    public final static CciL3FileType INSTANCE = new CciL3FileType();
    public static final String OUT_VAR_TOTAL_UNCERTAINTY = "total_uncertainty";

    @Override
    public AggregationContext readSourceGrids(NetcdfFile dataFile, SstDepth sstDepth, AggregationContext context) throws IOException {
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
        context.setLargeScaleUncertaintyGrid(NcUtils.readGrid(dataFile, "large_scale_correlated_uncertainty", gridDef, 0));
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
        Variable[] variables;

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

            variables = new Variable[]{
                    sstVar,
                    sstAnomalyVar,
                    totalUncertaintyVar
            };
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

            variables = new Variable[]{
                    sstVar,
                    sstAnomalyVar,
                    coverageUncertaintyVar,
                    uncorrelatedUncertaintyVar,
                    largeScaleCorrelatedUncertaintyVar,
                    synopticallyCorrelatedUncertaintyVar,
                    adjustmentUncertaintyVar
            };

        }
        return variables;
    }

    @Override
    public AggregationFactory<SameMonthAggregation> getSameMonthAggregationFactory() {
        return new AggregationFactory<SameMonthAggregation>() {
            @Override
            public SameMonthAggregation createAggregation() {
                return new L3USameMonthAggregation();
            }
        };
    }

    @Override
    public AggregationFactory<MultiMonthAggregation> getMultiMonthAggregationFactory() {
        return new AggregationFactory<MultiMonthAggregation>() {
            @Override
            public MultiMonthAggregation createAggregation() {
                return new L3UMultiMonthAggregation();
            }
        };
    }

    @Override
    public CellFactory<CellAggregationCell<AggregationCell>> getTemporalAggregationCellFactory() {
        return new CellFactory<CellAggregationCell<AggregationCell>>() {
            @Override
            public L3UTemporalCell createCell(int cellX, int cellY) {
                return new L3UTemporalCell(cellX, cellY);
            }
        };
    }

    @Override
    public CellFactory getCellFactory(final AggregationContext context, final CellTypes cellType) {
        switch (cellType) {
            case SYNOPTIC_CELL_1:
                return new CellFactory<L3USynopticAreaCell1>() {
                    @Override
                    public L3USynopticAreaCell1 createCell(int cellX, int cellY) {
                        return new L3USynopticAreaCell1(cellX, cellY);
                    }
                };
            case SYNOPTIC_CELL_5:
                return new CellFactory<L3USynopticCell5>() {
                    @Override
                    public L3USynopticCell5 createCell(int cellX, int cellY) {
                        return new L3USynopticCell5(cellX, cellY);
                    }
                };
            case CELL_90: {
                return new CellFactory<CellAggregationCell>() {
                    @Override
                    public L3UCell90 createCell(int cellX, int cellY) {
                        return new L3UCell90(context, cellX, cellY);
                    }
                };
            }
            case SPATIAL_CELL_5: {
                return new CellFactory<SpatialAggregationCell>() {
                    @Override
                    public L3UCell5 createCell(int cellX, int cellY) {
                        return new L3UCell5(context, cellX, cellY);
                    }
                };
            }
            default:
                throw new IllegalStateException("never come here.");
        }
    }

    @Override
    public String getFilenameRegex() {
//        return "\\d{14}-" + getRdac() + "-" + "L3[CU]{1}" + "_GHRSST-SST[a-z]{3,7}-[A-Z1-2_]{3,10}-[DMLT]{2}-v\\d{1,2}\\.\\d{1}-fv\\d{1,2}\\.\\d{1}.nc";
        return "\\d{14}-" + getRdac() + "-L3[CU]{1}_GHRSST-SST((skin)|(subskin)|(depth)|(fnd))[-]" +
               "((ATSR1)|(ATSR2)|(AATSR)|(AMSRE)|(SEVIRI_SST)|(TMI))[-]((LT)|(DM))-" +
               "v\\d{1,2}\\.\\d{1}-fv\\d{1,2}\\.\\d{1}.nc";
    }

    private static abstract class AbstractL3UCell extends AbstractAggregationCell {

        protected final NumberAccumulator sstAccu = new ArithmeticMeanAccumulator();
        protected final NumberAccumulator sstAnomalyAccu = new ArithmeticMeanAccumulator();
        protected final NumberAccumulator uncorrelatedUncertaintyAccu = new RandomUncertaintyAccumulator();
        protected final NumberAccumulator largeScaleCorrelatedUncertaintyAccu = new ArithmeticMeanAccumulator();

        private AbstractL3UCell(AggregationContext context, int x, int y) {
            super(context, x, y);
        }

        @Override
        public long getSampleCount() {
            return sstAnomalyAccu.getSampleCount();
        }

        public double computeSstAverage() {
            return sstAccu.combine();
        }

        public double computeSstAnomalyAverage() {
            return sstAnomalyAccu.combine();
        }

        public double computeUncorrelatedUncertaintyAverage() {
            return uncorrelatedUncertaintyAccu.combine();
        }

        public double computeLargeScaleCorrelatedUncertaintyAverage() {
            return largeScaleCorrelatedUncertaintyAccu.combine();
        }

        public abstract double computeCoverageUncertainty();

        @Override
        public Number[] getResults() {
            // Note: Result types must match those defined in FileType.createOutputVariables().
            return new Number[]{
                    (float) computeSstAverage(),
                    (float) computeSstAnomalyAverage(),
                    (float) computeCoverageUncertainty(),
                    (float) computeUncorrelatedUncertaintyAverage(),
                    (float) computeLargeScaleCorrelatedUncertaintyAverage()
            };
        }
    }

    private static class L3UCell5 extends AbstractL3UCell implements SpatialAggregationCell {

        private L3UCell5(AggregationContext coverageUncertaintyProvider, int x, int y) {
            super(coverageUncertaintyProvider, x, y);
        }

        @Override
        public double computeCoverageUncertainty() {
            return getAggregationContext().getCoverageUncertaintyProvider().calculate(this, 5.0);
        }

        @Override
        public void accumulate(AggregationContext aggregationContext, Rectangle rectangle) {
            final Grid sstGrid = aggregationContext.getSourceGrids()[0];
            final Grid qualityLevelGrid = aggregationContext.getSourceGrids()[1];
            final Grid uncorrelatedUncertaintyGrid = aggregationContext.getSourceGrids()[2];
            final Grid largeScaleCorrelatedUncertaintyGrid = aggregationContext.getSourceGrids()[3];
            final Grid analysedSstGrid = aggregationContext.getClimatologySstGrid();
            final Grid seaCoverageGrid = aggregationContext.getSeaCoverageGrid();

            final int x0 = rectangle.x;
            final int y0 = rectangle.y;
            final int x1 = x0 + rectangle.width - 1;
            final int y1 = y0 + rectangle.height - 1;
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    final double seaCoverage = seaCoverageGrid.getSampleDouble(x, y);
                    int qualityLevel = qualityLevelGrid.getSampleInt(x, y);
                    boolean valid = seaCoverage > 0.0 && qualityLevel == 5;
                    if (valid) {
                        sstAccu.accumulate(sstGrid.getSampleDouble(x, y), seaCoverage);
                        sstAnomalyAccu.accumulate(sstGrid.getSampleDouble(x, y) - analysedSstGrid.getSampleDouble(x, y),
                                                  seaCoverage);
                        uncorrelatedUncertaintyAccu.accumulate(uncorrelatedUncertaintyGrid.getSampleDouble(x, y),
                                                               seaCoverage);
                        largeScaleCorrelatedUncertaintyAccu.accumulate(
                                largeScaleCorrelatedUncertaintyGrid.getSampleDouble(x, y), seaCoverage);
                    }
                }
            }
        }
    }

    private static class L3UTemporalCell extends AbstractL3UCell
            implements CellAggregationCell<AggregationCell> { //used in regridding tool
        private final NumberAccumulator coverageUncertaintyAccu = new RandomUncertaintyAccumulator();
        protected final NumberAccumulator synopticallyCorrelatedUncertaintyAccu = new RandomUncertaintyAccumulator();
        protected final NumberAccumulator adjustmentUncertaintyAccu = new RandomUncertaintyAccumulator();


        private L3UTemporalCell(int x, int y) {
            super(null, x, y);
        }

        @Override
        public double computeCoverageUncertainty() {
            return coverageUncertaintyAccu.combine();
        }

        @Override
        public void accumulate(AggregationCell cell, double weight) {
            Number[] values = cell.getResults();
            //Note: know the ordering from AbstractL3UCell#getResults
            sstAccu.accumulate(values[0].floatValue(), 1);
            sstAnomalyAccu.accumulate(values[1].floatValue(), 1);
            coverageUncertaintyAccu.accumulate(values[2].floatValue(), 1);
            uncorrelatedUncertaintyAccu.accumulate(values[3].floatValue(), 1);
            largeScaleCorrelatedUncertaintyAccu.accumulate(values[4].floatValue(), 1);
            synopticallyCorrelatedUncertaintyAccu.accumulate(values[5].floatValue(), 1);
            adjustmentUncertaintyAccu.accumulate(values[6].floatValue(), 1);
        }
    }

    //Note: Combines the accumulators of L3UCell5 and SynopticCell5
    private static class L3UCell90 extends AbstractL3UCell implements CellAggregationCell<AggregationCell> {

        protected final NumberAccumulator synopticallyCorrelatedUncertaintyAccu = new ArithmeticMeanAccumulator();
        protected final NumberAccumulator adjustmentUncertaintyAccu = new ArithmeticMeanAccumulator();
        // New 5-to-90 deg coverage uncertainty aggregation
        protected final NumberAccumulator coverageUncertainty5Accu = new RandomUncertaintyAccumulator();

        private L3UCell90(AggregationContext context, int x, int y) {
            super(context, x, y);
        }

        public double computeCoverageUncertainty5Average() {
            return coverageUncertainty5Accu.combine();
        }

        @Override
        public double computeCoverageUncertainty() {
            final double uncertainty5 = computeCoverageUncertainty5Average();
            final double uncertainty90 = getAggregationContext().getCoverageUncertaintyProvider().calculate(this, 90.0);
            return Math.sqrt(uncertainty5 * uncertainty5 + uncertainty90 * uncertainty90);
        }

        @Override
        public long getSampleCount() {
            return Math.max(super.getSampleCount(), synopticallyCorrelatedUncertaintyAccu.getSampleCount());
        }

        public double computeSynopticallyCorrelatedUncertaintyAverage() {
            return synopticallyCorrelatedUncertaintyAccu.combine();
        }

        public double computeAdjustmentUncertaintyAverage() {
            return adjustmentUncertaintyAccu.combine();
        }

        @Override
        public void accumulate(AggregationCell cell, double seaCoverage90) {

            if (cell instanceof L3UCell5) {
                L3UCell5 cell5 = (L3UCell5) cell;
                sstAccu.accumulate(cell5.computeSstAverage(), seaCoverage90);
                sstAnomalyAccu.accumulate(cell5.computeSstAnomalyAverage(), seaCoverage90);
                // New 5-to-90 deg coverage uncertainty aggregation
                coverageUncertainty5Accu.accumulate(cell5.computeCoverageUncertainty(), seaCoverage90);
                uncorrelatedUncertaintyAccu.accumulate(cell5.computeUncorrelatedUncertaintyAverage(), seaCoverage90);
                largeScaleCorrelatedUncertaintyAccu.accumulate(cell5.computeLargeScaleCorrelatedUncertaintyAverage(),
                                                               seaCoverage90);

            } else if (cell instanceof L3USynopticCell5) {
                L3USynopticCell5 cellSynoptic = (L3USynopticCell5) cell;
                synopticallyCorrelatedUncertaintyAccu.accumulate(
                        cellSynoptic.computeSynopticallyCorrelatedUncertaintyAverage(), seaCoverage90);
                adjustmentUncertaintyAccu.accumulate(cellSynoptic.computeAdjustmentUncertaintyAverage(), seaCoverage90);

            } else {
                throw new IllegalStateException("L3UCell5 or L3USynopticCell5 expected.");
            }
        }

        @Override
        public Number[] getResults() {
            //Note: know the ordering from AbstractL3UCell#getResults
            Number[] superResults = super.getResults();
            Number[] results = Arrays.copyOf(superResults, superResults.length + 2);
            results[results.length - 2] = (float) computeSynopticallyCorrelatedUncertaintyAverage();
            results[results.length - 1] = (float) computeAdjustmentUncertaintyAverage();
            return results;
        }
    }

    private static class L3USynopticCell5 extends AbstractAggregationCell
            implements CellAggregationCell<L3USynopticAreaCell1> {

        protected final NumberAccumulator synopticallyCorrelatedUncertaintyAccu = new RandomUncertaintyAccumulator();
        protected final NumberAccumulator adjustmentUncertaintyAccu = new RandomUncertaintyAccumulator();

        protected L3USynopticCell5(int x, int y) {
            super(null, x, y);
        }

        @Override
        public long getSampleCount() {
            return synopticallyCorrelatedUncertaintyAccu.getSampleCount();
        }

        public double computeSynopticallyCorrelatedUncertaintyAverage() {
            return synopticallyCorrelatedUncertaintyAccu.combine();
        }

        public double computeAdjustmentUncertaintyAverage() {
            return adjustmentUncertaintyAccu.combine();
        }

        @Override
        public Number[] getResults() {
            return new Number[]{
                    (float) computeSynopticallyCorrelatedUncertaintyAverage(),
                    (float) computeAdjustmentUncertaintyAverage()
            };
        }

        @Override
        public void accumulate(L3USynopticAreaCell1 cell, double seaCoverage90) {
            synopticallyCorrelatedUncertaintyAccu.accumulate(cell.computeSynopticallyCorrelatedUncertaintyAverage(),
                                                             seaCoverage90);
            adjustmentUncertaintyAccu.accumulate(cell.computeAdjustmentUncertaintyAverage(), seaCoverage90);
        }
    }

    //Note: Only for RegionalAveraging Tool
    //Should regrid from input resolution to resolution of synoptic areas (1 °)
    private static class L3USynopticAreaCell1 extends AbstractAggregationCell implements SpatialAggregationCell {

        protected final NumberAccumulator synopticallyCorrelatedUncertaintyAccu = new ArithmeticMeanAccumulator();
        protected final NumberAccumulator adjustmentUncertaintyAccu = new ArithmeticMeanAccumulator();

        protected L3USynopticAreaCell1(int x, int y) {
            super(null, x, y);
        }

        @Override
        public long getSampleCount() {
            return synopticallyCorrelatedUncertaintyAccu.getSampleCount();
        }

        public double computeSynopticallyCorrelatedUncertaintyAverage() {
            return synopticallyCorrelatedUncertaintyAccu.combine();
        }

        public double computeAdjustmentUncertaintyAverage() {
            return adjustmentUncertaintyAccu.combine();
        }

        @Override
        public Number[] getResults() {
            return new Number[]{
                    (float) computeSynopticallyCorrelatedUncertaintyAverage(),
                    (float) computeAdjustmentUncertaintyAverage()
            };
        }

        @Override
        public void accumulate(AggregationContext aggregationContext, Rectangle rectangle) {
            final Grid qualityLevelGrid = aggregationContext.getSourceGrids()[1];
            final Grid synopticallyCorrelatedUncertaintyGrid = aggregationContext.getSourceGrids()[4];
            Grid adjustmentUncertaintyGrid = null;
            if (aggregationContext.getSourceGrids().length > 5) {
                adjustmentUncertaintyGrid = aggregationContext.getSourceGrids()[5];
            }
            final Grid seaCoverageGrid = aggregationContext.getSeaCoverageGrid();

            final int x0 = rectangle.x;
            final int y0 = rectangle.y;
            final int x1 = x0 + rectangle.width - 1;
            final int y1 = y0 + rectangle.height - 1;
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    final double seaCoverage = seaCoverageGrid.getSampleDouble(x, y);
                    int qualityLevel = qualityLevelGrid.getSampleInt(x, y);
                    boolean valid = seaCoverage > 0.0 && qualityLevel == 5;
                    if (valid) {
                        synopticallyCorrelatedUncertaintyAccu.accumulate(
                                synopticallyCorrelatedUncertaintyGrid.getSampleDouble(x, y), seaCoverage);
                        if (adjustmentUncertaintyGrid != null) {
                            adjustmentUncertaintyAccu.accumulate(adjustmentUncertaintyGrid.getSampleDouble(x, y),
                                                                 seaCoverage);
                        }
                    }
                }
            }
        }
    }

    private static abstract class L3UAggregation implements RegionalAggregation {

        protected final NumberAccumulator sstAccu = new ArithmeticMeanAccumulator();
        protected final NumberAccumulator sstAnomalyAccu = new ArithmeticMeanAccumulator();
        protected final NumberAccumulator coverageUncertaintyAccu = new RandomUncertaintyAccumulator();
        protected final NumberAccumulator uncorrelatedUncertaintyAccu = new RandomUncertaintyAccumulator();
        protected final NumberAccumulator largeScaleCorrelatedUncertaintyAccu = new ArithmeticMeanAccumulator();
        protected final NumberAccumulator synopticallyCorrelatedUncertaintyAccu = new RandomUncertaintyAccumulator();
        protected final NumberAccumulator adjustmentUncertaintyAccu = new RandomUncertaintyAccumulator();

        @Override
        public long getSampleCount() {
            return sstAccu.getSampleCount();
        }

        public double computeSstAverage() {
            return sstAccu.combine();
        }

        public double computeSstAnomalyAverage() {
            return sstAnomalyAccu.combine();
        }

        public double computeUncorrelatedUncertaintyAverage() {
            return uncorrelatedUncertaintyAccu.combine();
        }

        public double computeLargeScaleCorrelatedUncertaintyAverage() {
            return largeScaleCorrelatedUncertaintyAccu.combine();
        }

        public double computeCoverageUncertaintyAverage() {
            return coverageUncertaintyAccu.combine();
        }

        public double computeSynopticallyCorrelatedUncertaintyAverage() {
            return synopticallyCorrelatedUncertaintyAccu.combine();
        }

        public double computeAdjustmentUncertaintyAverage() {
            return adjustmentUncertaintyAccu.combine();
        }

        @Override
        public Number[] getResults() {
            // Note: Result types must match those defined in FileType.createOutputVariables().
            return new Number[]{
                    (float) computeSstAverage(),
                    (float) computeSstAnomalyAverage(),
                    (float) computeCoverageUncertaintyAverage(),
                    (float) computeUncorrelatedUncertaintyAverage(),
                    (float) computeLargeScaleCorrelatedUncertaintyAverage(),
                    (float) computeSynopticallyCorrelatedUncertaintyAverage(),
                    (float) computeAdjustmentUncertaintyAverage()
            };
        }
    }

    private static class L3USameMonthAggregation extends L3UAggregation
            implements SameMonthAggregation<AbstractAggregationCell> {

        @Override
        public void accumulate(AbstractAggregationCell cell, double seaCoverage) {

            if (cell instanceof L3UCell90) {
                L3UCell90 cell90 = (L3UCell90) cell;
                sstAccu.accumulate(cell90.computeSstAverage(), seaCoverage);
                sstAnomalyAccu.accumulate(cell90.computeSstAnomalyAverage(), seaCoverage);
                coverageUncertaintyAccu.accumulate(cell90.computeCoverageUncertainty(), seaCoverage);
                uncorrelatedUncertaintyAccu.accumulate(cell90.computeUncorrelatedUncertaintyAverage(), seaCoverage);
                largeScaleCorrelatedUncertaintyAccu.accumulate(cell90.computeLargeScaleCorrelatedUncertaintyAverage(),
                                                               seaCoverage);
                synopticallyCorrelatedUncertaintyAccu.accumulate(
                        cell90.computeSynopticallyCorrelatedUncertaintyAverage(), seaCoverage);
                adjustmentUncertaintyAccu.accumulate(cell90.computeAdjustmentUncertaintyAverage(), seaCoverage);

            } else if (cell instanceof L3UCell5) {
                L3UCell5 cell5 = (L3UCell5) cell;
                sstAccu.accumulate(cell5.computeSstAverage(), seaCoverage);
                sstAnomalyAccu.accumulate(cell5.computeSstAnomalyAverage(), seaCoverage);
                coverageUncertaintyAccu.accumulate(cell5.computeCoverageUncertainty(), seaCoverage);
                uncorrelatedUncertaintyAccu.accumulate(cell5.computeUncorrelatedUncertaintyAverage(), seaCoverage);
                largeScaleCorrelatedUncertaintyAccu.accumulate(cell5.computeLargeScaleCorrelatedUncertaintyAverage(),
                                                               seaCoverage);

            } else if (cell instanceof L3USynopticCell5) {
                L3USynopticCell5 cellSynoptic = (L3USynopticCell5) cell;
                synopticallyCorrelatedUncertaintyAccu.accumulate(
                        cellSynoptic.computeSynopticallyCorrelatedUncertaintyAverage(), seaCoverage);
                adjustmentUncertaintyAccu.accumulate(cellSynoptic.computeAdjustmentUncertaintyAverage(), seaCoverage);

            } else {
                throw new IllegalStateException("L3UCell90 or L3UCell5 or L3USynopticCell5 expected.");
            }
        }
    }

    private static class L3UMultiMonthAggregation extends L3UAggregation
            implements MultiMonthAggregation<L3UAggregation> {

        @Override
        public void accumulate(L3UAggregation aggregation) {
            sstAccu.accumulate(aggregation.computeSstAverage(), 1.0);
            sstAnomalyAccu.accumulate(aggregation.computeSstAnomalyAverage(), 1.0);
            coverageUncertaintyAccu.accumulate(aggregation.computeCoverageUncertaintyAverage(), 1.0);
            uncorrelatedUncertaintyAccu.accumulate(aggregation.computeUncorrelatedUncertaintyAverage(), 1.0);
            largeScaleCorrelatedUncertaintyAccu.accumulate(aggregation.computeLargeScaleCorrelatedUncertaintyAverage(),
                                                           1.0);
            synopticallyCorrelatedUncertaintyAccu.accumulate(
                    aggregation.computeSynopticallyCorrelatedUncertaintyAverage(), 1.0);
            adjustmentUncertaintyAccu.accumulate(aggregation.computeAdjustmentUncertaintyAverage(), 1.0);
        }
    }

    @Override
    public boolean hasSynopticUncertainties() {
        return true;
    }
}
