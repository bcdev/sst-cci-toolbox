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

// todo - make it a parameterised algorithm object

/**
 * Utility that performs the regional averaging.
 *
 * @author Norman Fomferra
 */
public class RegionalAveraging {
    private static final GridDef GLOBAL_5_DEG_GRID_DEF = GridDef.createGlobal(5.0);
    private static final GridDef GLOBAL_90_DEG_GRID_DEF = GridDef.createGlobal(90.0);

    private static final Logger LOGGER = Tool.LOGGER;

    public static class OutputTimeStep {
        final Date date1;
        final Date date2;
        final List<Cell> regionalAverages;

        public OutputTimeStep(Date date1, Date date2, List<Cell> regionalAverages) {
            this.date1 = date1;
            this.date2 = date2;
            this.regionalAverages = regionalAverages;
        }
    }

    public static List<OutputTimeStep> computeOutputTimeSteps(ProductStore productStore,
                                                              Climatology climatology,
                                                              SstDepth sstDepth,
                                                              Date startDate,
                                                              Date endDate,
                                                              TemporalResolution temporalResolution,
                                                              RegionMaskList regionMaskList)
            throws IOException {

        List<OutputTimeStep> results = new ArrayList<OutputTimeStep>();
        Calendar calendar = UTC.createCalendar(startDate);
        while (calendar.getTime().before(endDate)) {
            Date date1 = calendar.getTime();
            List<Cell> result;
            if (temporalResolution == TemporalResolution.daily) {
                calendar.add(Calendar.DATE, 1);
                Date date2 = calendar.getTime();
                result = computeOutputTimeStep(productStore, climatology, sstDepth, date1, date2, regionMaskList);
            } else if (temporalResolution == TemporalResolution.monthly) {
                calendar.add(Calendar.MONTH, 1);
                Date date2 = calendar.getTime();
                result = computeOutputTimeStep(productStore, climatology, sstDepth, date1, date2, regionMaskList);
            } else if (temporalResolution == TemporalResolution.seasonal) {
                calendar.add(Calendar.MONTH, 3);
                Date date2 = calendar.getTime();
                result = computeMonthlyOutputTimeStep(productStore, climatology, sstDepth, date1, date2, regionMaskList);
            } else /*if (temporalResolution == TemporalResolution.anual)*/ {
                calendar.add(Calendar.YEAR, 1);
                Date date2 = calendar.getTime();
                result = computeMonthlyOutputTimeStep(productStore, climatology, sstDepth, date1, date2, regionMaskList);
            }
            results.add(new OutputTimeStep(date1, calendar.getTime(), result));
        }
        return results;
    }

    private static List<Cell> computeMonthlyOutputTimeStep(ProductStore productStore, Climatology climatology, SstDepth sstDepth, Date date1, Date date2, RegionMaskList regionMaskList) throws IOException {
        List<OutputTimeStep> intermediateResults = computeOutputTimeSteps(productStore, climatology, sstDepth, date1, date2, TemporalResolution.monthly, regionMaskList);
        return aggregateMultiMonthsResults(intermediateResults);
    }

    private static List<Cell> aggregateMultiMonthsResults(List<OutputTimeStep> monthlyResults) {
        // todo - aggregate results according to Nick's equations
        throw new IllegalStateException("Not implemented yet.");
    }

    private static List<Cell> computeOutputTimeStep(ProductStore productStore,
                                                    Climatology climatology,
                                                    SstDepth sstDepth,
                                                    Date date1,
                                                    Date date2,
                                                    RegionMaskList regionMaskList) throws IOException {

        RegionMask combinedRegionMask = RegionMask.combine(regionMaskList);

        CellGrid combined5DGrid = computeCombinedGrid(productStore, climatology, sstDepth, date1, date2, combinedRegionMask);

        List<Cell> regionalCells = new ArrayList<Cell>();
        for (RegionMask regionMask : regionMaskList) {

            // If we have multiple masks, split the aggregation results we've got to each region grid.
            CellGrid regional5DegGrid = getRegionalGrid(combined5DGrid, regionMask);

            // Check if region is Globe or Hemisphere (actually: check if we have fully covered
            // 90 deg grid boxes), if so apply special averaging for all 90 deg grid boxes.
            if (regionMask.getCoverage() == RegionMask.Coverage.Globe
                    || regionMask.getCoverage() == RegionMask.Coverage.N_Hemisphere
                    || regionMask.getCoverage() == RegionMask.Coverage.S_Hemisphere) {
                CellGrid regional90DegGrid = averageTo90DegGrid(regional5DegGrid, combinedRegionMask);
                regionalCells.add(regional90DegGrid.combine());
            } else {
                regionalCells.add(regional5DegGrid.combine());
            }
        }

        return regionalCells;
    }

