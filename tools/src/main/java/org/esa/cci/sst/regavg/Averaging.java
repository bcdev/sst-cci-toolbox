package org.esa.cci.sst.regavg;

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
 * Utility that performs the regional averaging.
 *
 * @author Norman Fomferra
 */
public class Averaging {

    private static final GridDef GRID_DEF_GLOBAL_5 = GridDef.createGlobal(5.0);
    private static final GridDef GRID_DEF_GLOBAL_90 = GridDef.createGlobal(90.0);

    private static final Logger LOGGER = Tool.LOGGER;
    private final FileStore fileStore;
    private final FileType fileType;
    private final Climatology climatology;
    private final LUT1 lut1;
    private final LUT2 lut2;

    private final SstDepth sstDepth;

    public static class OutputTimeStep {
        final Date date1;
        final Date date2;
        final List<CombinedAggregation> regionalAverages;

        public OutputTimeStep(Date date1, Date date2, List<CombinedAggregation> regionalAverages) {
            this.date1 = date1;
            this.date2 = date2;
            this.regionalAverages = regionalAverages;
        }
    }

    public Averaging(FileStore fileStore,
                     Climatology climatology,
                     LUT1 lut1,
                     LUT2 lut2,
                     SstDepth sstDepth) {
        this.fileStore = fileStore;
        this.fileType = fileStore.getProductType().getFileType();
        this.climatology = climatology;
        this.lut1 = lut1;
        this.lut2 = lut2;
        this.sstDepth = sstDepth;
    }

    public List<OutputTimeStep> aggregateTimeRanges(Date startDate,
                                                    Date endDate,
                                                    TemporalResolution temporalResolution,
                                                    RegionMaskList regionMaskList)
            throws IOException {

        List<OutputTimeStep> results = new ArrayList<OutputTimeStep>();
        Calendar calendar = UTC.createCalendar(startDate);
        while (calendar.getTime().before(endDate)) {
            Date date1 = calendar.getTime();
            List<CombinedAggregation> result;
            if (temporalResolution == TemporalResolution.daily) {
                calendar.add(Calendar.DATE, 1);
                Date date2 = calendar.getTime();
                result = aggregateTimeRange(date1, date2, regionMaskList);
            } else if (temporalResolution == TemporalResolution.monthly) {
                calendar.add(Calendar.MONTH, 1);
                Date date2 = calendar.getTime();
                result = aggregateTimeRange(date1, date2, regionMaskList);
            } else if (temporalResolution == TemporalResolution.seasonal) {
                calendar.add(Calendar.MONTH, 3);
                Date date2 = calendar.getTime();
                List<OutputTimeStep> intermediateResults = aggregateTimeRanges(date1, date2, TemporalResolution.monthly, regionMaskList);
                result = performA4(intermediateResults, regionMaskList);
            } else /*if (temporalResolution == TemporalResolution.annual)*/ {
                calendar.add(Calendar.YEAR, 1);
                Date date2 = calendar.getTime();
                List<OutputTimeStep> intermediateResults = aggregateTimeRanges(date1, date2, TemporalResolution.monthly, regionMaskList);
                result = performA4(intermediateResults, regionMaskList);
            }
            results.add(new OutputTimeStep(date1, calendar.getTime(), result));
        }
        return results;
    }

    private List<CombinedAggregation> aggregateTimeRange(Date date1,
                                                         Date date2,
                                                         RegionMaskList regionMaskList) throws IOException {
        CellGrid<AggregationCell5> cell5Grid = aggregateTimeRange(date1, date2, RegionMask.combine(regionMaskList));
        List<CombinedAggregation> regionalCells = new ArrayList<CombinedAggregation>();
        for (RegionMask regionMask : regionMaskList) {
            CellGrid<AggregationCell5> regional5DegGrid = getRegionalGrid(cell5Grid, regionMask);
            CombinedAggregation combinedCell = performA3(regional5DegGrid, regionMask);
            regionalCells.add(combinedCell);
        }
        return regionalCells;
    }

