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

import org.esa.cci.sst.common.AggregationFactory;
import org.esa.cci.sst.common.RegionalAggregation;
import org.esa.cci.sst.common.SpatialAggregationContext;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.calculator.*;
import org.esa.cci.sst.common.cell.*;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.regavg.MultiMonthAggregation;
import org.esa.cci.sst.regavg.SameMonthAggregation;
import org.esa.cci.sst.util.NcUtils;
import org.esa.cci.sst.util.UTC;
import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.nc2.Dimension;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static java.lang.Math.round;

/**
 * Represents the ARC_L3U file type.
 * <p/>
 * The filename regex pattern is <code>AT[12S]_AVG_3PAARC\d{8}_[DTEM]_[nd][ND][23][bms][.]nc([.]gz)?</code>
 * with
 * <p/>
 * AT[12S] = ATSR1, ATSR2, AATSR<br/>
 * \d{8} = date in the format YYYYMMDD <br/>
 * [DTEM] = daily, ?, ?, monthly
 * [nd] = night or day<br/>
 * [ND] = Nadir or Dual view<br/>
 * [23] = 2 or 3 channel retrieval (3 channel only valid during night)<br/>
 * [bms] = bayes, min-bayes, SADIST cloud screening<br/>
 * <p/>
 * Find more info in the <a href="https://www.wiki.ed.ac.uk/display/arcwiki/Test+Data#TestData-NetCDFDataFiles">arcwiki</a>.
 *
 * @author Norman Fomferra
 */
public class ArcL3UFileType implements FileType {

    public static final ArcL3UFileType INSTANCE = new ArcL3UFileType();

    public final DateFormat dateFormat = UTC.getDateFormat("yyyyMMdd");
    public final int filenameDateOffset = "ATS_AVG_3PAARC".length();
    public final GridDef gridDef = GridDef.createGlobal(3600, 1800); //source always in 0.01 ° resolution

    @Override
    public Date parseDate(File file) throws ParseException {
        String dateString = file.getName().substring(filenameDateOffset, filenameDateOffset + 8);
        return dateFormat.parse(dateString);
    }

    @Override
    public String getRdac() {
        return "ARC";
    }

    @Override
    public String getFilenameRegex() {
        return "AT[12S]_AVG_3PAARC\\d{8}_[DTEM]_[nd][ND][23][bms][.]nc([.]gz)?";
    }

    @Override
    public GridDef getGridDef() {
        return gridDef;
    }

    @Override
    public Date readDate(NetcdfFile file) throws IOException {
        Variable variable = file.findTopVariable("time");
        if (variable == null) {
            throw new IOException("Missing variable 'time' in file '" + file.getLocation() + "'");
        }
        // The time of ARC files is encoded as seconds since 01.01.1981
        int secondsSince1981 = round(variable.readScalarFloat());
        Calendar calendar = UTC.createCalendar(1981);
        calendar.add(Calendar.SECOND, secondsSince1981);
        return calendar.getTime();
    }

    @Override
    public Grid[] readSourceGrids(NetcdfFile file, SstDepth sstDepth) throws IOException {
        Grid[] grids = new Grid[2];
        if (sstDepth == SstDepth.depth_20) {
            grids[0] = NcUtils.readGrid(file, "sst_depth", getGridDef(), 0);
        } else if (sstDepth == SstDepth.depth_100) {
            grids[0] = NcUtils.readGrid(file, "sst_depth", getGridDef(), 1);
        } else /*if (sstDepth == SstDepth.skin)*/ {
            grids[0] = NcUtils.readGrid(file, "sst_skin", getGridDef(), 0);
        }
        grids[1] = NcUtils.readGrid(file, "uncertainty", getGridDef(), 0);
        return grids;
    }