    private static CellGrid getRegionalGrid(CellGrid combined5DGrid, RegionMask regionMask) {
        // todo - use Climatology.getWaterCoverageGrid5() here, see Nick's doc.
        CellGrid regional5DGrid = new CellGrid(combined5DGrid.getGridDef());
        for (int cellY = 0; cellY < combined5DGrid.getGridDef().getHeight(); cellY++) {
            for (int cellX = 0; cellX < combined5DGrid.getGridDef().getWidth(); cellX++) {
                if (regionMask.getSampleBoolean(cellX, cellY)) {
                    Cell cell = combined5DGrid.getCell(cellX, cellY);
                    if (cell != null && cell.getAccuCount() > 0) {
                        regional5DGrid.setCell(cellX, cellY, cell.clone());
                    }
                }
            }
        }
        return regional5DGrid;
    }

    private static CellGrid averageTo90DegGrid(CellGrid regional5DGrid, RegionMask combinedRegionMask) {
        // todo - use Climatology.getWaterCoverageGrid90() here, see Nick's doc.
        CellGrid regional90DegGrid = new CellGrid(GLOBAL_90_DEG_GRID_DEF);
        for (int cellY = 0; cellY < regional5DGrid.getGridDef().getHeight(); cellY++) {
            for (int cellX = 0; cellX < regional5DGrid.getGridDef().getWidth(); cellX++) {
                if (combinedRegionMask.getSampleBoolean(cellX, cellY)) {
                    Cell regional5DegCell = regional5DGrid.getCell(cellX, cellY);
                    if (regional5DegCell != null && regional5DegCell.getAccuCount() > 0) {
                        int cellX90D = (cellX * regional90DegGrid.getGridDef().getWidth()) / regional5DGrid.getGridDef().getWidth();
                        int cellY90D = (cellY * regional90DegGrid.getGridDef().getHeight()) / regional5DGrid.getGridDef().getHeight();
                        Cell regional90DCell = regional90DegGrid.getCellSafe(cellX90D, cellY90D);
                        regional90DCell.accumulate(regional5DegCell);
                    }
                }
            }
        }
        return regional90DegGrid;
    }

    private static CellGrid computeCombinedGrid(ProductStore productStore,
                                                Climatology climatology,
                                                SstDepth sstDepth,
                                                Date date1, Date date2,
                                                RegionMask combinedRegionMask) throws IOException {
        List<File> files = productStore.getFiles(date1, date2);

        GridDef sourceGridDef = productStore.getProductType().getGridDef();

        DateFormat isoDateFormat = UTC.getIsoFormat();
        LOGGER.info(String.format("Computing output time step from %s to %s, %d file(s) found.",
                                  isoDateFormat.format(date1), isoDateFormat.format(date2), files.size()));

        CellGrid combined5DGrid = new CellGrid(GLOBAL_5_DEG_GRID_DEF);

        for (File file : files) {
            LOGGER.info(String.format("Processing input file '%s'", file));
            long t0 = System.currentTimeMillis();
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            try {
                aggregateFile(netcdfFile, sourceGridDef, climatology, sstDepth, combinedRegionMask, combined5DGrid);
            } finally {
                netcdfFile.close();
            }
            LOGGER.fine("Processing took " + (System.currentTimeMillis() - t0) + " ms");
        }

        return combined5DGrid;
    }

