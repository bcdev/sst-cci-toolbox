package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.*;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// todo - make it a parameterised algorithm object

/**
 * Utility that performs the regional averaging.
 *
 * @author Norman Fomferra
 */
public class RegionalAveraging {
    private static final GridDef GLOBAL_5_DEG_GRID_DEF = GridDef.createGlobalGrid(5.0);
    private static final GridDef GLOBAL_90_DEG_GRID_DEF = GridDef.createGlobalGrid(90.0);

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

        CellGrid combined5DGrid = computeCombinedGrid(productStore, climatology,sstDepth, date1, date2, combinedRegionMask);

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
        CellGrid regional5DGrid = new CellGrid(combined5DGrid.getGridDef());
        for (int cellY = 0; cellY < combined5DGrid.getGridDef().getHeight(); cellY++) {
            for (int cellX = 0; cellX < combined5DGrid.getGridDef().getWidth(); cellX++) {
                if (regionMask.getSampleBoolean(cellX, cellY)) {
                    Cell cell = combined5DGrid.getCell(cellX, cellY);
                    if (cell != null) {
                        regional5DGrid.setCell(cellX, cellY, cell.clone());
                    }
                }
            }
        }
        return regional5DGrid;
    }

    private static CellGrid averageTo90DegGrid(CellGrid regional5DGrid, RegionMask combinedRegionMask) {
        CellGrid regional90DegGrid = new CellGrid(GLOBAL_90_DEG_GRID_DEF);
        for (int cellY = 0; cellY < regional5DGrid.getGridDef().getHeight(); cellY++) {
            for (int cellX = 0; cellX < regional5DGrid.getGridDef().getWidth(); cellX++) {
                if (combinedRegionMask.getSampleBoolean(cellX, cellY)) {
                    Cell regional5DegCell = regional5DGrid.getCell(cellX, cellY);
                    if (regional5DegCell != null) {
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
        System.out.printf("Computing output time step from %s to %s. %d file(s) found.%n",
                          isoDateFormat.format(date1), isoDateFormat.format(date2), files.size());


        CellGrid combined5DGrid = new CellGrid(GLOBAL_5_DEG_GRID_DEF);

        // todo - Aggregate all variables of all netcdfFile into 72x36 5deg grid boxes
        for (File file : files) {
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            long t0 = System.currentTimeMillis();
            System.out.printf("Aggregating file %s\n", netcdfFile.getLocation());
            try {
                // todo - generalise code: the following code is for ARC L3U
                // {{{<<<
                float time = netcdfFile.findTopVariable("time").readScalarFloat();
                int secondsSince1981 = Math.round(time);
                Calendar calendar = UTC.createCalendar(1981);
                calendar.add(Calendar.SECOND, secondsSince1981);
                int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
                if (dayOfYear == 366) {
                    dayOfYear = 365; // Leap year
                }
                System.out.println("Day of year is " + dayOfYear);
                Aggregator aggregator = new ArcL3Aggregator();
                System.out.printf("Reading SST from %s\n", netcdfFile.getLocation());
                Grid sstGrid;
                if (sstDepth == SstDepth.depth_20) {
                    sstGrid = NcUtils.readGrid(netcdfFile, "sst_depth", 0, sourceGridDef);
                } else if (sstDepth == SstDepth.depth_100) {
                    sstGrid = NcUtils.readGrid(netcdfFile, "sst_depth", 1, sourceGridDef);
                } else /*if (sstDepth == SstDepth.skin)*/ {
                    sstGrid = NcUtils.readGrid(netcdfFile, "sst_skin", 0, sourceGridDef);
                }
                // System.out.printf("Reading uncertainty from %s\n", netcdfFile.getLocation());
                // Grid uncertaintyGrid = NcUtils.readGrid(netcdfFile, "uncertainty", 0, productStore.getProductType().getGridDef());
                // >>>}}}

                Grid analysedSstGrid = climatology.getAnalysedSstGrid(dayOfYear);
                // todo - use analysed SST to compute anomaly


                // todo - we can compute all the grid rows in parallel (use Executors / ExecutorService)

                // note: the following loop may be inefficient for Global or Hemispheric coverage
                for (int cellY = 0; cellY < combinedRegionMask.getHeight(); cellY++) {
                    for (int cellX = 0; cellX < combinedRegionMask.getWidth(); cellX++) {
                        if (combinedRegionMask.getSampleBoolean(cellX, cellY)) {

                            Rectangle2D lonLatRectangle = combinedRegionMask.getGridDef().getLonLatRectangle(cellX, cellY);
                            Rectangle sourceGridRectangle = sourceGridDef.getGridRectangle(lonLatRectangle);

                            Cell combined5DCell = combined5DGrid.createCell();
                            aggregate(sstGrid, sourceGridRectangle, combined5DCell);
                            if (combined5DCell.getAccuCount() > 0) {
                                combined5DGrid.setCell(cellX, cellY, combined5DCell);
                            }

                            // todo - for each uncertainty variables A-G: create a grid comprising max. 72 x 36 aggregation results.
                        }
                    }
                }
            } finally {
                netcdfFile.close();
            }

            long t1 = System.currentTimeMillis();
            long dt = t1 - t0;
            System.out.println("Aggregating completed after " + dt + " ms");
        }
        return combined5DGrid;
    }

    public interface Reader {
        Array read(Variable variable, Rectangle gridRectangle) throws IOException;
    }

    public static class ArcL3Reader implements Reader {
        @Override
        public Array read(Variable variable, Rectangle gridRectangle) throws IOException {
            //System.out.println("reading sstVar rect " + gridRectangle);
            return NcUtils.readRaster(variable, gridRectangle, 0);
        }
    }

    public static void aggregate(Grid grid, Rectangle sourceGridRectangle, Cell cell) {
        final int x0 = sourceGridRectangle.x;
        final int y0 = sourceGridRectangle.y;
        final int x1 = x0 +sourceGridRectangle.width - 1;
        final int y1 = y0 +sourceGridRectangle.height - 1;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                cell.accumulate(grid.getSampleDouble(x, y));
            }
        }
    }

    public static void aggregateAnomaly(Grid grid, Grid refGrid, Rectangle sourceGridRectangle, Cell cell) {
        final int x0 = sourceGridRectangle.x;
        final int y0 = sourceGridRectangle.y;
        final int x1 = x0 +sourceGridRectangle.width - 1;
        final int y1 = y0 +sourceGridRectangle.height - 1;
        final GridDef gridDef = grid.getGridDef();
        final GridDef refGridDef = refGrid.getGridDef();
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                // todo - we use nearest-neighbour refGrid access here, but we shall resample the refGrid first using simple average (--> Nick R.)
                final double lon = gridDef.getCenterLon(x);
                final double lat = gridDef.getCenterLat(y);
                final int refX = refGridDef.getGridX(lon, true);
                final int refY = refGridDef.getGridY(lat, true);
                cell.accumulate(grid.getSampleDouble(x, y)
                                        - refGrid.getSampleDouble(refX, refY));
            }
        }
    }

    public interface Aggregator {
        void aggregate(Array sstData, Array maskData, Number fillValue, Cell cell);

        void aggregate(Grid sstGrid, Rectangle sourceGridRectangle, Cell combined5DCell);
    }

    public static class ArcL3Aggregator implements Aggregator {
        @Override
        public void aggregate(Array sstData, Array maskData, Number fillValue, Cell cell) {
            int size = (int) sstData.getSize();
            for (int i = 0; i < size; i++) {
                double sstSample = sstData.getDouble(i);
                int maskSample = maskData.getInt(i);

                boolean isQualityLevel5 = maskSample == 0; // (maskSample & 0x10) != 0;
                boolean isNumber = !Double.isNaN(sstSample);
                boolean hasData = fillValue == null || sstSample != fillValue.doubleValue();

                if (isQualityLevel5 && isNumber && hasData) {
                    cell.accumulate(sstSample);
                }
            }
        }

        @Override
        public void aggregate(Grid grid, Rectangle sourceGridRectangle, Cell cell) {
            final int x0 = sourceGridRectangle.x;
            final int y0 = sourceGridRectangle.y;
            final int x1 = x0 +sourceGridRectangle.width - 1;
            final int y1 = y0 +sourceGridRectangle.height - 1;
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    cell.accumulate(grid.getSampleDouble(x, y));
                }
            }
        }
    }
}