    private CellGrid<AggregationCell5> aggregateTimeRange(Date date1,
                                                          Date date2,
                                                          RegionMask allRegionsMask) throws IOException {
        List<File> files = fileStore.getFiles(date1, date2);

        DateFormat isoDateFormat = UTC.getIsoFormat();
        LOGGER.info(String.format("Computing output time step from %s to %s, %d file(s) found.",
                                  isoDateFormat.format(date1), isoDateFormat.format(date2), files.size()));

        CellFactory<AggregationCell5> cell5Factory = fileType.getCell5Factory(createCoverageUncertaintyProvider(0)); // todo - provide correct month
        CellGrid<AggregationCell5> cell5Grid = new CellGrid<AggregationCell5>(GRID_DEF_GLOBAL_5, cell5Factory);

        for (File file : files) {
            LOGGER.info(String.format("Processing input %s file '%s'", fileStore.getProductType(), file));
            long t0 = System.currentTimeMillis();
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            try {
                aggregateSource(netcdfFile, allRegionsMask, cell5Grid);
            } finally {
                netcdfFile.close();
            }
            LOGGER.fine(String.format("Processing input %s file took %d ms", fileStore.getProductType(), System.currentTimeMillis() - t0));
        }

        return cell5Grid;
    }

    private void aggregateSource(NetcdfFile netcdfFile,
                                 RegionMask regionMask,
                                 CellGrid<AggregationCell5> cell5Grid) throws IOException {

        Date date = fileType.readDate(netcdfFile);
        int dayOfYear = UTC.getDayOfYear(date);
        LOGGER.fine("Day of year is " + dayOfYear);
        AggregationCell5Context aggregationCell5Context = new AggregationCell5Context(fileStore.getProductType().getGridDef(),
                                                                                      readSourceGrids(netcdfFile),
                                                                                      climatology.getAnalysedSstGrid(dayOfYear),
                                                                                      climatology.getSeaCoverageGrid());
        LOGGER.fine("Aggregating grid(s)...");
        long t0 = System.currentTimeMillis();
        GridDef sourceGridDef = aggregationCell5Context.getSourceGridDef();
        for (int cellY = 0; cellY < regionMask.getHeight(); cellY++) {
            for (int cellX = 0; cellX < regionMask.getWidth(); cellX++) {
                if (regionMask.getSampleBoolean(cellX, cellY)) {
                    Rectangle2D lonLatRectangle = regionMask.getGridDef().getLonLatRectangle(cellX, cellY);
                    Rectangle sourceGridRectangle = sourceGridDef.getGridRectangle(lonLatRectangle);
                    AggregationCell5 cell5 = cell5Grid.getCellSafe(cellX, cellY);
                    cell5.accumulate(aggregationCell5Context, sourceGridRectangle);
                }
            }
        }
        LOGGER.fine(String.format("Aggregating grid(s) took %d ms", (System.currentTimeMillis() - t0)));
    }

    private CellGrid<AggregationCell90> aggregateGrid5ToGrid90(CellGrid<AggregationCell5> cell5Grid) {
        Grid seaCoverage90Grid = climatology.getSeaCoverage90DegGrid();
        CellFactory<AggregationCell90> cell90Factory = fileType.getCell90Factory(createCoverageUncertaintyProvider(0)); // todo - provide correct month
        CellGrid<AggregationCell90> cell90Grid = new CellGrid<AggregationCell90>(GRID_DEF_GLOBAL_90, cell90Factory);
        for (int cellY = 0; cellY < cell5Grid.getGridDef().getHeight(); cellY++) {
            for (int cellX = 0; cellX < cell5Grid.getGridDef().getWidth(); cellX++) {
                AggregationCell5 cell5 = cell5Grid.getCell(cellX, cellY);
                if (cell5 != null && !cell5.isEmpty()) {
                    int cell90X = (cellX * cell90Grid.getGridDef().getWidth()) / cell5Grid.getGridDef().getWidth();
                    int cell90Y = (cellY * cell90Grid.getGridDef().getHeight()) / cell5Grid.getGridDef().getHeight();
                    AggregationCell90 cell90 = cell90Grid.getCellSafe(cell90X, cell90Y);
                    double seaCoverage90 = seaCoverage90Grid.getSampleDouble(cell90X, cell90Y);
                    // noinspection unchecked
                    cell90.accumulate(cell5, seaCoverage90);
                }
            }
        }
        return cell90Grid;
    }

