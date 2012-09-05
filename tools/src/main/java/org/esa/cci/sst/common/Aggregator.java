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

package org.esa.cci.sst.common;

import org.esa.cci.sst.common.auxiliary.Climatology;
import org.esa.cci.sst.common.auxiliary.LUT1;
import org.esa.cci.sst.common.calculator.CoverageUncertaintyProvider;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cell.TemporalAggregationCell;
import org.esa.cci.sst.common.cellgrid.CellGrid;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.RegionMask;
import org.esa.cci.sst.common.file.FileStore;
import org.esa.cci.sst.common.file.FileType;
import org.esa.cci.sst.regavg.*;
import org.esa.cci.sst.regrid.RegriddingTimeStep;
import org.esa.cci.sst.regrid.SpatialResolution;
import org.esa.cci.sst.tool.Tool;
import org.esa.cci.sst.util.UTC;
import ucar.nc2.NetcdfFile;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * The one and only aggregator.
 *
 * @author Norman Fomferra
 */
public class Aggregator {

    public static final GridDef GRID_DEF_GLOBAL_5 = GridDef.createGlobal(5.0);
    public static final GridDef GRID_DEF_GLOBAL_90 = GridDef.createGlobal(90.0);

    private static final Logger LOGGER = Tool.LOGGER;
    private final RegionMaskList regionMaskList;
    private final FileStore fileStore;
    private final FileType fileType;
    private final Climatology climatology;
    private final LUT1 lut1;
    private final LUT2 lut2;

    private final SstDepth sstDepth;
    private RegionMask combinedRegionMask;

    public Aggregator(RegionMaskList regionMaskList,
                      FileStore fileStore,
                      Climatology climatology,
                      LUT1 lut1,
                      LUT2 lut2,
                      SstDepth sstDepth) {
        this.regionMaskList = regionMaskList;
        this.combinedRegionMask = RegionMask.combine(regionMaskList);
        this.fileStore = fileStore;
        this.fileType = fileStore.getProductType().getFileType();
        this.climatology = climatology;
        this.lut1 = lut1;
        this.lut2 = lut2;
        this.sstDepth = sstDepth;
    }

    // Hardly testable, because NetCDF files of given fileType required

    /**
     * @param startDate          Start date of the aggregation time range.
     * @param endDate            End date of the aggregation time range.
     * @param temporalResolution The required temporal resolution.
     * @return A {@link AveragingTimeStep} for each required region.
     * @throws IOException if an I/O error occurs.
     */
    public List<AveragingTimeStep> aggregate(Date startDate, Date endDate,
                                             TemporalResolution temporalResolution) throws IOException {
        final List<AveragingTimeStep> results = new ArrayList<AveragingTimeStep>();
        final Calendar calendar = UTC.createCalendar(startDate);

        while (calendar.getTime().before(endDate)) {
            Date date1 = calendar.getTime();
            List<RegionalAggregation> result;
            if (temporalResolution == TemporalResolution.daily) {
                calendar.add(Calendar.DATE, 1);
                Date date2 = calendar.getTime();
                result = aggregateRegions(date1, date2);
            } else if (temporalResolution == TemporalResolution.monthly) {
                calendar.add(Calendar.MONTH, 1);
                Date date2 = calendar.getTime();
                result = aggregateRegions(date1, date2);
            } else if (temporalResolution == TemporalResolution.seasonal) {
                calendar.add(Calendar.MONTH, 3);
                Date date2 = calendar.getTime();
                List<AveragingTimeStep> monthlyTimeSteps = aggregate(date1, date2, TemporalResolution.monthly);
                result = aggregateMonthlyTimeSteps(monthlyTimeSteps);
            } else /*if (temporalResolution == TemporalResolution.annual)*/ {
                calendar.add(Calendar.YEAR, 1);
                Date date2 = calendar.getTime();
                List<AveragingTimeStep> monthlyTimeSteps = aggregate(date1, date2, TemporalResolution.monthly);
                result = aggregateMonthlyTimeSteps(monthlyTimeSteps);
            }
            results.add(new AveragingTimeStep(date1, calendar.getTime(), result));
        }
        return results;
    }