    private static void aggregateFile(NetcdfFile netcdfFile, GridDef sourceGridDef, Climatology climatology, SstDepth sstDepth, RegionMask combinedRegionMask, CellGrid combined5DGrid) throws IOException {
        // todo - generalise code: the following code is for ARC L3U,
        // e.g.
        //
        //  class ProductType {
        //     abstract GridReader getGridReader(NetcdfFile netcdfFile, GridDef sourceGridDef);
        //  }
        //
        //  interface GridReader {
        //     int getDayOfYear();
        //     Grid getSst(SstDepth sstDepth);
        //     Grid getUncertainty(UncertaintyType uncertaintyType); // See Nick's tool spec.
        // }
        //
        // ((([[[{{{<<<
        float time = netcdfFile.findTopVariable("time").readScalarFloat();
        int secondsSince1981 = Math.round(time);
        Calendar calendar = UTC.createCalendar(1981);
        calendar.add(Calendar.SECOND, secondsSince1981);
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        if (dayOfYear == 366) {
            dayOfYear = 365; // Leap year
        }
        LOGGER.fine("Day of year is " + dayOfYear);

        long t0 = System.currentTimeMillis();
        LOGGER.fine("Reading grid(s)...");
        Grid sstGrid;
        if (sstDepth == SstDepth.depth_20) {
            sstGrid = NcUtils.readGrid(netcdfFile, "sst_depth", 0, sourceGridDef);
        } else if (sstDepth == SstDepth.depth_100) {
            sstGrid = NcUtils.readGrid(netcdfFile, "sst_depth", 1, sourceGridDef);
        } else /*if (sstDepth == SstDepth.skin)*/ {
            sstGrid = NcUtils.readGrid(netcdfFile, "sst_skin", 0, sourceGridDef);
        }
        // Grid uncertaintyGrid = NcUtils.readGrid(netcdfFile, "uncertainty", 0, sourceGridDef);
        LOGGER.fine(String.format("Reading grid(s) took %d ms", (System.currentTimeMillis() - t0)));
        // >>>}}}]]])))

        Grid analysedSstGrid = climatology.getAnalysedSstGrid(dayOfYear);

        aggregate(sstGrid, analysedSstGrid, combinedRegionMask, combined5DGrid);
    }

    private static void aggregate(Grid sstGrid, Grid analysedSstGrid, RegionMask combinedRegionMask, CellGrid combined5DGrid) {
        long t0 = System.currentTimeMillis();
        LOGGER.fine("Aggregating grid(s)...");
        GridDef sourceGridDef = sstGrid.getGridDef();
        for (int cellY = 0; cellY < combinedRegionMask.getHeight(); cellY++) {
            for (int cellX = 0; cellX < combinedRegionMask.getWidth(); cellX++) {
                if (combinedRegionMask.getSampleBoolean(cellX, cellY)) {

                    Rectangle2D lonLatRectangle = combinedRegionMask.getGridDef().getLonLatRectangle(cellX, cellY);
                    Rectangle sourceGridRectangle = sourceGridDef.getGridRectangle(lonLatRectangle);

                    Cell combined5DCell = combined5DGrid.createCell();
                    aggregateAnomaly(sstGrid, analysedSstGrid, sourceGridRectangle, combined5DCell);
                    if (combined5DCell.getAccuCount() > 0) {
                        combined5DGrid.setCell(cellX, cellY, combined5DCell);
                    }

                    // todo - for each uncertainty variables A-G: create a grid comprising max. 72 x 36 aggregation results.
                }
            }
        }
        LOGGER.fine(String.format("Aggregating grid(s) took %d ms", (System.currentTimeMillis() - t0)));
    }

    public static void aggregate(Grid grid, Rectangle sourceGridRectangle, Cell cell) {
        final int x0 = sourceGridRectangle.x;
        final int y0 = sourceGridRectangle.y;
        final int x1 = x0 + sourceGridRectangle.width - 1;
        final int y1 = y0 + sourceGridRectangle.height - 1;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                cell.accumulate(grid.getSampleDouble(x, y));
            }
        }
    }

    public static void aggregateAnomaly(Grid grid, Grid refGrid, Rectangle sourceGridRectangle, Cell cell) {
        final int x0 = sourceGridRectangle.x;
        final int y0 = sourceGridRectangle.y;
        final int x1 = x0 + sourceGridRectangle.width - 1;
        final int y1 = y0 + sourceGridRectangle.height - 1;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                cell.accumulate(grid.getSampleDouble(x, y) - refGrid.getSampleDouble(x, y));
            }
        }
    }

}
