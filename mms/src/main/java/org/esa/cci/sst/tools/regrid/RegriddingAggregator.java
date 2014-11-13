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

import org.esa.cci.sst.aggregate.Aggregation;
import org.esa.cci.sst.aggregate.AggregationContext;
import org.esa.cci.sst.common.*;
import org.esa.cci.sst.common.auxiliary.Climatology;
import org.esa.cci.sst.aggregate.CoverageUncertaintyProvider;
import org.esa.cci.sst.aggregate.AggregationCell;
import org.esa.cci.sst.cell.CellAggregationCell;
import org.esa.cci.sst.cell.CellFactory;
import org.esa.cci.sst.aggregate.SpatialAggregationCell;
import org.esa.cci.sst.cell.CellGrid;
import org.esa.cci.sst.common.file.FileList;
import org.esa.cci.sst.common.file.FileStore;
import org.esa.cci.sst.common.file.FileType;
import org.esa.cci.sst.common.file.ProductType;
import org.esa.cci.sst.grid.Grid;
import org.esa.cci.sst.grid.GridDef;
import org.esa.cci.sst.util.StopWatch;
import org.esa.cci.sst.util.TimeUtil;
import ucar.nc2.NetcdfFile;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Aggregator for the Regridding Tool.
 * <p/>
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
class RegriddingAggregator extends AbstractAggregator {

    private final AggregationContext aggregationContext;
    private final LUT timeLut;
    private final LUT spaceLut;