    // Hardly testable, because NetCDF files of given fileType required
    private List<RegionalAggregation> aggregateRegions(Date date1, Date date2) throws IOException {
        //Compute the cell 5 grid for *all* combined regions first
        //regrid spatially from 0.01 to 5 °, and aggregate given time range (<= monthly)
        final CellGrid<SpatialAggregationCell> combinedCell5Grid = aggregateTimeRange(date1, date2);
        return aggregateRegions(combinedCell5Grid,
                regionMaskList,
                fileType.getSameMonthAggregationFactory(),
                fileType.getCell90Factory(createCoverageUncertaintyProvider(date1, SpatialResolution.DEGREE_5_00)),
                climatology.getSeaCoverageCell5Grid(),
                climatology.getSeaCoverageCell90Grid());
    }

    static List<RegionalAggregation> aggregateRegions(CellGrid<SpatialAggregationCell> combinedCell5Grid,
                                                      RegionMaskList regionMaskList,
                                                      AggregationFactory<SameMonthAggregation> aggregationFactory,
                                                      CellFactory<AggregationCell90> cell90Factory,
                                                      Grid seaCoverageCell5Grid,
                                                      Grid seaCoverageCell90Grid) {
        final List<RegionalAggregation> regionalAggregations = new ArrayList<RegionalAggregation>();
        for (RegionMask regionMask : regionMaskList) {
            // Extract the cell 5 grid for the current region
            CellGrid<SpatialAggregationCell> cell5Grid = getCell5GridForRegion(combinedCell5Grid, regionMask);
            // Check if region is Globe or Hemisphere, if so apply special averaging for all 90 deg grid boxes.
            boolean mustAggregateTo90 = mustAggregateTo90(regionMask);
            SameMonthAggregation aggregation = aggregationFactory.createAggregation();
            if (mustAggregateTo90) {
                CellGrid<AggregationCell90> cell90Grid = aggregateCell5GridToCell90Grid(
                        cell5Grid, seaCoverageCell5Grid, cell90Factory);
                aggregateCell5OrCell90Grid(cell90Grid, seaCoverageCell90Grid, aggregation);
            } else {
                aggregateCell5OrCell90Grid(cell5Grid, seaCoverageCell5Grid, aggregation);
            }
            regionalAggregations.add(aggregation);
        }
        return regionalAggregations;
    }

    // Hardly testable, because NetCDF files of given fileType required
    private CellGrid<SpatialAggregationCell> aggregateTimeRange(Date date1, Date date2) throws IOException {
        final List<File> files = fileStore.getFiles(date1, date2);

        final DateFormat isoDateFormat = UTC.getIsoFormat();
        LOGGER.info(String.format("Computing output time step from %s to %s, %d file(s) found.",
                isoDateFormat.format(date1), isoDateFormat.format(date2), files.size()));
        final CoverageUncertaintyProvider coverageUncertaintyProvider = createCoverageUncertaintyProvider(date1, SpatialResolution.DEGREE_5_00);
        final CellFactory<SpatialAggregationCell> cell5Factory = fileType.getSpatialAggregationCellFactory(coverageUncertaintyProvider);
        final CellGrid<SpatialAggregationCell> cell5Grid = new CellGrid<SpatialAggregationCell>(GRID_DEF_GLOBAL_5, cell5Factory);

        for (File file : files) { //loop time
            LOGGER.info(String.format("Processing input %s file '%s'", fileStore.getProductType(), file));
            long t0 = System.currentTimeMillis();
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            try {
                SpatialAggregationContext aggregationCell5Context = createAggregationCellContext(netcdfFile);
                LOGGER.fine("Aggregating grid(s)...");
                long t01 = System.currentTimeMillis();
                aggregateSources(aggregationCell5Context, combinedRegionMask, cell5Grid);
                LOGGER.fine(String.format("Aggregating grid(s) took %d ms", (System.currentTimeMillis() - t01)));
            } finally {
                netcdfFile.close();
            }
            LOGGER.fine(String.format("Processing input %s file took %d ms", fileStore.getProductType(),
                    System.currentTimeMillis() - t0));
        }

        return cell5Grid;
    }