    @Override
    public Variable[] createOutputVariables(NetcdfFileWriteable file, SstDepth sstDepth, boolean totalUncertainty, Dimension[] dims) {

        Variable sstVar = file.addVariable(String.format("sst_%s", sstDepth), DataType.FLOAT, dims);
        sstVar.addAttribute(new Attribute("units", "kelvin"));
        sstVar.addAttribute(new Attribute("long_name", String.format("mean of sst %s in kelvin", sstDepth)));
        sstVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        Variable sstAnomalyVar = file.addVariable(String.format("sst_%s_anomaly", sstDepth), DataType.FLOAT, dims);
        sstAnomalyVar.addAttribute(new Attribute("units", "kelvin"));
        sstAnomalyVar.addAttribute(new Attribute("long_name", String.format("mean of sst %s anomaly in kelvin", sstDepth)));
        sstAnomalyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        Variable coverageUncertaintyVar = file.addVariable("coverage_uncertainty", DataType.FLOAT, dims);
        coverageUncertaintyVar.addAttribute(new Attribute("units", "1"));
        coverageUncertaintyVar.addAttribute(new Attribute("long_name", "mean of sampling/coverage uncertainty"));
        coverageUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        Variable arcUncertaintyVar = file.addVariable("arc_uncertainty", DataType.FLOAT, dims);
        arcUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
        arcUncertaintyVar.addAttribute(new Attribute("long_name", "mean of arc uncertainty in kelvin"));
        arcUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        return new Variable[]{
                sstVar,
                sstAnomalyVar,
                coverageUncertaintyVar,
                arcUncertaintyVar
        };
    }

    @Override
    public AggregationFactory<SameMonthAggregation> getSameMonthAggregationFactory() {
        return new AggregationFactory<SameMonthAggregation>() {
            @Override
            public SameMonthAggregation createAggregation() {
                return new ArcL3USameMonthAggregation();
            }
        };
    }

    @Override
    public AggregationFactory<MultiMonthAggregation> getMultiMonthAggregationFactory() {
        return new AggregationFactory<MultiMonthAggregation>() {
            @Override
            public MultiMonthAggregation createAggregation() {
                return new ArcL3UMultiMonthAggregation();
            }
        };
    }

    @Override
    public CellFactory getCellFactory(final CellTypes cellType) {
        switch (cellType) {
            case TEMPORAL_CELL:
                return new CellFactory<AggregationCell>() {
                    @Override
                    public AggregationCell createCell(int cellX, int cellY) {
                        return new ArcL3UTemporalCell(cellX, cellY);
                    }
                };
            case SPATIAL_CELL_5: {
                return new CellFactory<SpatialAggregationCell>() {
                    @Override
                    public ArcL3UCell5 createCell(int cellX, int cellY) {
                        return new ArcL3UCell5(cellType.getCoverageUncertaintyProvider(), cellX, cellY);
                    }
                };
            }
            case SPATIAL_CELL_REGRIDDING: {
                return new CellFactory<SpatialAggregationCell>() {
                    @Override
                    public ArcL3URegriddingCell createCell(int cellX, int cellY) {
                        return new ArcL3URegriddingCell(cellType.getCoverageUncertaintyProvider(),
                                CellTypes.getMinCoverage(), cellX, cellY);
                    }
                };
            }
            case CELL_90: {
                return new CellFactory<CellAggregationCell>() {
                    @Override
                    public ArcL3UCell90 createCell(int cellX, int cellY) {
                        return new ArcL3UCell90(cellType.getCoverageUncertaintyProvider(), cellX, cellY);
                    }
                };
            }
            default:
                throw new IllegalStateException("never come here");
        }
    }

    private static abstract class AbstractArcL3UCell extends AbstractAggregationCell {
        protected final NumberAccumulator sstAccu = new ArithmeticMeanAccumulator();
        protected final NumberAccumulator sstAnomalyAccu = new ArithmeticMeanAccumulator();
        protected final NumberAccumulator arcUncertaintyAccu = new RandomUncertaintyAccumulator();

        private AbstractArcL3UCell(CoverageUncertainty coverageUncertaintyProvider, int x, int y) {
            super(coverageUncertaintyProvider, x, y);
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

        public double computeArcUncertaintyAverage() {
            return arcUncertaintyAccu.combine();
        }

        public abstract double computeCoverageUncertainty();

        @Override
        public Number[] getResults() {
            // Note: Result types must match those defined in FileType.createOutputVariables().
            return new Number[]{
                    (float) computeSstAverage(),
                    (float) computeSstAnomalyAverage(),
                    (float) computeCoverageUncertainty(),
                    (float) computeArcUncertaintyAverage()
            };
        }
    }

    private static class ArcL3UTemporalCell extends AbstractArcL3UCell implements CellAggregationCell<AggregationCell> {
        private final NumberAccumulator coverageUncertaintyAccu = new RandomUncertaintyAccumulator();

        private ArcL3UTemporalCell(int x, int y) {
            super(null, x, y);
        }