    RegriddingAggregator(FileStore fileStore,
                         Climatology climatology,
                         SstDepth sstDepth,
                         AggregationContext aggregationContext,
                         LUT timeLut,
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
        final List<RegriddingTimeStep> resultGridList = new ArrayList<>();
        final Calendar calendar = TimeUtil.createUtcCalendar(startDate);

        while (calendar.getTime().before(endDate)) {
            final Date date1 = calendar.getTime();
            final CellGrid<? extends AggregationCell> resultGrid;

            switch (temporalResolution) {
                case daily: {
                    calendar.add(Calendar.DATE, 1);
                    final Date date2 = calendar.getTime();
                    resultGrid = aggregateTimeStep(date1, date2);
                    break;
                }
                case weekly5d: {
                    calendar.add(Calendar.DATE, 5);
                    final Date date2 = calendar.getTime();
                    resultGrid = aggregateTimeStep(date1, date2);
                    break;
                }
                case weekly7d: {
                    calendar.add(Calendar.DATE, 7);
                    final Date date2 = calendar.getTime();
                    resultGrid = aggregateTimeStep(date1, date2);
                    break;
                }
                case monthly: {
                    calendar.add(Calendar.MONTH, 1);
                    final Date date2 = calendar.getTime();
                    resultGrid = aggregateTimeStep(date1, date2);
                    break;
                }
                case seasonal: {
                    resultGrid = aggregateMonths(calendar, 3);
                    break;
                }
                case annual: {
                    resultGrid = aggregateMonths(calendar, 12);
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
                        logger.warning(e.getMessage());
                    }
                } else {
                    resultGridList.add(timeStep);
                }
            }
        }
        return resultGridList;
    }

    private CellGrid<? extends AggregationCell> aggregateMonths(Calendar calendar, int monthCount) throws IOException {
        final CellGrid<? extends AggregationCell> resultGrid;
        final FileType fileType = getFileType();
        final CellFactory<CellAggregationCell<AggregationCell>> cellFactory = fileType.getMultiMonthAggregationCellFactory();

        CellGrid<CellAggregationCell<AggregationCell>> multiMonthGrid = null;
        for (int i = 0; i < monthCount; i++) {
            final Date date1 = calendar.getTime();
            calendar.add(Calendar.MONTH, 1);
            final Date date2 = calendar.getTime();
            final CellGrid<SpatialAggregationCell> singleMonthGrid = aggregateTimeStep(date1, date2);
            if (singleMonthGrid != null) {
                if (multiMonthGrid == null) {
                    multiMonthGrid = CellGrid.create(aggregationContext.getTargetGridDef(), cellFactory);
                }
                aggregateMonth(singleMonthGrid, multiMonthGrid);
            }
        }
        resultGrid = multiMonthGrid;
        return resultGrid;
    }

    CellGrid<SpatialAggregationCell> aggregateTimeStep(Date date1, Date date2) throws IOException {
        final FileStore fileStore = getFileStore();
        final ProductType productType = fileStore.getProductType();
        final List<FileList> allFiles = fileStore.getFiles(date1, date2);

        if (allFiles.isEmpty()) {
            logger.warning(MessageFormat.format("No matching files found in {0} for period {1} - {2}",
                    Arrays.toString(fileStore.getInputPaths()),
                    SimpleDateFormat.getDateInstance().format(date1),
                    SimpleDateFormat.getDateInstance().format(date2)));
            return null;
        }
        logger.info(String.format("Aggregating output time step from %s to %s.",
                TimeUtil.formatIsoUtcFormat(date1), TimeUtil.formatIsoUtcFormat(date2)));

        final Climatology climatology = getClimatology();
        final CoverageUncertaintyProvider coverageUncertaintyProvider = createCoverageUncertaintyProvider(date1, date2);
        aggregationContext.setCoverageUncertaintyProvider(coverageUncertaintyProvider);
        CellGrid<SpatialAggregationCell> targetGrid = null;

        for (final FileList singleDayFiles : allFiles) {
            final int doy = TimeUtil.getYear(singleDayFiles.getDate());
            logger.info("Day of year is " + doy);

            aggregationContext.setClimatologySstGrid(climatology.getSstGrid(doy));
            aggregationContext.setSeaCoverageGrid(climatology.getSeaCoverageGrid());

            final CellGrid<SpatialAggregationCell> singleDayGrid = aggregateSingleDay(productType, singleDayFiles.getFiles());

            if (singleDayGrid != null) {
                aggregationContext.setSstGrid(new CellGridAdapter(singleDayGrid, Aggregation.SST));
                aggregationContext.setRandomUncertaintyGrid(new CellGridAdapter(singleDayGrid, Aggregation.RANDOM_UNCERTAINTY));
                aggregationContext.setLargeScaleUncertaintyGrid(new CellGridAdapter(singleDayGrid, Aggregation.LARGE_SCALE_UNCERTAINTY));
                aggregationContext.setSynopticUncertaintyGrid(new CellGridAdapter(singleDayGrid, Aggregation.SYNOPTIC_UNCERTAINTY));
                aggregationContext.setAdjustmentUncertaintyGrid(new CellGridAdapter(singleDayGrid, Aggregation.ADJUSTMENT_UNCERTAINTY));
                aggregationContext.setAdjustmentUncertaintyGrid(new CellGridAdapter(singleDayGrid, Aggregation.ADJUSTMENT_UNCERTAINTY));
                aggregationContext.setSeaIceFractionGrid(new CellGridAdapter(singleDayGrid, Aggregation.SEA_ICE_FRACTION));
                aggregationContext.setQualityGrid(null);

                if (targetGrid == null) {
                    targetGrid = createSpatialAggregationCellGrid();
                }
                aggregateSourcePixels(aggregationContext, aggregationContext.getTargetRegionMask(), targetGrid);
            }

            aggregationContext.setSstGrid(null);
            aggregationContext.setRandomUncertaintyGrid(null);
            aggregationContext.setLargeScaleUncertaintyGrid(null);
            aggregationContext.setSynopticUncertaintyGrid(null);
            aggregationContext.setAdjustmentUncertaintyGrid(null);
            aggregationContext.setAdjustmentUncertaintyGrid(null);
            aggregationContext.setSeaIceFractionGrid(null);
        }

        return targetGrid;
    }

    private CellGrid<SpatialAggregationCell> aggregateSingleDay(ProductType productType, List<File> files) throws
            IOException {
        final FileType fileType = productType.getFileType();
        final CellFactory<SpatialAggregationCell> dailyCellFactory = fileType.getSingleDayAggregationCellFactory(
                aggregationContext);
        CellGrid<SpatialAggregationCell> targetGrid = null;

        for (final File file : files) {
            logger.info(String.format("Processing input %s file '%s'", productType, file));

            final StopWatch fileWatch = new StopWatch();
            fileWatch.start();

            NetcdfFile datafile = null;
            try {
                datafile = NetcdfFile.open(file.getPath());
                readSourceGrids(datafile, aggregationContext);
                if (targetGrid == null) {
                    targetGrid = CellGrid.create(fileType.getGridDef(), dailyCellFactory);
                }

                logger.fine("Aggregating grid(s)...");
                final StopWatch gridWatch = new StopWatch();
                gridWatch.start();

                aggregateSingleDaySourcePixels(aggregationContext, targetGrid);

                gridWatch.stop();
                logger.fine(String.format("Aggregating grid(s) took %d ms", gridWatch.getElapsedMillis()));
            } catch (IOException e) {
                logger.warning(
                        String.format("Cannot process input %s file '%s' because of an I/O error: '%s'.", productType,
                                file, e.getMessage()));
            } catch (Exception e) {
                if (e.getMessage() == null) {
                    logger.severe(
                            String.format("Cannot process input %s file '%s' because of an unknown error.", productType,
                                    file));
                } else {
                    logger.warning(
                            String.format("Cannot process input %s file '%s' because of an error: '%s'.", productType,
                                    file, e.getMessage()));
                }
            } finally {
                if (datafile != null) {
                    try {
                        datafile.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }

            fileWatch.stop();
            logger.fine(String.format("Processing input %s file took %d ms", productType, fileWatch.getElapsedMillis()));
        }

        return targetGrid;
    }

    private CellGrid<CellAggregationCell<AggregationCell>> aggregateMonth(
            CellGrid<SpatialAggregationCell> singleMonthGrid,
            CellGrid<CellAggregationCell<AggregationCell>> multiMonthGrid) {
        final int h = multiMonthGrid.getHeight();
        final int w = multiMonthGrid.getWidth();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final AggregationCell sourceCell = singleMonthGrid.getCell(x, y);
                if (sourceCell != null) {
                    CellAggregationCell<AggregationCell> targetCell = multiMonthGrid.getCell(x, y);
                    if (targetCell != null) {
                        targetCell.accumulate(sourceCell, 1.0);
                    } else {
                        targetCell = multiMonthGrid.createCell(x, y);
                        targetCell.accumulate(sourceCell, 1.0);
                        if (!targetCell.isEmpty()) {
                            multiMonthGrid.setCell(targetCell);
                        }
                    }
                }
            }
        }
        return multiMonthGrid;
    }

    private static <C extends SpatialAggregationCell> void aggregateSingleDaySourcePixels(AggregationContext context,
                                                                                          CellGrid<C> targetGrid) {
        final GridDef targetGridDef = targetGrid.getGridDef();

        final int w = targetGridDef.getWidth();
        final int h = targetGridDef.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final Rectangle targetRectangle = new Rectangle(x, y, 1, 1);
                C targetCell = targetGrid.getCell(x, y);
                if (targetCell != null) {
                    targetCell.accumulate(context, targetRectangle);
                } else {
                    targetCell = targetGrid.createCell(x, y);
                    targetCell.accumulate(context, targetRectangle);
                    if (!targetCell.isEmpty()) {
                        targetGrid.setCell(targetCell);
                    }
                }
            }
        }
    }


    CellGrid<SpatialAggregationCell> createSpatialAggregationCellGrid() {
        final FileType fileType = getFileType();
        final CellFactory<SpatialAggregationCell> cellFactory = fileType.getSpatialAggregationCellFactory(
                aggregationContext);
        return CellGrid.create(aggregationContext.getTargetGridDef(), cellFactory);
    }

    private CoverageUncertaintyProvider createCoverageUncertaintyProvider(Date date1, Date date2) {
        return new RegriddingCoverageUncertaintyProvider(spaceLut, timeLut, date1, date2);
    }

    private static final class CellGridAdapter implements Grid {

        private final CellGrid<SpatialAggregationCell> cellGrid;
        private int index;

        public CellGridAdapter(CellGrid<SpatialAggregationCell> cellGrid, int index) {
            this.cellGrid = cellGrid;
            this.index = index;
        }

        @Override
        public GridDef getGridDef() {
            return cellGrid.getGridDef();
        }

        private Number getNumber(int x, int y) {
            final SpatialAggregationCell cell = cellGrid.getCell(x, y);
            if (cell == null || cell.isEmpty()) {
                return Double.NaN;
            }
            return cell.getResults()[index];
        }

        @Override
        public final double getSampleDouble(int x, int y) {
            return getNumber(x, y).doubleValue();
        }

        @Override
        public final int getSampleInt(int x, int y) {
            return getNumber(x, y).intValue();
        }

        @Override
        public final boolean getSampleBoolean(int x, int y) {
            return getSampleInt(x, y) != 0;
        }
    }
}
