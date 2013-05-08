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
import org.esa.cci.sst.common.calculator.WeightedUncertaintyAccumulator;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellAggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.regavg.MultiMonthAggregation;
import org.esa.cci.sst.regavg.SameMonthAggregation;
import org.esa.cci.sst.util.NcUtils;
import org.esa.cci.sst.util.UTC;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * Represents the ARC_L3U file type.
 * <p/>
 * Further info in the <a href="https://www.wiki.ed.ac.uk/display/arcwiki/Test+Data#TestData-NetCDFDataFiles">arcwiki</a>.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public final class ArcL3FileType implements FileType {

    public static final ArcL3FileType INSTANCE = new ArcL3FileType();

    private static final DateFormat DATE_FORMAT = UTC.getDateFormat("yyyyMMdd");
    private static final int FILENAME_DATE_OFFSET = "ATS_AVG_3PAARC".length();
    private static final GridDef GRID_DEF = GridDef.createGlobal(3600, 1800); // 0.01°

    @Override
    public Date parseDate(String filename) throws ParseException {
        final String dateString = filename.substring(FILENAME_DATE_OFFSET, FILENAME_DATE_OFFSET + 8);
        return DATE_FORMAT.parse(dateString);
    }

    @Override
    public String getRdac() {
        return "ARC";
    }

    /**
     * The filename regex pattern is <code>AT[12S]_AVG_3PAARC\d{8}_[DTEM]_[nd][ND][23][bms][.]nc([.]gz)?</code>,
     * where
     * <p/>
     * AT[12S] = ATSR1, ATSR2, AATSR<br/>
     * \d{8} = date in the format YYYYMMDD <br/>
     * [DTEM] = daily, ?, ?, monthly
     * [nd] = night or day<br/>
     * [ND] = Nadir or Dual view<br/>
     * [23] = 2 or 3 channel retrieval (3 channel only valid during night)<br/>
     * [bms] = bayes, min-bayes, SADIST cloud screening<br/>
     *
     * @return the filename regex.
     */
    @Override
    public String getFilenameRegex() {
        return "AT[12S]_AVG_3PAARC\\d{8}_[DTEM]_[nd][ND][23][bms][.]nc([.]gz)?";
    }

    @Override
    public GridDef getGridDef() {
        return GRID_DEF;
    }

    @Override
    public Date readDate(NetcdfFile dataFile) throws IOException {
        final Variable variable = dataFile.findTopVariable("time");
        if (variable == null) {
            throw new IOException("Missing variable 'time' in dataFile '" + dataFile.getLocation() + "'");
        }
        // time of ARC is encoded as seconds since 01.01.1981
        final int secondsSince1981 = Math.round(variable.readScalarFloat());
        final Calendar calendar = UTC.createCalendar(1981);
        calendar.add(Calendar.SECOND, secondsSince1981);

        return calendar.getTime();
    }

    @Override
    public AggregationContext readSourceGrids(NetcdfFile dataFile, SstDepth sstDepth, AggregationContext context) throws
                                                                                                                  IOException {
        switch (sstDepth) {
            case skin:
                context.setSstGrid(NcUtils.readGrid(dataFile, "sst_skin", getGridDef(), 0));
                break;
            case depth_20:
                context.setSstGrid(NcUtils.readGrid(dataFile, "sst_depth", getGridDef(), 0));
                break;
            case depth_100:
                context.setSstGrid(NcUtils.readGrid(dataFile, "sst_depth", getGridDef(), 1));
                break;
            default:
                throw new IllegalArgumentException(MessageFormat.format("sstDept = {0}", sstDepth));
        }
        context.setRandomUncertaintyGrid(NcUtils.readGrid(dataFile, "uncertainty", getGridDef(), 0));
        return context;
    }

    @Override
    public Variable[] addResultVariables(NetcdfFileWriteable dataFile, Dimension[] dims, SstDepth sstDepth) {
        final Variable sstVar = dataFile.addVariable(String.format("sst_%s", sstDepth), DataType.FLOAT, dims);
        sstVar.addAttribute(new Attribute("units", "kelvin"));
        sstVar.addAttribute(new Attribute("long_name", String.format("mean of sst %s", sstDepth)));
        sstVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable sstAnomalyVar = dataFile.addVariable(String.format("sst_%s_anomaly", sstDepth), DataType.FLOAT, dims);
        sstAnomalyVar.addAttribute(new Attribute("units", "kelvin"));
        sstAnomalyVar.addAttribute(new Attribute("long_name", String.format("mean of sst %s anomaly", sstDepth)));
        sstAnomalyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable coverageUncertaintyVar = dataFile.addVariable("coverage_uncertainty", DataType.FLOAT, dims);
        coverageUncertaintyVar.addAttribute(new Attribute("units", "1"));
        coverageUncertaintyVar.addAttribute(new Attribute("long_name", "mean of sampling/coverage uncertainty"));
        coverageUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable arcUncertaintyVar = dataFile.addVariable("arc_uncertainty", DataType.FLOAT, dims);
        arcUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
        arcUncertaintyVar.addAttribute(new Attribute("long_name", "mean of ARC uncertainty"));
        arcUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable[] variables = new Variable[8];
        variables[Aggregation.SST] = sstVar;
        variables[Aggregation.SST_ANOMALY] = sstAnomalyVar;
        variables[Aggregation.RANDOM_UNCERTAINTY] = arcUncertaintyVar;
        variables[Aggregation.COVERAGE_UNCERTAINTY] = coverageUncertaintyVar;

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
    public CellFactory<SpatialAggregationCell> getSpatialAggregationCellFactory(AggregationContext context) {
        return new SpatialAggregationCellFactory(context);
    }

    @Override
    public CellFactory<CellAggregationCell<AggregationCell>> getTemporalAggregationCellFactory() {
        return new TemporalAggregationCellFactory();
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
    public CellFactory<SpatialAggregationCell> getCellFactory5(final AggregationContext context) {
        return new CellFactory<SpatialAggregationCell>() {
            @Override
            public Cell5 createCell(int cellX, int cellY) {
                return new Cell5(context, cellX, cellY);
            }
        };
    }

    private static final class Cell5 extends DefaultSpatialAggregationCell {

        private Cell5(AggregationContext context, int cellX, int cellY) {
            super(context, cellX, cellY);
        }

        @Override
        public double getCoverageUncertainty() {
            return getAggregationContext().getCoverageUncertaintyProvider().calculate(this, 5.0);
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
        private final NumberAccumulator randomUncertaintyAccumulator = new WeightedUncertaintyAccumulator();
        private final NumberAccumulator coverageUncertaintyAccumulator = new WeightedUncertaintyAccumulator();

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
        public double getCoverageUncertainty() {
            return coverageUncertaintyAccumulator.combine();
        }

        @Override
        public void accumulate(AggregationCell cell, double seaCoverage) {
            sstAccumulator.accumulate(cell.getSeaSurfaceTemperature(), seaCoverage);
            sstAnomalyAccumulator.accumulate(cell.getSeaSurfaceTemperatureAnomaly(), seaCoverage);
            randomUncertaintyAccumulator.accumulate(cell.getRandomUncertainty(), seaCoverage);
            coverageUncertaintyAccumulator.accumulate(cell.getCoverageUncertainty(), seaCoverage);
        }

        @Override
        public void accumulate(RegionalAggregation aggregation) {
            sstAccumulator.accumulate(aggregation.getSeaSurfaceTemperature());
            sstAnomalyAccumulator.accumulate(aggregation.getSeaSurfaceTemperatureAnomaly());
            randomUncertaintyAccumulator.accumulate(aggregation.getRandomUncertainty());
            coverageUncertaintyAccumulator.accumulate(aggregation.getCoverageUncertainty());
        }
    }

    @Override
    public boolean hasSynopticUncertainties() {
        return false;
    }
}