    static <C extends SpatialAggregationCell> void aggregateSources(SpatialAggregationContext aggregationContext,
                                                                    RegionMask regionMask, CellGrid<C> cellGrid) {
        final GridDef sourceGridDef = aggregationContext.getSourceGridDef();
        final int width = regionMask.getWidth();
        final int height = regionMask.getHeight();
        for (int cellY = 0; cellY < height; cellY++) {
            for (int cellX = 0; cellX < width; cellX++) {
                if (regionMask.getSampleBoolean(cellX, cellY)) {
                    Rectangle2D lonLatRectangle = regionMask.getGridDef().getLonLatRectangle(cellX, cellY);
                    Rectangle sourceGridRectangle = sourceGridDef.getGridRectangle(lonLatRectangle);
                    SpatialAggregationCell cell = cellGrid.getCellSafe(cellX, cellY);
                    cell.accumulate(aggregationContext, sourceGridRectangle);
                }
            }
        }
    }

    // Hardly testable, because NetCDF file of given fileType required
    private SpatialAggregationContext createAggregationCellContext(NetcdfFile netcdfFile) throws IOException {
        final Date date = fileType.readDate(netcdfFile);
        final int dayOfYear = UTC.getDayOfYear(date);
        LOGGER.fine("Day of year is " + dayOfYear);

        return new SpatialAggregationContext(fileStore.getProductType().getGridDef(),
                readSourceGrids(netcdfFile),
                climatology.getAnalysedSstGrid(dayOfYear),
                climatology.getSeaCoverageSourceGrid());
    }

    static <C5 extends SpatialAggregationCell, C90 extends AggregationCell90> CellGrid<C90> aggregateCell5GridToCell90Grid(
            CellGrid<C5> cell5Grid, Grid seaCoverage5Grid, CellFactory<C90> cell90Factory) {

        final CellGrid<C90> cell90Grid = new CellGrid<C90>(GRID_DEF_GLOBAL_90, cell90Factory);
        final int width = cell5Grid.getGridDef().getWidth();
        final int height = cell5Grid.getGridDef().getHeight();
        for (int cell5Y = 0; cell5Y < height; cell5Y++) {
            for (int cell5X = 0; cell5X < width; cell5X++) {
                C5 cell5 = cell5Grid.getCell(cell5X, cell5Y);
                if (cell5 != null && !cell5.isEmpty()) {
                    int cell90X = (cell5X * cell90Grid.getGridDef().getWidth()) / width;
                    int cell90Y = (cell5Y * cell90Grid.getGridDef().getHeight()) / height;
                    C90 cell90 = cell90Grid.getCellSafe(cell90X, cell90Y);
                    double seaCoverage5 = seaCoverage5Grid.getSampleDouble(cell5X, cell5Y);
                    // noinspection unchecked
                    cell90.accumulate(cell5, seaCoverage5);
                }
            }
        }
        return cell90Grid;
    }

    static boolean mustAggregateTo90(RegionMask regionMask) {
        return regionMask.getCoverage() == RegionMask.Coverage.Globe
                || regionMask.getCoverage() == RegionMask.Coverage.N_Hemisphere
                || regionMask.getCoverage() == RegionMask.Coverage.S_Hemisphere;
    }

    private List<RegionalAggregation> aggregateMonthlyTimeSteps(List<AveragingTimeStep> monthlyTimeSteps) {

        return aggregateMonthlyTimeSteps(monthlyTimeSteps,
                regionMaskList.size(), fileType.getMultiMonthAggregationFactory());
    }

    static List<RegionalAggregation> aggregateMonthlyTimeSteps(
            List<AveragingTimeStep> monthlyTimeSteps, int regionCount,
            AggregationFactory<MultiMonthAggregation> aggregationFactory) {

        final MultiMonthAggregation multiMonthAggregation = aggregationFactory.createAggregation();
        final ArrayList<RegionalAggregation> resultList = new ArrayList<RegionalAggregation>();

        for (int regionIndex = 0; regionIndex < regionCount; regionIndex++) {
            for (AveragingTimeStep timeStep : monthlyTimeSteps) {
                RegionalAggregation sameMonthRegionalAggregation = timeStep.getRegionalAggregation(regionIndex);
                // noinspection unchecked
                multiMonthAggregation.accumulate(sameMonthRegionalAggregation);
            }
            resultList.add(multiMonthAggregation);
        }
        return resultList;
    }