    private SameMonthCombinedAggregation performA3(CellGrid<AggregationCell5> cell5Grid, RegionMask regionMask) {
        // Check if region is Globe or Hemisphere (actually: check if we have fully covered
        // 90 deg grid boxes), if so apply special averaging for all 90 deg grid boxes.
        CombinedAggregationFactory<SameMonthCombinedAggregation> combinedAggregationFactory = fileType.getSameMonthCombinedAggregationFactory();
        SameMonthCombinedAggregation combinedAggregation = combinedAggregationFactory.createCombinedAggregation();
        if (regionMask.getCoverage() == RegionMask.Coverage.Globe
                || regionMask.getCoverage() == RegionMask.Coverage.N_Hemisphere
                || regionMask.getCoverage() == RegionMask.Coverage.S_Hemisphere) {
            CellGrid<AggregationCell90> cell90Grid = aggregateGrid5ToGrid90(cell5Grid);
            aggregateGrid(cell90Grid, climatology.getSeaCoverage90DegGrid(), combinedAggregation);
        } else {
            aggregateGrid(cell5Grid, climatology.getSeaCoverage5DegGrid(), combinedAggregation);
        }
        return combinedAggregation;
    }

    private List<CombinedAggregation> performA4(List<OutputTimeStep> intermediateResults, RegionMaskList regionMaskList) {
        CombinedAggregationFactory<MultiMonthCombinedAggregation> a4CellFactoryCombined = fileType.getMultiMonthCombinedAggregationFactory();
        ArrayList<CombinedAggregation> multiMonthCombinedAggregations = new ArrayList<CombinedAggregation>();
        for (int regionIndex = 0; regionIndex < regionMaskList.size(); regionIndex++) {
            MultiMonthCombinedAggregation multiMonthCombinedAggregation = a4CellFactoryCombined.createCombinedAggregation();
            for (OutputTimeStep intermediateResult : intermediateResults) {
                CombinedAggregation sameMonthCombinedAggregation = intermediateResult.regionalAverages.get(regionIndex);
                // noinspection unchecked
                multiMonthCombinedAggregation.accumulate(sameMonthCombinedAggregation);
            }
            multiMonthCombinedAggregations.add(multiMonthCombinedAggregation);
        }
        return multiMonthCombinedAggregations;
    }

    public static <C extends AggregationCell> void aggregateGrid(CellGrid<C> cellGrid, Grid seaCoverageGrid, SameMonthCombinedAggregation aggregation) {
        int width = cellGrid.getGridDef().getWidth();
        int height = cellGrid.getGridDef().getHeight();
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

    private static CellGrid<AggregationCell5> getRegionalGrid(CellGrid<AggregationCell5> combinedGrid5, RegionMask regionMask) {
        CellGrid<AggregationCell5> regionalGrid5 = new CellGrid<AggregationCell5>(combinedGrid5.getGridDef(), combinedGrid5.getCellFactory());
        for (int cellY = 0; cellY < combinedGrid5.getGridDef().getHeight(); cellY++) {
            for (int cellX = 0; cellX < combinedGrid5.getGridDef().getWidth(); cellX++) {
                if (regionMask.getSampleBoolean(cellX, cellY)) {
                    AggregationCell5 cell5 = combinedGrid5.getCell(cellX, cellY);
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

}
