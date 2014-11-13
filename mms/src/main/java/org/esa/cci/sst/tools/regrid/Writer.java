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

package org.esa.cci.sst.tools.regrid;

import org.esa.cci.sst.common.*;
import org.esa.cci.sst.common.calculator.NumberAccumulator;
import org.esa.cci.sst.common.calculator.UncertaintyAccumulator;
import org.esa.cci.sst.common.AggregationCell;
import org.esa.cci.sst.common.CellGrid;
import org.esa.cci.sst.common.RegionMask;
import org.esa.cci.sst.common.file.FileType;
import org.esa.cci.sst.common.file.ProductType;
import org.esa.cci.sst.log.SstLogging;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * For writing target files.
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
final class Writer {

    private static final Logger logger = SstLogging.getLogger();

    private final ProductType productType;
    private final String toolName;
    private final String toolVersion;
    private final String fileFormatVersion;
    private final boolean totalUncertaintyWanted;
    private final double maxTotalUncertainty;
    private final File targetDir;
    private final String filenameRegex;
    private final SstDepth sstDepth;
    private final TemporalResolution temporalResolution;
    private final RegionMask regionMask;

    Writer(ProductType productType,
           String toolName,
           String toolVersion,
           String fileFormatVersion,
           boolean totalUncertaintyWanted,
           double maxTotalUncertainty,
           File targetDir,
           String filenameRegex,
           SstDepth sstDepth,
           TemporalResolution temporalResolution,
           RegionMask regionMask) {
        this.productType = productType;
        this.toolName = toolName;
        this.toolVersion = toolVersion;
        this.fileFormatVersion = fileFormatVersion;
        this.totalUncertaintyWanted = totalUncertaintyWanted;
        this.maxTotalUncertainty = maxTotalUncertainty;
        this.targetDir = targetDir;
        this.filenameRegex = filenameRegex;
        this.sstDepth = sstDepth;
        this.temporalResolution = temporalResolution;
        this.regionMask = regionMask;
    }

