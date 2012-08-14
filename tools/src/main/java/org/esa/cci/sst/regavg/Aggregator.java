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

package org.esa.cci.sst.regavg;

import org.esa.cci.sst.regrid.RegriddingTimeStep;
import org.esa.cci.sst.regrid.SpatialResolution;
import org.esa.cci.sst.tool.Tool;
import org.esa.cci.sst.util.*;
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

    public static class AveragingTimeStep implements TimeStep {

        private final Date startDate;
        private final Date endDate;
        private final List<RegionalAggregation> regionalAggregations;

        public AveragingTimeStep(Date startDate, Date endDate, List<RegionalAggregation> regionalAggregations) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.regionalAggregations = regionalAggregations;
        }

        @Override
        public Date getStartDate() {
            return startDate;
        }

        @Override
        public Date getEndDate() {
            return endDate;
        }

        public int getRegionCount() {
            return regionalAggregations.size();
        }

        public Number[] getRegionalAggregationResults(int regionIndex) {
            RegionalAggregation regionalAggregation = regionalAggregations.get(regionIndex);
            return regionalAggregation.getResults();
        }
    }

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
     * @return A {@link org.esa.cci.sst.regavg.Aggregator.AveragingTimeStep} for each required region.
     * @throws IOException if an I/O error occurs.
     */
    public List<AveragingTimeStep> aggregate(Date startDate,
                                             Date endDate,
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
                List<AveragingTimeStep> sameMonthTimeSteps = aggregate(date1, date2, TemporalResolution.monthly);
                result = aggregateSameMonthAggregationsToMultiMonthAggregation(sameMonthTimeSteps);
            } else /*if (temporalResolution == TemporalResolution.annual)*/ {
                calendar.add(Calendar.YEAR, 1);
                Date date2 = calendar.getTime();
                List<AveragingTimeStep> sameMonthTimeSteps = aggregate(date1, date2, TemporalResolution.monthly);
                result = aggregateSameMonthAggregationsToMultiMonthAggregation(sameMonthTimeSteps);
            }
            results.add(new AveragingTimeStep(date1, calendar.getTime(), result));
        }
        return results;
    }

    // Hardly testable, because NetCDF files of given fileType required
    private List<RegionalAggregation> aggregateRegions(Date date1, Date date2) throws IOException {
        // Compute the cell 5 grid for *all* combined regions first
        final CellGrid<AggregationCell5> combinedCell5Grid = aggregateTimeRange(date1, date2);
        return aggregateRegions(combinedCell5Grid,
                regionMaskList,
                fileType.getSameMonthAggregationFactory(),
                fileType.getCell90Factory(createCoverageUncertaintyProvider(date1)),
                climatology.getSeaCoverageCell5Grid(),
                climatology.getSeaCoverageCell90Grid());
    }

    static List<RegionalAggregation> aggregateRegions(CellGrid<AggregationCell5> combinedCell5Grid,
                                                      RegionMaskList regionMaskList,
                                                      AggregationFactory<SameMonthAggregation> aggregationFactory,
                                                      CellFactory<AggregationCell90> cell90Factory,
                                                      Grid seaCoverageCell5Grid,
                                                      Grid seaCoverageCell90Grid) {
        final List<RegionalAggregation> regionalAggregations = new ArrayList<RegionalAggregation>();
        for (RegionMask regionMask : regionMaskList) {
            // Extract the cell 5 grid for the current region
            CellGrid<AggregationCell5> cell5Grid = getCell5GridForRegion(combinedCell5Grid, regionMask);
            // Check if region is Globe or Hemisphere, if so apply special averaging for all 90 deg grid boxes.
            boolean mustAggregateTo90 = mustAggregateTo90(regionMask);
            SameMonthAggregation aggregation = aggregationFactory.createAggregation();
            if (mustAggregateTo90) {
                CellGrid<AggregationCell90> cell90Grid = aggregateCell5GridToCell90Grid(cell5Grid, seaCoverageCell5Grid,
                        cell90Factory);
                aggregateCell5OrCell90Grid(cell90Grid, seaCoverageCell90Grid, aggregation);
            } else {
                aggregateCell5OrCell90Grid(cell5Grid, seaCoverageCell5Grid, aggregation);
            }
            regionalAggregations.add(aggregation);
        }
        return regionalAggregations;
    }

    // Hardly testable, because NetCDF files of given fileType required
    private CellGrid<AggregationCell5> aggregateTimeRange(Date date1, Date date2) throws IOException {
        final List<File> files = fileStore.getFiles(date1, date2);

        final DateFormat isoDateFormat = UTC.getIsoFormat();
        LOGGER.info(String.format("Computing output time step from %s to %s, %d file(s) found.",
                isoDateFormat.format(date1), isoDateFormat.format(date2), files.size()));
        final CoverageUncertaintyProvider coverageUncertaintyProvider = createCoverageUncertaintyProvider(date1);
        final CellFactory<AggregationCell5> cell5Factory = fileType.getCell5Factory(coverageUncertaintyProvider);
        final CellGrid<AggregationCell5> cell5Grid = new CellGrid<AggregationCell5>(GRID_DEF_GLOBAL_5, cell5Factory);

        for (File file : files) { //loop time
            LOGGER.info(String.format("Processing input %s file '%s'", fileStore.getProductType(), file));
            long t0 = System.currentTimeMillis();
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            try {
                AggregationCell5Context aggregationCell5Context = createAggregationCell5Context(netcdfFile);
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

    static <C extends AggregationCell5> void aggregateSources(AggregationCell5Context aggregationCell5Context,
                                                              RegionMask regionMask, CellGrid<C> cell5Grid) {
        final GridDef sourceGridDef = aggregationCell5Context.getSourceGridDef();
        final int width = regionMask.getWidth();
        final int height = regionMask.getHeight();
        for (int cellY = 0; cellY < height; cellY++) {
            for (int cellX = 0; cellX < width; cellX++) {
                if (regionMask.getSampleBoolean(cellX, cellY)) {
                    Rectangle2D lonLatRectangle = regionMask.getGridDef().getLonLatRectangle(cellX, cellY);
                    Rectangle sourceGridRectangle = sourceGridDef.getGridRectangle(lonLatRectangle);
                    C cell5 = cell5Grid.getCellSafe(cellX, cellY);
                    cell5.accumulate(aggregationCell5Context, sourceGridRectangle);
                }
            }
        }
    }

    // Hardly testable, because NetCDF file of given fileType required
    private AggregationCell5Context createAggregationCell5Context(NetcdfFile netcdfFile) throws IOException {
        final Date date = fileType.readDate(netcdfFile);
        final int dayOfYear = UTC.getDayOfYear(date);
        LOGGER.fine("Day of year is " + dayOfYear);
        return new AggregationCell5Context(fileStore.getProductType().getGridDef(),
                readSourceGrids(netcdfFile),
                climatology.getAnalysedSstGrid(dayOfYear),
                climatology.getSeaCoverageSourceGrid());
    }

    static <C5 extends AggregationCell5, C90 extends AggregationCell90> CellGrid<C90> aggregateCell5GridToCell90Grid(
            CellGrid<C5> cell5Grid,
            Grid seaCoverage5Grid,
            CellFactory<C90> cell90Factory) {
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

    private List<RegionalAggregation> aggregateSameMonthAggregationsToMultiMonthAggregation(List<AveragingTimeStep> sameMonthTimeSteps) {
        return aggregateSameMonthAggregationsToMultiMonthAggregation(sameMonthTimeSteps,
                regionMaskList.size(),
                fileType.getMultiMonthAggregationFactory());
    }

    static List<RegionalAggregation> aggregateSameMonthAggregationsToMultiMonthAggregation(
            List<AveragingTimeStep> sameMonthTimeSteps, int regionCount,
            AggregationFactory<MultiMonthAggregation> a4CellFactory) {
        final MultiMonthAggregation multiMonthCombinedAggregation = a4CellFactory.createAggregation();
        final ArrayList<RegionalAggregation> multiMonthRegionalAggregations = new ArrayList<RegionalAggregation>();
        for (int regionIndex = 0; regionIndex < regionCount; regionIndex++) {
            for (AveragingTimeStep timeStep : sameMonthTimeSteps) {
                RegionalAggregation sameMonthRegionalAggregation = timeStep.regionalAggregations.get(regionIndex);
                // noinspection unchecked
                multiMonthCombinedAggregation.accumulate(sameMonthRegionalAggregation);
            }
            multiMonthRegionalAggregations.add(multiMonthCombinedAggregation);
        }
        return multiMonthRegionalAggregations;
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

    static <C extends AggregationCell5> CellGrid<C> getCell5GridForRegion(CellGrid<C> combinedGrid5,
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

    private CoverageUncertaintyProvider createCoverageUncertaintyProvider(Date date) {
        int month = UTC.createCalendar(date).get(Calendar.MONTH);
        return createCoverageUncertaintyProvider(month);
    }

    private CoverageUncertaintyProvider createCoverageUncertaintyProvider(int month) {
        return new CoverageUncertaintyProvider(month) {
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
     * @param startDate          Start date of the aggregation time range.
     * @param endDate            End date of the aggregation time range.
     * @param temporalResolution The required temporal resolution.
     * @param spatialResolution  The required spatial resolution.
     * @return A {@link org.esa.cci.sst.regavg.Aggregator.AveragingTimeStep} for each required region.
     * @throws IOException if an I/O error occurs.
     */
    public List<RegriddingTimeStep> aggregate(Date startDate, Date endDate,
                                              TemporalResolution temporalResolution,
                                              SpatialResolution spatialResolution) throws IOException {
        final List<RegriddingTimeStep> resultList = new ArrayList<RegriddingTimeStep>();
        final Calendar calendar = UTC.createCalendar(startDate);

        while (calendar.getTime().before(endDate)) {
            Date date1 = calendar.getTime();
            CellGrid<AggregationCell5> result;
            if (temporalResolution == TemporalResolution.daily) {
                calendar.add(Calendar.DATE, 1);
                Date date2 = calendar.getTime();
                result = aggregateRegion(date1, date2, spatialResolution);
            } else if (temporalResolution == TemporalResolution.monthly) {
                calendar.add(Calendar.MONTH, 1);
                Date date2 = calendar.getTime();
                result = aggregateRegion(date1, date2, spatialResolution);
            } else if (temporalResolution == TemporalResolution.seasonal) {
                calendar.add(Calendar.MONTH, 3);
                Date date2 = calendar.getTime();
                List<RegriddingTimeStep> sameMonthTimeSteps = aggregate(date1, date2, TemporalResolution.monthly,
                        spatialResolution);
                result = aggregateSameMonthAggregationsToMultiMonthAggregation_returnCellGrid(sameMonthTimeSteps);
            } else /*if (temporalResolution == TemporalResolution.annual)*/ {
                calendar.add(Calendar.YEAR, 1);
                Date date2 = calendar.getTime();
                List<RegriddingTimeStep> sameMonthTimeSteps = aggregate(date1, date2, TemporalResolution.monthly,
                        spatialResolution);
                result = aggregateSameMonthAggregationsToMultiMonthAggregation_returnCellGrid(sameMonthTimeSteps);
            }
            resultList.add(new RegriddingTimeStep(date1, calendar.getTime(), result));
        }
        return resultList;
    }

    private CellGrid<AggregationCell5> aggregateRegion(Date date1, Date date2,
                                                       SpatialResolution spatialResolution) throws IOException {
        return aggregateTimeRange(date1, date2, spatialResolution);
    }

    private CellGrid<AggregationCell5> aggregateTimeRange(Date date1, Date date2,
                                                          SpatialResolution spatialResolution) throws IOException {

        final List<File> files = fileStore.getFiles(date1, date2);

        final DateFormat isoDateFormat = UTC.getIsoFormat();
        LOGGER.info(String.format("Computing output time step from %s to %s, %d file(s) found.",
                isoDateFormat.format(date1), isoDateFormat.format(date2), files.size()));
        final CoverageUncertaintyProvider coverageUncertaintyProvider = createCoverageUncertaintyProvider(date1);

        // TODO - use spatial resolution for cell grid
        final CellFactory<AggregationCell5> cell5Factory = fileType.getCell5Factory(coverageUncertaintyProvider);
        final CellGrid<AggregationCell5> cell5Grid = new CellGrid<AggregationCell5>(GRID_DEF_GLOBAL_5, cell5Factory);

        for (File file : files) { //loop time
            LOGGER.info(String.format("Processing input %s file '%s'", fileStore.getProductType(), file));
            long t0 = System.currentTimeMillis();
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            try {
                AggregationCell5Context aggregationCell5Context = createAggregationCell5Context(netcdfFile);
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

    private CellGrid<AggregationCell5> aggregateSameMonthAggregationsToMultiMonthAggregation_returnCellGrid(List<RegriddingTimeStep> sameMonthTimeSteps) {
        return null;  //todo implement it!
    }


}
