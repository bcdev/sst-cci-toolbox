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

import org.esa.cci.sst.aggregate.*;
import org.esa.cci.sst.cell.CellAggregationCell;
import org.esa.cci.sst.cell.CellFactory;
import org.esa.cci.sst.common.*;
import org.esa.cci.sst.accumulate.ArithmeticMeanAccumulator;
import org.esa.cci.sst.accumulate.NumberAccumulator;
import org.esa.cci.sst.accumulate.WeightedUncertaintyAccumulator;
import org.esa.cci.sst.common.cellgrid.YFlip;
import org.esa.cci.sst.file.FileType;
import org.esa.cci.sst.grid.Grid;
import org.esa.cci.sst.netcdf.NcTools;
import ucar.ma2.DataType;
import ucar.nc2.*;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * Represents the SST-CCI L3U and L3C file types.
 *
 * @author Norman Fomferra
 * @author Bettina Scholze
 * @author Ralf Quast
 */
class CciL3FileType extends AbstractCciFileType {

    final static FileType INSTANCE = new CciL3FileType();

    @Override
    public AggregationContext readSourceGrids(NetcdfFile datafile,
                                              SstDepth sstDepth,
                                              AggregationContext context) throws IOException {
        switch (sstDepth) {
            case depth_20:
            case depth_100:
                context.setSstGrid(readGrid(datafile, "sea_surface_temperature_depth", 0));
                break;
            case skin:
                context.setSstGrid(readGrid(datafile, "sea_surface_temperature", 0));
                break;
            default:
                throw new IllegalArgumentException(MessageFormat.format("sstDepth = {0}", sstDepth));
        }
        context.setQualityGrid(readGrid(datafile, "quality_level", 0));
        context.setRandomUncertaintyGrid(readGrid(datafile, "uncorrelated_uncertainty", 0));
        context.setLargeScaleUncertaintyGrid(readGrid(datafile, "large_scale_correlated_uncertainty", 0));
        context.setSynopticUncertaintyGrid(readGrid(datafile, "synoptically_correlated_uncertainty", 0));
        if (NcTools.hasVariable(datafile, "adjustment_uncertainty")) {
            context.setAdjustmentUncertaintyGrid(readGrid(datafile, "adjustment_uncertainty", 0));
        }

        return context;
    }

    private Grid readGrid(NetcdfFile datafile, String variableName, int z) throws IOException {
        return YFlip.create(NcTools.readGrid(datafile, variableName, getGridDef(), z));
    }

    @Override
    public Variable[] addResultVariables(NetcdfFileWriteable datafile, Dimension[] dims, SstDepth sstDepth) {
        final Variable[] variables = new Variable[9];

        final Variable sstVar = datafile.addVariable(String.format("sst_%s", sstDepth), DataType.FLOAT, dims);
        sstVar.addAttribute(new Attribute("units", "kelvin"));
        sstVar.addAttribute(new Attribute("long_name", String.format("SST %s", sstDepth)));
        sstVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable sstAnomalyVar = datafile.addVariable(String.format("sst_%s_anomaly", sstDepth), DataType.FLOAT,
                dims);
        sstAnomalyVar.addAttribute(new Attribute("units", "kelvin"));
        sstAnomalyVar.addAttribute(
                new Attribute("long_name", String.format("SST %s anomaly", sstDepth)));
        sstAnomalyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable coverageUncertaintyVar = datafile.addVariable("coverage_uncertainty", DataType.FLOAT, dims);
        coverageUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
        coverageUncertaintyVar.addAttribute(new Attribute("long_name", "coverage uncertainty"));
        coverageUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable uncorrelatedUncertaintyVar = datafile.addVariable("uncorrelated_uncertainty", DataType.FLOAT,
                dims);
        uncorrelatedUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
        uncorrelatedUncertaintyVar.addAttribute(
                new Attribute("long_name", "uncorrelated uncertainty"));
        uncorrelatedUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable largeScaleUncertaintyVar = datafile.addVariable("large_scale_correlated_uncertainty",
                DataType.FLOAT, dims);
        largeScaleUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
        largeScaleUncertaintyVar.addAttribute(
                new Attribute("long_name", "large scale correlated uncertainty"));
        largeScaleUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable synopticUncertaintyVar = datafile.addVariable("synoptically_correlated_uncertainty",
                DataType.FLOAT, dims);
        synopticUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
        synopticUncertaintyVar.addAttribute(
                new Attribute("long_name", "synoptically correlated uncertainty"));
        synopticUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable adjustmentUncertaintyVar = datafile.addVariable("adjustment_uncertainty", DataType.FLOAT, dims);
        adjustmentUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
        adjustmentUncertaintyVar.addAttribute(
                new Attribute("long_name", "adjustment uncertainty"));
        adjustmentUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        variables[Aggregation.SST] = sstVar;
        variables[Aggregation.SST_ANOMALY] = sstAnomalyVar;
        variables[Aggregation.RANDOM_UNCERTAINTY] = uncorrelatedUncertaintyVar;
        variables[Aggregation.COVERAGE_UNCERTAINTY] = coverageUncertaintyVar;
        variables[Aggregation.LARGE_SCALE_UNCERTAINTY] = largeScaleUncertaintyVar;
        variables[Aggregation.SYNOPTIC_UNCERTAINTY] = synopticUncertaintyVar;
        variables[Aggregation.ADJUSTMENT_UNCERTAINTY] = adjustmentUncertaintyVar;

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
            public SynopticCell5 createCell(int cellX, int cellY) {
                return new SynopticCell5(context, cellX, cellY);
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
        return "\\d{14}-ESACCI-L3[CU]_GHRSST-SST((skin)|(subskin)|(depth)|(fnd))-((ATSR1)|(ATSR2)|(AATSR)|(AMSRE)|(AVHRR_MTA)|(SEVIRI_SST)|(TMI))-((LT)|(DM))-v\\d{1,2}\\.\\d{1}-fv\\d{1,2}\\.\\d{1}.nc";
    }

    private static final class MultiPurposeAggregation extends AbstractAggregation implements RegionalAggregation,
            SameMonthAggregation<AggregationCell>,
            MultiMonthAggregation<RegionalAggregation> {

        private final NumberAccumulator sstAccumulator = new ArithmeticMeanAccumulator();
        private final NumberAccumulator sstAnomalyAccumulator = new ArithmeticMeanAccumulator();
        private final NumberAccumulator coverageUncertaintyAccumulator = new WeightedUncertaintyAccumulator();
        private final NumberAccumulator randomUncertaintyAccumulator = new WeightedUncertaintyAccumulator();
        private final NumberAccumulator largeScaleUncertaintyAccumulator = new ArithmeticMeanAccumulator();
        private final NumberAccumulator synopticUncertaintyAccumulator = new WeightedUncertaintyAccumulator();
        private final NumberAccumulator adjustmentUncertaintyAccumulator = new WeightedUncertaintyAccumulator();

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

}
