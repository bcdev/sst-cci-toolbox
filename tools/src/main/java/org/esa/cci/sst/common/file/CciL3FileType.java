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
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Norman Fomferra
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public class CciL3FileType extends AbstractCciFileType {

    public final static CciL3FileType INSTANCE = new CciL3FileType();

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
    public Variable[] addResultVariables(NetcdfFileWriteable file, Dimension[] dims, SstDepth sstDepth) {
        final Variable[] variables = new Variable[9];

        final Variable sstVar = file.addVariable(String.format("sst_%s", sstDepth), DataType.FLOAT, dims);
        sstVar.addAttribute(new Attribute("units", "kelvin"));
        sstVar.addAttribute(new Attribute("long_name", String.format("mean of sst %s in kelvin", sstDepth)));
        sstVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable sstAnomalyVar = file.addVariable(String.format("sst_%s_anomaly", sstDepth), DataType.FLOAT, dims);
        sstAnomalyVar.addAttribute(new Attribute("units", "kelvin"));
        sstAnomalyVar.addAttribute(
                new Attribute("long_name", String.format("mean of sst %s anomaly in kelvin", sstDepth)));
        sstAnomalyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable coverageUncertaintyVar = file.addVariable("coverage_uncertainty", DataType.FLOAT, dims);
        coverageUncertaintyVar.addAttribute(new Attribute("units", "1"));
        coverageUncertaintyVar.addAttribute(new Attribute("long_name", "coverage uncertainty"));
        coverageUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable uncorrelatedUncertaintyVar = file.addVariable("uncorrelated_uncertainty", DataType.FLOAT, dims);
        uncorrelatedUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
        uncorrelatedUncertaintyVar.addAttribute(
                new Attribute("long_name", "uncorrelated uncertainty in kelvin"));
        uncorrelatedUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable largeScaleUncertaintyVar = file.addVariable("large_scale_correlated_uncertainty",
                                                                       DataType.FLOAT, dims);
        largeScaleUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
        largeScaleUncertaintyVar.addAttribute(
                new Attribute("long_name", "large scale correlated uncertainty in kelvin"));
        largeScaleUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable synopticUncertaintyVar = file.addVariable("synoptically_correlated_uncertainty",
                                                                         DataType.FLOAT, dims);
        synopticUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
        synopticUncertaintyVar.addAttribute(
                new Attribute("long_name", "synoptically correlated uncertainty in kelvin"));
        synopticUncertaintyVar.addAttribute(new Attribute("_FillValue", Float.NaN));

        final Variable adjustmentUncertaintyVar = file.addVariable("adjustment_uncertainty", DataType.FLOAT, dims);
        adjustmentUncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
        adjustmentUncertaintyVar.addAttribute(
                new Attribute("long_name", "adjustment uncertainty in kelvin"));
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
//        return "\\d{14}-" + getRdac() + "-" + "L3[CU]{1}" + "_GHRSST-SST[a-z]{3,7}-[A-Z1-2_]{3,10}-[DMLT]{2}-v\\d{1,2}\\.\\d{1}-fv\\d{1,2}\\.\\d{1}.nc";
        return "\\d{14}-" + getRdac() + "-L3[CU]{1}_GHRSST-SST((skin)|(subskin)|(depth)|(fnd))[-]" +
               "((ATSR1)|(ATSR2)|(AATSR)|(AMSRE)|(SEVIRI_SST)|(TMI))[-]((LT)|(DM))-" +
               "v\\d{1,2}\\.\\d{1}-fv\\d{1,2}\\.\\d{1}.nc";
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
