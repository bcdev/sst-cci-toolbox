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

package org.esa.cci.sst.regavg.filetypes;

import org.esa.cci.sst.regavg.*;
import org.esa.cci.sst.util.*;
import org.esa.cci.sst.util.accumulators.ArithmeticMeanAccumulator;
import org.esa.cci.sst.util.accumulators.RandomUncertaintyAccumulator;
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
 * The filename regex pattern is <code>AT[12S]_AVG_3PAARC\d{8}_[DTEM]_[nd][ND][23][bms][.]nc[.]gz</code>
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

    public final static ArcL3UFileType INSTANCE = new ArcL3UFileType();
    public final DateFormat dateFormat = UTC.getDateFormat("yyyyMMdd");
    public final int filenameDateOffset = "ATS_AVG_3PAARC".length();
    public final GridDef gridDef = GridDef.createGlobalGrid(3600, 1800);

    @Override
    public Date parseDate(File file) throws ParseException {
        String dateString = file.getName().substring(filenameDateOffset, filenameDateOffset + 8);
        return dateFormat.parse(dateString);
    }

    @Override
    public String getFilenameRegex() {
        return "AT[12S]_AVG_3PAARC\\d{8}_[DTEM]_[nd][ND][23][bms][.]nc[.]gz";
    }

    @Override
    public GridDef getGridDef() {
        return gridDef;
    }

    @Override
    public ProcessingLevel getProcessingLevel() {
        return ProcessingLevel.L3U;
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
    public Variable[] createOutputVariables(NetcdfFileWriteable file, SstDepth sstDepth, Dimension[] dims) {

        Variable sstVar = file.addVariable(String.format("sst_%s", sstDepth), DataType.FLOAT, dims);
        sstVar.addAttribute(new Attribute("units", "kelvin"));
        sstVar.addAttribute(new Attribute("long_name", String.format("mean of sst %s in kelvin", sstDepth)));
        sstVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        Variable sstAnomalyVar = file.addVariable(String.format("sst_%s_anomaly", sstDepth), DataType.FLOAT, dims);
        sstAnomalyVar.addAttribute(new Attribute("units", "kelvin"));
        sstAnomalyVar.addAttribute(new Attribute("long_name", String.format("mean of sst %s anomaly in kelvin", sstDepth)));
        sstAnomalyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        Variable arcUncertaintyVar = file.addVariable("arc_uncertainty", DataType.FLOAT, dims);
        arcUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
        arcUncertaintyVar.addAttribute(new Attribute("long_name", "mean of arc uncertainty in kelvin"));
        arcUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        Variable coverageUncertaintyVar = file.addVariable("coverage_uncertainty", DataType.FLOAT, dims);
        coverageUncertaintyVar.addAttribute(new Attribute("units", "1"));
        coverageUncertaintyVar.addAttribute(new Attribute("long_name", "mean of sampling/coverage uncertainty"));
        coverageUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        /*
        Variable sampleCountVar = file.addVariable("sample_count", DataType.DOUBLE, dims);
        arcUncertaintyVar.addAttribute(new Attribute("units", "1"));
        sampleCountVar.addAttribute(new Attribute("long_name", String.format("counts of sst %s contributions.", sstDepth)));
        */

        return new Variable[]{
                sstVar,
                sstAnomalyVar,
                arcUncertaintyVar,
                coverageUncertaintyVar,
                // sampleCountVar,
        };
    }


    @Override
    public CellFactory<AggregationCell5> getCell5Factory(final CoverageUncertaintyProvider coverageUncertaintyProvider) {
        return new CellFactory<AggregationCell5>() {
            @Override
            public MyCell5 createCell(int cellX, int cellY) {
                return new MyCell5(coverageUncertaintyProvider, cellX, cellY);
            }
        };
    }

    @Override
    public CellFactory<AggregationCell90> getCell90Factory(final CoverageUncertaintyProvider coverageUncertaintyProvider) {
        return new CellFactory<AggregationCell90>() {
            @Override
            public MyCell90 createCell(int cellX, int cellY) {
                return new MyCell90(coverageUncertaintyProvider, cellX, cellY);
            }
        };
    }

    @Override
    public AggregationFactory<SameMonthAggregation> getSameMonthAggregationFactory() {
        return new AggregationFactory<SameMonthAggregation>() {
            @Override
            public SameMonthAggregation createAggregation() {
                return new MySameMonthAggregation();
            }
        };
    }

    @Override
    public AggregationFactory<MultiMonthAggregation> getMultiMonthAggregationFactory() {
        return new AggregationFactory<MultiMonthAggregation>() {
            @Override
            public MultiMonthAggregation createAggregation() {
                return new MyMultiMonthAggregation();
            }
        };
    }

    private static abstract class MyAggregationCell extends AbstractAggregationCell {

        protected final Accumulator sstAccu = new ArithmeticMeanAccumulator();
        protected final Accumulator sstAnomalyAccu = new ArithmeticMeanAccumulator();
        protected final Accumulator arcUncertaintyAccu = new RandomUncertaintyAccumulator();

        private MyAggregationCell(CoverageUncertaintyProvider coverageUncertaintyProvider, int x, int y) {
            super(coverageUncertaintyProvider, x, y);
        }

        @Override
        public long getSampleCount() {
            return sstAnomalyAccu.getSampleCount();
        }

        public double computeSstAverage() {
            return sstAccu.computeAverage();
        }

        public double computeSstAnomalyAverage() {
            return sstAnomalyAccu.computeAverage();
        }

        public double computeArcUncertaintyAverage() {
            return arcUncertaintyAccu.computeAverage();
        }

        public abstract double computeCoverageUncertainty();

        @Override
        public Number[] getResults() {
            // Note: Result types must match those defined in FileType.createOutputVariables().
            return new Number[]{
                    (float) computeSstAverage(),
                    (float) computeSstAnomalyAverage(),
                    (float) computeArcUncertaintyAverage(),
                    (float) computeCoverageUncertainty()
            };
        }
    }

    private static class MyCell5 extends MyAggregationCell implements AggregationCell5 {

        private MyCell5(CoverageUncertaintyProvider coverageUncertaintyProvider, int x, int y) {
            super(coverageUncertaintyProvider, x, y);
        }

        @Override
        public double computeCoverageUncertainty() {
            return getCoverageUncertaintyProvider().getCoverageUncertainty5(getX(), getY(), sstAnomalyAccu.getSampleCount());
        }

        @Override
        public void accumulate(AggregationCell5Context aggregationCell5Context, Rectangle rect) {
            final Grid sstGrid = aggregationCell5Context.getSourceGrids()[0];
            final Grid uncertaintyGrid = aggregationCell5Context.getSourceGrids()[1];
            final Grid analysedSstGrid = aggregationCell5Context.getAnalysedSstGrid();
            final Grid seaCoverageGrid = aggregationCell5Context.getSeaCoverageGrid();

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

    private static class MyCell90 extends MyAggregationCell implements AggregationCell90<MyCell5> {

        private MyCell90(CoverageUncertaintyProvider coverageUncertaintyProvider, int x, int y) {
            super(coverageUncertaintyProvider, x, y);
        }

        @Override
        public double computeCoverageUncertainty() {
            return getCoverageUncertaintyProvider().getCoverageUncertainty90(getX(), getY(), sstAnomalyAccu.getSampleCount());
        }

        @Override
        public void accumulate(MyCell5 cell, double seaCoverage90) {
            sstAccu.accumulate(cell.computeSstAverage(), seaCoverage90);
            sstAnomalyAccu.accumulate(cell.computeSstAnomalyAverage(), seaCoverage90);
            arcUncertaintyAccu.accumulate(cell.computeArcUncertaintyAverage(), seaCoverage90);
        }
    }

    private static class MyRegionalAggregation implements RegionalAggregation {

        protected final Accumulator sstAccu = new ArithmeticMeanAccumulator();
        protected final Accumulator sstAnomalyAccu = new ArithmeticMeanAccumulator();
        protected final Accumulator arcUncertaintyAccu = new RandomUncertaintyAccumulator();
        protected final Accumulator coverageUncertaintyAccu = new RandomUncertaintyAccumulator();

        @Override
        public long getSampleCount() {
            return sstAccu.getSampleCount();
        }

        public double computeSstAverage() {
            return sstAccu.computeAverage();
        }

        public double computeSstAnomalyAverage() {
            return sstAnomalyAccu.computeAverage();
        }

        public double computeArcUncertaintyAverage() {
            return arcUncertaintyAccu.computeAverage();
        }

        public double computeCoverageUncertaintyAverage() {
            return coverageUncertaintyAccu.computeAverage();
        }

        @Override
        public Number[] getResults() {
            // Note: Result types must match those defined in FileType.createOutputVariables().
            return new Number[]{
                    (float) computeSstAverage(),
                    (float) computeSstAnomalyAverage(),
                    (float) computeArcUncertaintyAverage(),
                    (float) computeCoverageUncertaintyAverage()
            };
        }

    }

    private static class MySameMonthAggregation extends MyRegionalAggregation implements SameMonthAggregation<MyAggregationCell> {
        @Override
        public void accumulate(MyAggregationCell cell, double seaCoverage) {
            sstAccu.accumulate(cell.computeSstAverage(), seaCoverage);
            sstAnomalyAccu.accumulate(cell.computeSstAnomalyAverage(), seaCoverage);
            arcUncertaintyAccu.accumulate(cell.computeArcUncertaintyAverage(), seaCoverage);
            coverageUncertaintyAccu.accumulate(cell.computeCoverageUncertainty(), seaCoverage);
        }
    }

    private static class MyMultiMonthAggregation extends MyRegionalAggregation implements MultiMonthAggregation<MyRegionalAggregation> {

        @Override
        public void accumulate(MyRegionalAggregation aggregation) {
            sstAccu.accumulate(aggregation.computeSstAverage(), 1.0);
            sstAnomalyAccu.accumulate(aggregation.computeSstAnomalyAverage(), 1.0);
            arcUncertaintyAccu.accumulate(aggregation.computeArcUncertaintyAverage(), 1.0);
            coverageUncertaintyAccu.accumulate(aggregation.computeCoverageUncertaintyAverage(), 1.0);
        }
    }
}