        @Override
        public double computeCoverageUncertainty() {
            return coverageUncertaintyAccu.combine();
        }

        @Override
        public void accumulate(AggregationCell cell, double weight) {
            Number[] values = cell.getResults();
            sstAccu.accumulate(values[0].floatValue(), 1);
            sstAnomalyAccu.accumulate(values[1].floatValue(), 1);
            coverageUncertaintyAccu.accumulate(values[2].floatValue(), 1);
            arcUncertaintyAccu.accumulate(values[3].floatValue(), 1);
        }
    }


    private static class ArcL3UCell5 extends AbstractArcL3UCell implements SpatialAggregationCell {

        private ArcL3UCell5(CoverageUncertainty coverageUncertaintyProvider, int x, int y) {
            super(coverageUncertaintyProvider, x, y);
        }

        @Override
        public double computeCoverageUncertainty() {
            return getCoverageUncertainty().calculate(this, 5.0);
        }

        @Override
        public void accumulate(SpatialAggregationContext spatialAggregationContext, Rectangle rect) {

            final Grid sstGrid = spatialAggregationContext.getSourceGrids()[0];
            final Grid uncertaintyGrid = spatialAggregationContext.getSourceGrids()[1];
            final Grid analysedSstGrid = spatialAggregationContext.getClimatologySst();
            final Grid seaCoverageGrid = spatialAggregationContext.getClimatologySeaCoverage();

            final int x0 = rect.x;
            final int y0 = rect.y;
            final int x1 = x0 + rect.width - 1;
            final int y1 = y0 + rect.height - 1;
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    final double seaCoverage = seaCoverageGrid.getSampleDouble(x, y);
                    if (seaCoverage > 0.0) {
                        sstAccu.accumulate(sstGrid.getSampleDouble(x, y), seaCoverage);
                        sstAnomalyAccu.accumulate(sstGrid.getSampleDouble(x, y) - analysedSstGrid.getSampleDouble(x, y), seaCoverage);
                        arcUncertaintyAccu.accumulate(uncertaintyGrid.getSampleDouble(x, y), seaCoverage);
                    }
                }
            }
        }
    }

    private static class ArcL3URegriddingCell extends AbstractArcL3UCell implements SpatialAggregationCell {
        private double minCoverage;
        private int maximumSampleCount;
        protected final NumberAccumulator stdDeviationAccu = new SquaredAverageAccumulator();


        private ArcL3URegriddingCell(CoverageUncertainty coverageUncertaintyProvider, double minCoverage, int x, int y) {
            super(coverageUncertaintyProvider, x, y);
            this.minCoverage = minCoverage;
        }

        @Override
        public double computeCoverageUncertainty() {
            return getCoverageUncertainty().calculate(this, stdDeviationAccu.combine());
        }

        @Override
        public void accumulate(SpatialAggregationContext spatialAggregationContext, Rectangle rect) {
            maximumSampleCount = rect.height * rect.width;

            final Grid sstGrid = spatialAggregationContext.getSourceGrids()[0];
            final Grid uncertaintyGrid = spatialAggregationContext.getSourceGrids()[1];
            final Grid analysedSstGrid = spatialAggregationContext.getClimatologySst();
            final Grid seaCoverageGrid = spatialAggregationContext.getClimatologySeaCoverage();
            final Grid stdDeviationGrid = spatialAggregationContext.getStandardDeviation();

            final int x0 = rect.x;
            final int y0 = rect.y;
            final int x1 = x0 + rect.width - 1;
            final int y1 = y0 + rect.height - 1;
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    final double seaCoverage = seaCoverageGrid.getSampleDouble(x, y);
                    final double sst = sstGrid.getSampleDouble(x, y);
                    if (seaCoverage > 0.0 && sst > 0.0) {
                        sstAccu.accumulate(sst, seaCoverage);
                        sstAnomalyAccu.accumulate(sst - analysedSstGrid.getSampleDouble(x, y), seaCoverage);
                        arcUncertaintyAccu.accumulate(uncertaintyGrid.getSampleDouble(x, y), seaCoverage);
                        stdDeviationAccu.accumulate(stdDeviationGrid.getSampleDouble(x, y), seaCoverage);
                    }
                }
            }
        }

        @Override
        public Number[] getResults() {
            // Note: Result types must match those defined in FileType.createOutputVariables().
            return new Number[]{
                    (float) checkMinCoverage(computeSstAverage()),
                    (float) checkMinCoverage(computeSstAnomalyAverage()),
                    (float) checkMinCoverage(computeCoverageUncertainty()),
                    (float) checkMinCoverage(computeArcUncertaintyAverage())
            };
        }

        private double checkMinCoverage(double result) {
            boolean hasMinCoverage = minCoverage < sstAccu.getSampleCount() * 1.0 / maximumSampleCount;

            if (hasMinCoverage) {
                return result;
            } else {
                return Double.NaN;
            }
        }
    }

    private static class ArcL3UCell90 extends AbstractArcL3UCell implements CellAggregationCell<ArcL3UCell5> {
        // New 5-to-90 deg coverage uncertainty aggregation
        protected final NumberAccumulator coverageUncertainty5Accu = new RandomUncertaintyAccumulator();

        private ArcL3UCell90(CoverageUncertainty coverageUncertaintyProvider, int x, int y) {
            super(coverageUncertaintyProvider, x, y);
        }

        public double computeCoverageUncertainty5Average() {
            return coverageUncertainty5Accu.combine();
        }

        @Override
        public double computeCoverageUncertainty() {
            final double uncertainty5 = computeCoverageUncertainty5Average();
            final double uncertainty90 = getCoverageUncertainty().calculate(this, 90.0);
            return Math.sqrt(uncertainty5 * uncertainty5 + uncertainty90 * uncertainty90);
        }

        @Override
        public void accumulate(ArcL3UCell5 cell, double seaCoverage90) {
            sstAccu.accumulate(cell.computeSstAverage(), seaCoverage90);
            sstAnomalyAccu.accumulate(cell.computeSstAnomalyAverage(), seaCoverage90);
            arcUncertaintyAccu.accumulate(cell.computeArcUncertaintyAverage(), seaCoverage90);
            // New 5-to-90 deg coverage uncertainty aggregation  
            coverageUncertainty5Accu.accumulate(cell.computeCoverageUncertainty(), seaCoverage90);
        }
    }

    private static class ArcL3UAggregation implements RegionalAggregation {
        protected final NumberAccumulator sstAccu = new ArithmeticMeanAccumulator();
        protected final NumberAccumulator sstAnomalyAccu = new ArithmeticMeanAccumulator();
        protected final NumberAccumulator arcUncertaintyAccu = new RandomUncertaintyAccumulator();
        protected final NumberAccumulator coverageUncertaintyAccu = new RandomUncertaintyAccumulator();

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

        public double computeArcUncertaintyAverage() {
            return arcUncertaintyAccu.combine();
        }

        public double computeCoverageUncertaintyAverage() {
            return coverageUncertaintyAccu.combine();
        }

        @Override
        public Number[] getResults() {
            // Note: Result types must match those defined in FileType.createOutputVariables().
            return new Number[]{
                    (float) computeSstAverage(),
                    (float) computeSstAnomalyAverage(),
                    (float) computeCoverageUncertaintyAverage(),
                    (float) computeArcUncertaintyAverage()
            };
        }
    }

    private static class ArcL3USameMonthAggregation extends ArcL3UAggregation implements SameMonthAggregation<AbstractArcL3UCell> {
        @Override
        public void accumulate(AbstractArcL3UCell cell, double seaCoverage) {
            sstAccu.accumulate(cell.computeSstAverage(), seaCoverage);
            sstAnomalyAccu.accumulate(cell.computeSstAnomalyAverage(), seaCoverage);
            arcUncertaintyAccu.accumulate(cell.computeArcUncertaintyAverage(), seaCoverage);
            coverageUncertaintyAccu.accumulate(cell.computeCoverageUncertainty(), seaCoverage);
        }
    }

    private static class ArcL3UMultiMonthAggregation extends ArcL3UAggregation implements MultiMonthAggregation<ArcL3UAggregation> {
        @Override
        public void accumulate(ArcL3UAggregation aggregation) {
            sstAccu.accumulate(aggregation.computeSstAverage(), 1.0);
            sstAnomalyAccu.accumulate(aggregation.computeSstAnomalyAverage(), 1.0);
            arcUncertaintyAccu.accumulate(aggregation.computeArcUncertaintyAverage(), 1.0);
            coverageUncertaintyAccu.accumulate(aggregation.computeCoverageUncertaintyAverage(), 1.0);
        }
    }

    @Override
    public boolean hasSynopticUncertainties() {
        return false;
    }
}
