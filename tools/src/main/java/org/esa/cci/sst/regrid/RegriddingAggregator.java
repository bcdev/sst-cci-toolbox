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

package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.AbstractAggregator;
import org.esa.cci.sst.common.AggregationContext;
import org.esa.cci.sst.common.LUT;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.TemporalResolution;
import org.esa.cci.sst.common.auxiliary.Climatology;
import org.esa.cci.sst.common.calculator.CoverageUncertaintyProvider;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellAggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.CellGrid;
import org.esa.cci.sst.common.file.FileStore;
import org.esa.cci.sst.common.file.FileType;
import org.esa.cci.sst.util.UTC;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Aggregator for the Regridding Tool.
 * <p/>
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
class RegriddingAggregator extends AbstractAggregator {

    private static final Logger LOGGER = Logger.getLogger("org.esa.cci.sst");

    private final AggregationContext aggregationContext;
    private final LUT timeLut;
    private final LUT spaceLut;

    RegriddingAggregator(FileStore fileStore,
                         Climatology climatology,
                         SstDepth sstDepth, AggregationContext aggregationContext, LUT timeLut,
                         LUT spaceLut) {
        super(fileStore, climatology, sstDepth);
        this.aggregationContext = aggregationContext;
        this.timeLut = timeLut;
        this.spaceLut = spaceLut;
    }

    @Override
    public List<RegriddingTimeStep> aggregate(Date startDate, Date endDate,
                                              TemporalResolution temporalResolution) throws IOException {
        return aggregate(startDate, endDate, temporalResolution, null);
    }

    public List<RegriddingTimeStep> aggregate(Date startDate, Date endDate,
                                              TemporalResolution temporalResolution, Writer writer) throws IOException {
        final List<RegriddingTimeStep> resultGridList = new ArrayList<RegriddingTimeStep>();
        final Calendar calendar = UTC.createCalendar(startDate);
        // TODO - make dailies
        while (calendar.getTime().before(endDate)) {
            final Date date1 = calendar.getTime();
            CellGrid<? extends AggregationCell> resultGrid;
            switch (temporalResolution) {
                case daily: {
                    calendar.add(Calendar.DATE, 1);
                    Date date2 = calendar.getTime();
                    resultGrid = aggregateTimeStep(date1, date2);
                    break;
                }
                case weekly5d: {
                    calendar.add(Calendar.DATE, 5);
                    Date date2 = calendar.getTime();
                    resultGrid = aggregateTimeStep(date1, date2);
                    break;
                }
                case weekly7d: {
                    calendar.add(Calendar.DATE, 7);
                    Date date2 = calendar.getTime();
                    resultGrid = aggregateTimeStep(date1, date2);
                    break;
                }
                case monthly: {
                    calendar.add(Calendar.MONTH, 1);
                    Date date2 = calendar.getTime();
                    resultGrid = aggregateTimeStep(date1, date2);
                    break;
                }
                case seasonal: {
                    calendar.add(Calendar.MONTH, 3);
                    Date date2 = calendar.getTime();
                    // TODO - accumulate monthly time steps immediately, do not keep them in memory all
                    final List<RegriddingTimeStep> monthlyTimeSteps = aggregate(date1, date2,
                                                                                TemporalResolution.monthly);
                    resultGrid = aggregateMultiMonths(monthlyTimeSteps);
                    break;
                }
                case annual: {
                    calendar.add(Calendar.YEAR, 1);
                    Date date2 = calendar.getTime();
                    // TODO - accumulate monthly time steps immediately, do not keep them in memory all
                    final List<RegriddingTimeStep> monthlyTimeSteps = aggregate(date1, date2,
                                                                                TemporalResolution.monthly);
                    resultGrid = aggregateMultiMonths(monthlyTimeSteps);
                    break;
                }
                default:
                    throw new IllegalArgumentException(
                            String.format("Temporal resolution '%s' is not supported.", temporalResolution.toString()));
            }
            if (resultGrid != null) {
                final RegriddingTimeStep timeStep = new RegriddingTimeStep(date1, calendar.getTime(), resultGrid);
                if (writer != null) {
                    try {
                        writer.writeTargetFile(timeStep);
                    } catch (IOException e) {
                        LOGGER.warning(e.getMessage());
                    }
                } else {
                    resultGridList.add(timeStep);
                }
            }
        }
        return resultGridList;
    }