    static <C extends AggregationCell> void aggregateCell5OrCell90Grid(CellGrid<C> cellGrid, Grid seaCoverageGrid,
                                                                       SameMonthAggregation aggregation) {
        final int width = cellGrid.getGridDef().getWidth();
        final int height = cellGrid.getGridDef().getHeight();
        for (int cellY = 0; cellY < height; cellY++) {
            for (int cellX = 0; cellX < width; cellX++) {
                C cell = cellGrid.getCell(cellX, cellY);
                if (cell != null && !cell.isEmpty()) {
                    // noinspection unchecked
                    aggregation.accumulate(cell, seaCoverageGrid.getSampleDouble(cellX, cellY));
                }
            }
        }
    }

    static <C extends SpatialAggregationCell> CellGrid<C> getCell5GridForRegion(CellGrid<C> combinedGrid5,
                                                                                RegionMask regionMask) {
        final CellGrid<C> regionalGrid5 = new CellGrid<C>(combinedGrid5.getGridDef(), combinedGrid5.getCellFactory());
        final int width = combinedGrid5.getWidth();
        final int height = combinedGrid5.getHeight();
        for (int cellY = 0; cellY < height; cellY++) {
            for (int cellX = 0; cellX < width; cellX++) {
                if (regionMask.getSampleBoolean(cellX, cellY)) {
                    C cell5 = combinedGrid5.getCell(cellX, cellY);
                    if (cell5 != null && !cell5.isEmpty()) {
                        regionalGrid5.setCell(cellX, cellY, cell5);
                    }
                }
            }
        }
        return regionalGrid5;
    }

    private Grid[] readSourceGrids(NetcdfFile netcdfFile) throws IOException {
        long t0 = System.currentTimeMillis();
        LOGGER.fine("Reading source grid(s)...");
        Grid[] grids = fileType.readSourceGrids(netcdfFile, sstDepth);
        LOGGER.fine(String.format("Reading source grid(s) took %d ms", (System.currentTimeMillis() - t0)));
        return grids;
    }

    private CoverageUncertaintyProvider createCoverageUncertaintyProvider(Date date, SpatialResolution spatialResolution) {
        int month = UTC.createCalendar(date).get(Calendar.MONTH);
        return createCoverageUncertaintyProvider(month, spatialResolution);
    }

    private CoverageUncertaintyProvider createCoverageUncertaintyProvider(int month, SpatialResolution spatialResolution) {

        return new CoverageUncertaintyProvider(month, spatialResolution) {
            @Override
            protected double getMagnitude5(int cellX, int cellY) {
                return lut1.getMagnitudeGrid5().getSampleDouble(cellX, cellY);
            }

            @Override
            protected double getExponent5(int cellX, int cellY) {
                return lut1.getExponentGrid5().getSampleDouble(cellX, cellY);
            }

            @Override
            protected double getMagnitude90(int cellX, int cellY, int month) {
                return lut2.getMagnitude90(month, cellX, cellY);
            }
        };
    }

    /*
     * NEW!!
     */

