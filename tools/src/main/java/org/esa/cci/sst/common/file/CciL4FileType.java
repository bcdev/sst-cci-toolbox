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
import org.esa.cci.sst.common.ProcessingLevel;
import org.esa.cci.sst.common.RegionalAggregation;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.calculator.ArithmeticMeanAccumulator;
import org.esa.cci.sst.common.calculator.NumberAccumulator;
import org.esa.cci.sst.common.calculator.WeightedUncertaintyAccumulator;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellAggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.YFlip;
import org.esa.cci.sst.regavg.MultiMonthAggregation;
import org.esa.cci.sst.regavg.SameMonthAggregation;
import org.esa.cci.sst.util.NcUtils;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * Represents the SST-CCI L4 file type.
 *
 * @author Norman Fomferra
 * @author Bettina Scholze
 * @author Ralf Quast
 */
class CciL4FileType extends AbstractCciFileType {

    final static FileType INSTANCE = new CciL4FileType();

    @Override
    public String getFilenameRegex() {
        return "\\d{14}-" + getRdac() + "-" + ProcessingLevel.L4 + "_GHRSST-SST[a-z]{3,7}-[A-Z1-2_]{3,10}-[DMLT]{2}-v\\d{1,2}\\.\\d{1}-fv\\d{1,2}\\.\\d{1}.nc";

    }

    @Override
    public AggregationContext readSourceGrids(NetcdfFile datafile, SstDepth sstDepth,
                                              AggregationContext context) throws IOException {
        context.setSstGrid(readGrid(datafile, "analysed_sst", 0));
        context.setRandomUncertaintyGrid(readGrid(datafile, "analysis_error", 0));
        context.setSeaIceFractionGrid(readGrid(datafile, "sea_ice_fraction", 0));

        return context;
    }

    private Grid readGrid(NetcdfFile datafile, String variableName, int z) throws IOException {
        // TODO - check if grids need to be flipped
        return YFlip.create(NcUtils.readGrid(datafile, variableName, getGridDef(), z));
    }

    @Override
    public Variable[] addResultVariables(NetcdfFileWriteable datafile, Dimension[] dims, SstDepth sstDepth) {
        final Variable sstVar = datafile.addVariable(String.format("sst_%s", sstDepth), DataType.FLOAT, dims);
        sstVar.addAttribute(new Attribute("units", "kelvin"));
        sstVar.addAttribute(new Attribute("long_name", String.format("mean of sst %s", sstDepth)));
        sstVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable sstAnomalyVar = datafile.addVariable(String.format("sst_%s_anomaly", sstDepth), DataType.FLOAT,
                                                            dims);
        sstAnomalyVar.addAttribute(new Attribute("units", "kelvin"));
        sstAnomalyVar.addAttribute(new Attribute("long_name", String.format("mean of sst %s anomaly", sstDepth)));
        sstAnomalyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable seaIceCoverageVar = datafile.addVariable("sea_ice_fraction", DataType.FLOAT, dims);
        seaIceCoverageVar.addAttribute(new Attribute("units", "1"));
        seaIceCoverageVar.addAttribute(new Attribute("long_name", "mean of sea ice fraction"));
        seaIceCoverageVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable coverageUncertaintyVar = datafile.addVariable("coverage_uncertainty", DataType.FLOAT, dims);
        coverageUncertaintyVar.addAttribute(new Attribute("units", "1"));
        coverageUncertaintyVar.addAttribute(new Attribute("long_name", "mean of sampling/coverage uncertainty"));
        coverageUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable analysisErrorVar = datafile.addVariable("analysis_error", DataType.FLOAT, dims);
        analysisErrorVar.addAttribute(new Attribute("units", "kelvin"));
        analysisErrorVar.addAttribute(new Attribute("long_name", "mean of analysis_error"));
        analysisErrorVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable[] variables = new Variable[8];
        variables[Aggregation.SST] = sstVar;
        variables[Aggregation.SST_ANOMALY] = sstAnomalyVar;
        variables[Aggregation.RANDOM_UNCERTAINTY] = analysisErrorVar;
        variables[Aggregation.COVERAGE_UNCERTAINTY] = coverageUncertaintyVar;
        variables[Aggregation.SEA_ICE_FRACTION] = seaIceCoverageVar;

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

    private static class MultiPurposeAggregation extends AbstractAggregation implements RegionalAggregation,
                                                                                        SameMonthAggregation<AggregationCell>,
                                                                                        MultiMonthAggregation<RegionalAggregation> {

        private final NumberAccumulator sstAccumulator = new ArithmeticMeanAccumulator();
        private final NumberAccumulator sstAnomalyAccumulator = new ArithmeticMeanAccumulator();
        private final NumberAccumulator coverageUncertaintyAccumulator = new WeightedUncertaintyAccumulator();
        private final NumberAccumulator randomUncertaintyAccumulator = new WeightedUncertaintyAccumulator();
        private final NumberAccumulator seaIceFractionAccumulator = new ArithmeticMeanAccumulator();

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
            return Double.NaN;
        }

        @Override
        public double getCoverageUncertainty() {
            return coverageUncertaintyAccumulator.combine();
        }

        @Override
        public double getAdjustmentUncertainty() {
            return Double.NaN;
        }

        @Override
        public double getSynopticUncertainty() {
            return Double.NaN;
        }

        @Override
        public double getSeaIceFraction() {
            return seaIceFractionAccumulator.combine();
        }

        @Override
        public void accumulate(AggregationCell cell, double seaCoverage) {
            sstAccumulator.accumulate(cell.getSeaSurfaceTemperature(), seaCoverage);
            sstAnomalyAccumulator.accumulate(cell.getSeaSurfaceTemperatureAnomaly(), seaCoverage);
            coverageUncertaintyAccumulator.accumulate(cell.getCoverageUncertainty(), seaCoverage);
            randomUncertaintyAccumulator.accumulate(cell.getRandomUncertainty(), seaCoverage);
            seaIceFractionAccumulator.accumulate(cell.getSeaIceFraction(), 1.0);
        }

        @Override
        public void accumulate(RegionalAggregation aggregation) {
            sstAccumulator.accumulate(aggregation.getSeaSurfaceTemperature());
            sstAnomalyAccumulator.accumulate(aggregation.getSeaSurfaceTemperatureAnomaly());
            coverageUncertaintyAccumulator.accumulate(aggregation.getCoverageUncertainty());
            randomUncertaintyAccumulator.accumulate(aggregation.getRandomUncertainty());
            seaIceFractionAccumulator.accumulate(aggregation.getSeaIceFraction());
        }
    }

}