    CellGrid<SpatialAggregationCell> aggregateTimeStep(Date date1, Date date2) throws IOException {
        final List<File> fileList = getFileStore().getFiles(date1, date2);
        if (fileList.isEmpty()) {
            LOGGER.warning(MessageFormat.format("No matching files found in {0} for period {1} - {2}",
                                                Arrays.toString(getFileStore().getInputPaths()),
                                                SimpleDateFormat.getDateInstance().format(date1),
                                                SimpleDateFormat.getDateInstance().format(date2)));
            return null;
        }
        LOGGER.info(String.format("Aggregating output time step from %s to %s, %d file(s) found.",
                                  UTC.getIsoFormat().format(date1), UTC.getIsoFormat().format(date2),
                                  fileList.size()));

        final CellGrid<SpatialAggregationCell> targetGrid = createSpatialAggregationCellGrid();
        final CoverageUncertaintyProvider coverageUncertaintyProvider = createCoverageUncertaintyProvider(date1, date2);
        aggregationContext.setCoverageUncertaintyProvider(coverageUncertaintyProvider);

        for (final File file : fileList) {
            LOGGER.info(String.format("Processing input %s file '%s'", getFileStore().getProductType(), file));
            long t0 = System.currentTimeMillis();
            final NetcdfFile dataFile = NetcdfFile.open(file.getPath());
            try {
                final Date date = getFileType().readDate(dataFile);
                final int dayOfYear = UTC.getDayOfYear(date);
                LOGGER.fine("Day of year is " + dayOfYear);
                aggregationContext.setClimatologySstGrid(getClimatology().getSst(dayOfYear));
                aggregationContext.setSeaCoverageGrid(getClimatology().getSeaCoverage());
                readSourceGrids(dataFile, aggregationContext);

                LOGGER.fine("Aggregating grid(s)...");
                long t01 = System.currentTimeMillis();
                aggregateSourcePixels(aggregationContext, aggregationContext.getTargetRegionMask(), targetGrid);
                LOGGER.fine(String.format("Aggregating grid(s) took %d ms", (System.currentTimeMillis() - t01)));
            } catch (IOException e) {
                LOGGER.warning(e.getMessage());
            } finally {
                dataFile.close();
            }
            LOGGER.fine(String.format("Processing input %s file took %d ms", getFileStore().getProductType(),
                                      System.currentTimeMillis() - t0));
        }

        return targetGrid;
    }

    CellGrid<SpatialAggregationCell> createSpatialAggregationCellGrid() {
        final FileType fileType = getFileType();
        final CellFactory<SpatialAggregationCell> cellFactory = fileType.getSpatialAggregationCellFactory(
                aggregationContext);
        return CellGrid.create(aggregationContext.getTargetGridDef(), cellFactory);
    }

    CellGrid<? extends AggregationCell> aggregateMultiMonths(List<RegriddingTimeStep> monthlyTimeSteps) {
        final FileType fileType = getFileType();
        final CellFactory<CellAggregationCell<AggregationCell>> cellFactory = fileType.getTemporalAggregationCellFactory();
        final CellGrid<CellAggregationCell<AggregationCell>> targetGrid = CellGrid.create(
                aggregationContext.getTargetGridDef(), cellFactory);

        final int h = targetGrid.getHeight();
        final int w = targetGrid.getWidth();
        for (final RegriddingTimeStep timeStep : monthlyTimeSteps) {
            final CellGrid<? extends AggregationCell> sourceGrid = timeStep.getCellGrid();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    final AggregationCell sourceCell = sourceGrid.getCell(x, y);
                    if (sourceCell != null) {
                        CellAggregationCell<AggregationCell> targetCell = targetGrid.getCell(x, y);
                        if (targetCell != null) {
                            targetCell.accumulate(sourceCell, 1.0);
                        } else {
                            targetCell = targetGrid.createCell(x, y);
                            targetCell.accumulate(sourceCell, 1.0);
                            if (!targetCell.isEmpty()) {
                                targetGrid.setCell(targetCell);
                            }
                        }
                    }
                }
            }
        }
        return targetGrid;
    }

    private CoverageUncertaintyProvider createCoverageUncertaintyProvider(Date date1, Date date2) {
        return new RegriddingCoverageUncertaintyProvider(spaceLut, timeLut, date1, date2);
    }
}