    /**
     * Aggregates into demanded spatial resolution. Is in fact regridding.
     *
     * @param startDate          Start date of the considered data.
     * @param endDate            End date of the considered data.
     * @param temporalResolution The required temporal resolution, e.g. daily, monthly, seasonal.
     * @param spatialResolution  The required spatial resolution, e.g. 10.0, 0.5 ...
     * @return A list of {@link RegriddingTimeStep}
     * @throws IOException if an I/O error occurs.
     */
    public List<RegriddingTimeStep> aggregate(Date startDate, Date endDate,
                                              TemporalResolution temporalResolution,
                                              SpatialResolution spatialResolution) throws IOException {
        final List<RegriddingTimeStep> resultList = new ArrayList<RegriddingTimeStep>();
        final Calendar calendar = UTC.createCalendar(startDate);

        while (calendar.getTime().before(endDate)) {
            Date date1 = calendar.getTime();
            CellGrid<? extends AggregationCell> result;
            if (temporalResolution == TemporalResolution.daily) {
                calendar.add(Calendar.DATE, 1);
                Date date2 = calendar.getTime();
                result = aggregateTimeRange(date1, date2, spatialResolution);
            } else if (temporalResolution == TemporalResolution.monthly) {
                calendar.add(Calendar.MONTH, 1);
                Date date2 = calendar.getTime();
                result = aggregateTimeRange(date1, date2, spatialResolution);
            } else if (temporalResolution == TemporalResolution.seasonal) {
                calendar.add(Calendar.MONTH, 3);
                Date date2 = calendar.getTime();
                List<RegriddingTimeStep> monthlyTimeSteps = aggregate(date1, date2, TemporalResolution.monthly, spatialResolution);
                result = aggregateMultiMonths(monthlyTimeSteps);
            } else /*if (temporalResolution == TemporalResolution.annual)*/ {
                calendar.add(Calendar.YEAR, 1);
                Date date2 = calendar.getTime();
                List<RegriddingTimeStep> monthlyTimeSteps = aggregate(date1, date2, TemporalResolution.monthly,
                        spatialResolution);
                result = aggregateMultiMonths(monthlyTimeSteps);
            }
            if (result != null) {
                resultList.add(new RegriddingTimeStep(date1, calendar.getTime(), result));
            }
        }
        return resultList;
    }

    private CellGrid<SpatialAggregationCell> aggregateTimeRange(Date date1, Date date2,
                                                                SpatialResolution spatialResolution) throws IOException {
        //todo bs: optionally do something with regionMask
        final List<File> fileList = fileStore.getFiles(date1, date2);
        if (fileList.isEmpty()) {
            return null;
        }

        final DateFormat isoDateFormat = UTC.getIsoFormat();
        LOGGER.info(String.format("Computing output time step from %s to %s, %d file(s) found.",
                isoDateFormat.format(date1), isoDateFormat.format(date2), fileList.size()));

        final CoverageUncertaintyProvider coverageUncertaintyProvider = createCoverageUncertaintyProvider(date1, spatialResolution);
        final CellFactory<SpatialAggregationCell> cellFactory = fileType.getSpatialAggregationCellFactory(coverageUncertaintyProvider);
        GridDef global = GridDef.createGlobal(spatialResolution.getValue());
        final CellGrid<SpatialAggregationCell> cellGrid = new CellGrid<SpatialAggregationCell>(global, cellFactory);

        for (File file : fileList) { //loop time: implicitly 1 file is 1 day
            LOGGER.info(String.format("Processing input %s file '%s'", fileStore.getProductType(), file));
            long t0 = System.currentTimeMillis();
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            try {
                SpatialAggregationContext aggregationCellContext = createAggregationCellContext(netcdfFile);
                LOGGER.fine("Aggregating grid(s)...");
                long t01 = System.currentTimeMillis();
                aggregateSources(aggregationCellContext, combinedRegionMask, cellGrid);
                LOGGER.fine(String.format("Aggregating grid(s) took %d ms", (System.currentTimeMillis() - t01)));
            } finally {
                netcdfFile.close();
            }
            LOGGER.fine(String.format("Processing input %s file took %d ms", fileStore.getProductType(),
                    System.currentTimeMillis() - t0));
        }

        return cellGrid;
    }

    private CellGrid<AggregationCell> aggregateMultiMonths(List<RegriddingTimeStep> monthlyTimeSteps) {

        final CellFactory<AggregationCell> cellFactory = fileType.getCellFactory();
        GridDef gridDef = monthlyTimeSteps.get(0).getCellGrid().getGridDef();
        final CellGrid<AggregationCell> cellGrid = new CellGrid<AggregationCell>(gridDef, cellFactory);

        for (RegriddingTimeStep regriddingTimeStep : monthlyTimeSteps) {
            int height = cellGrid.getHeight();
            int width = cellGrid.getWidth();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    TemporalAggregationCell cell = (TemporalAggregationCell) cellGrid.getCellSafe(x, y);
                    AggregationCell cellFromTimeStep = regriddingTimeStep.getCellGrid().getCell(x, y);
                    Number[] results = cellFromTimeStep.getResults();
                    cell.accumulate(results, 1);
                }
            }
        }
        return cellGrid;
    }
}