    void writeTargetFile(RegriddingTimeStep timeStep) throws IOException {
        final CellGrid<? extends AggregationCell> targetCellGrid = timeStep.getCellGrid();
        final GridDef targetGridDef = targetCellGrid.getGridDef();
        final Date startDate = timeStep.getStartDate();
        final Date endDate = timeStep.getEndDate();
        final ProcessingLevel processingLevel = productType.getProcessingLevel();
        final String sstType;
        if (processingLevel != ProcessingLevel.L4) {
            sstType = "SST" + sstDepth;
        } else {
            sstType = "SSTxxx";
        }
        final String targetFilename = getTargetFilename(TimeUtil.formatInsituFilenameFormat(startDate),
                TimeUtil.formatInsituFilenameFormat(endDate),
                processingLevel,
                sstType,
                "ppp",
                "ss",
                regionMask.getName().toUpperCase(),
                "REGRIDDED_" + targetGridDef.getResolution());
        final File targetFile = new File(targetDir, targetFilename);
        logger.info("Writing target file '" + targetFile + "'...");

        final int rowCount = targetGridDef.getHeight();
        final int colCount = targetGridDef.getWidth();

        final NetcdfFileWriteable dataFile = NetcdfFileWriteable.createNew(targetFile.getPath());
        try {
            // define global attributes
            dataFile.addGlobalAttribute("title", String.format("Re-gridded %s SST", productType.toString()));
            dataFile.addGlobalAttribute("institution", "IAES, University of Edinburgh");
            dataFile.addGlobalAttribute("contact", "c.merchant@ed.ac.uk");
            dataFile.addGlobalAttribute("fileFormatVersion", fileFormatVersion);
            dataFile.addGlobalAttribute("toolName", toolName);
            dataFile.addGlobalAttribute("toolVersion", toolVersion);
            dataFile.addGlobalAttribute("generated_at", TimeUtil.formatIsoUtcFormat(new Date()));
            dataFile.addGlobalAttribute("product_type", productType.toString());
            dataFile.addGlobalAttribute("sst_depth", sstDepth.toString());
            dataFile.addGlobalAttribute("start_date", TimeUtil.formatIsoUtcFormat(startDate));
            dataFile.addGlobalAttribute("end_date", TimeUtil.formatIsoUtcFormat(endDate));
            dataFile.addGlobalAttribute("temporal_resolution", temporalResolution.toString());
            dataFile.addGlobalAttribute("geospatial_lon_resolution", targetGridDef.getResolutionX());
            dataFile.addGlobalAttribute("geospatial_lat_resolution", targetGridDef.getResolutionY());
            dataFile.addGlobalAttribute("region_name", regionMask.getName());
            dataFile.addGlobalAttribute("source_filename_regex", filenameRegex);

            // define global dims
            final Dimension latDim = dataFile.addDimension("lat", rowCount);
            final Dimension lonDim = dataFile.addDimension("lon", colCount);
            final Dimension timeDim = dataFile.addDimension("time", targetGridDef.getTime(), true, false, false);
            final Dimension boundsDim = dataFile.addDimension("bnds", 2);
            final Dimension[] dims = {timeDim, latDim, lonDim};

            // add variables
            final Variable lat = dataFile.addVariable("lat", DataType.FLOAT, new Dimension[]{latDim});
            lat.addAttribute(new Attribute("units", "degrees_north"));
            lat.addAttribute(new Attribute("long_name", "latitude"));
            lat.addAttribute(new Attribute("bounds", "lat_bnds"));
            final Variable lon = dataFile.addVariable("lon", DataType.FLOAT, new Dimension[]{lonDim});
            lon.addAttribute(new Attribute("units", "degrees_east"));
            lon.addAttribute(new Attribute("long_name", "longitude"));
            lon.addAttribute(new Attribute("bounds", "lon_bnds"));
            final Variable latBounds = dataFile.addVariable("lat_bnds", DataType.FLOAT,
                    new Dimension[]{latDim, boundsDim});
            latBounds.addAttribute(new Attribute("units", "degrees_north"));
            latBounds.addAttribute(new Attribute("long_name", "latitude cell boundaries"));
            final Variable lonBounds = dataFile.addVariable("lon_bnds", DataType.FLOAT,
                    new Dimension[]{lonDim, boundsDim});
            lonBounds.addAttribute(new Attribute("units", "degrees_east"));
            lonBounds.addAttribute(new Attribute("long_name", "longitude cell boundaries"));
            final Variable[] resultVariables = addResultVariables(dataFile, sstDepth, dims);
            // define file structure
            dataFile.create();

            // write data of coordinate variables
            final Array latArray = Array.factory(WriterHelper.createLatData(targetGridDef));
            writeData(dataFile, "lat", latArray);
            final Array lonArray = Array.factory(WriterHelper.createLonData(targetGridDef));
            writeData(dataFile, "lon", lonArray);
            final Array latBoundsArray = Array.factory(WriterHelper.createLatBoundsData(targetGridDef));
            writeData(dataFile, "lat_bnds", latBoundsArray);
            final Array lonBoundsArray = Array.factory(WriterHelper.createLonBoundsData(targetGridDef));
            writeData(dataFile, "lon_bnds", lonBoundsArray);

            // write data of result variables
            if (totalUncertaintyWanted) {
                writeTotalUncertaintyData(dataFile, resultVariables[0], targetCellGrid);
            } else {
                writeResultVariables(dataFile, resultVariables, targetCellGrid);
            }
        } catch (IOException e) {
            throw new IOException(MessageFormat.format("An exception occurred while writing target file ''{0}'':{1}",
                    dataFile.getLocation(),
                    e.getMessage()), e);
        } finally {
            try {
                dataFile.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private Variable[] addResultVariables(NetcdfFileWriteable dataFile,
                                          SstDepth sstDepth,
                                          Dimension[] dims) {
        final FileType fileType = productType.getFileType();
        final Variable[] resultVariables;

        if (totalUncertaintyWanted) {
            final Variable totalUncertainty = dataFile.addVariable("total_uncertainty", DataType.FLOAT, dims);
            totalUncertainty.addAttribute(new Attribute("units", "kelvin"));
            totalUncertainty.addAttribute(new Attribute("long_name", "the total uncertainty"));
            totalUncertainty.addAttribute(new Attribute("_FillValue", Float.NaN));

            resultVariables = new Variable[]{totalUncertainty};
        } else {
            resultVariables = fileType.addResultVariables(dataFile, dims, sstDepth);
        }

        return resultVariables;
    }

    private void writeResultVariables(NetcdfFileWriteable dataFile,
                                      Variable[] resultVariables,
                                      CellGrid<? extends AggregationCell> resultCellGrid) throws IOException {
        final int colCount = resultCellGrid.getWidth();
        final int rowCount = resultCellGrid.getHeight();

        for (int v = 0; v < resultVariables.length; v++) {
            final Variable variable = resultVariables[v];
            if (variable != null) {
                final Array array = Array.factory(variable.getDataType(), variable.getShape());
                for (int y = 0; y < rowCount; y++) {
                    for (int x = 0; x < colCount; x++) {
                        final int index = y * colCount + x;
                        array.setDouble(index, Double.NaN);

                        final AggregationCell cell = resultCellGrid.getCell(x, y);
                        if (cell != null) {
                            final Number[] results = cell.getResults();
                            final double totalUncertainty = calculateTotalUncertainty(results);
                            if (maxTotalUncertainty <= 0.0 || totalUncertainty <= maxTotalUncertainty) {
                                array.setDouble(index, results[v].doubleValue());
                            }
                        }
                    }
                }
                writeData(dataFile, variable.getFullNameEscaped(), array);
            }
        }
    }

    private void writeTotalUncertaintyData(NetcdfFileWriteable dataFile,
                                           Variable totalUncertaintyVariable,
                                           CellGrid<? extends AggregationCell> cellGrid) throws IOException {
        final int colCount = cellGrid.getWidth();
        final int rowCount = cellGrid.getHeight();
        final Array array = Array.factory(totalUncertaintyVariable.getDataType(), totalUncertaintyVariable.getShape());

        for (int y = 0; y < rowCount; y++) {
            for (int x = 0; x < colCount; x++) {
                final AggregationCell cell = cellGrid.getCell(x, y);
                final int index = y * colCount + x;
                if (cell != null) {
                    final Number[] results = cell.getResults();
                    final double totalUncertainty = calculateTotalUncertainty(results);
                    if (maxTotalUncertainty <= 0.0 || totalUncertainty <= maxTotalUncertainty) {
                        array.setDouble(index, totalUncertainty);
                    } else {
                        array.setDouble(index, Double.NaN);
                    }
                } else {
                    array.setDouble(index, Double.NaN);
                }
            }
        }
        writeData(dataFile, totalUncertaintyVariable.getFullNameEscaped(), array);
    }


    static double calculateTotalUncertainty(Number[] results) {
        final NumberAccumulator uncertaintyAccumulator = new UncertaintyAccumulator();

        uncertaintyAccumulator.accumulate(results[Aggregation.RANDOM_UNCERTAINTY].doubleValue());
        uncertaintyAccumulator.accumulate(results[Aggregation.ADJUSTMENT_UNCERTAINTY].doubleValue());
        uncertaintyAccumulator.accumulate(results[Aggregation.COVERAGE_UNCERTAINTY].doubleValue());
        uncertaintyAccumulator.accumulate(results[Aggregation.LARGE_SCALE_UNCERTAINTY].doubleValue());
        uncertaintyAccumulator.accumulate(results[Aggregation.SYNOPTIC_UNCERTAINTY].doubleValue());

        return uncertaintyAccumulator.combine();
    }

    private void writeData(NetcdfFileWriteable dataFile, String variable, Array array) throws IOException {
        try {
            dataFile.write(variable, array);
        } catch (InvalidRangeException cannotHappen) {
            logger.throwing(getClass().getName(), "writeData", cannotHappen);
        }
    }

    /**
     * Generates a filename of the form
     * <code>
     * <i>startOfPeriod</i><b>-</b><i>endOfPeriod</i><b>-</b><i>regionName</i><b>_regridding-ESACCI-</b><i>processingLevel</i><b>_GHRSST-</b><i>sstType</i><b>-</b><i>productString</i><b>-</b><i>additionalSegregator</i><b>-v02.0-fv</b><i>fileVersion</i><b>.nc</b>
     * </code>
     *
     * @param startOfPeriod        Start of period = YYYYMMDD
     * @param endOfPeriod          End of period = YYYYMMDD
     * @param processingLevel      Processing level = L3C, L3U or L4
     * @param sstType              SST type
     * @param productString        Product string (see Table 5 in PSD, e.g. AATSR, OSTIA)
     * @param additionalSegregator Additional segregator = LT or DM
     * @return The filename.
     */
    String getTargetFilename(String startOfPeriod,
                             String endOfPeriod,
                             ProcessingLevel processingLevel,
                             String sstType,
                             String productString,
                             String additionalSegregator,
                             String region,
                             String resolution) {

        final String rdac = productType.getFileType().getRdac();
        return String.format("%s-%s-%s-%s_GHRSST-%s-%s-%s-v%s-fv%s-%s-%s.nc",
                startOfPeriod,
                endOfPeriod,
                rdac,
                processingLevel,
                sstType,
                productString,
                additionalSegregator,
                toolVersion,
                fileFormatVersion,
                region,
                resolution);
    }
}
