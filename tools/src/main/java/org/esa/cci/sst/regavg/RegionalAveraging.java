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

    public static List<OutputTimeStep> computeOutputTimeSteps(FileStore fileStore,
                                                              Climatology climatology,
                                                              OutputType outputType, SstDepth sstDepth,
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
                result = computeOutputTimeStep(fileStore, climatology, outputType, sstDepth, date1, date2, regionMaskList);
            } else if (temporalResolution == TemporalResolution.monthly) {
                calendar.add(Calendar.MONTH, 1);
                Date date2 = calendar.getTime();
                result = computeOutputTimeStep(fileStore, climatology, outputType, sstDepth, date1, date2, regionMaskList);
            } else if (temporalResolution == TemporalResolution.seasonal) {
                calendar.add(Calendar.MONTH, 3);
                Date date2 = calendar.getTime();
                List<OutputTimeStep> intermediateResults = computeOutputTimeSteps(fileStore, climatology, outputType, sstDepth, date1, date2, TemporalResolution.monthly, regionMaskList);
                result = aggregateMultiMonthsResults(intermediateResults, regionMaskList);
            } else /*if (temporalResolution == TemporalResolution.anual)*/ {
                calendar.add(Calendar.YEAR, 1);
                Date date2 = calendar.getTime();
                List<OutputTimeStep> intermediateResults = computeOutputTimeSteps(fileStore, climatology, outputType, sstDepth, date1, date2, TemporalResolution.monthly, regionMaskList);
                result = aggregateMultiMonthsResults(intermediateResults, regionMaskList);
            }
            results.add(new OutputTimeStep(date1, calendar.getTime(), result));
        }
        return results;
    }

    private static List<Cell> aggregateMultiMonthsResults(List<OutputTimeStep> monthlyResults, RegionMaskList regionMaskList) {
        // todo - the following code is only valid for simple mean (SST, SST anomaly), but nor for the uncertaincies. Aggregate these according to Nick's equations
        ArrayList<Cell> combinedCells = new ArrayList<Cell>();
        for (int i = 0; i < regionMaskList.size(); i++) {
            Cell combinedCell = new Cell();
            for (OutputTimeStep monthlyResult : monthlyResults) {
                Cell cell = monthlyResult.regionalAverages.get(i);
                combinedCell.accumulate(cell);
            }
            combinedCells.add(combinedCell);
        }
        return combinedCells;
    }

    private static List<Cell> computeOutputTimeStep(FileStore fileStore,
                                                    Climatology climatology,
                                                    OutputType outputType,
                                                    SstDepth sstDepth,
                                                    Date date1,
                                                    Date date2,
                                                    RegionMaskList regionMaskList) throws IOException {

        RegionMask combinedRegionMask = RegionMask.combine(regionMaskList);

        CellGrid combined5DGrid = computeCombinedGrid(fileStore, climatology, outputType, sstDepth, date1, date2, combinedRegionMask);

        List<Cell> regionalCells = new ArrayList<Cell>();
        for (RegionMask regionMask : regionMaskList) {

            // If we have multiple masks, split the aggregation results we've got to each region grid.
            CellGrid regional5DegGrid = getRegionalGrid(combined5DGrid, climatology.getSeaCoverage5DegGrid(), regionMask);

            // Check if region is Globe or Hemisphere (actually: check if we have fully covered
            // 90 deg grid boxes), if so apply special averaging for all 90 deg grid boxes.
            if (regionMask.getCoverage() == RegionMask.Coverage.Globe
                    || regionMask.getCoverage() == RegionMask.Coverage.N_Hemisphere
                    || regionMask.getCoverage() == RegionMask.Coverage.S_Hemisphere) {
                CellGrid regional90DegGrid = averageTo90DegGrid(regional5DegGrid, climatology.getSeaCoverage90DegGrid(), combinedRegionMask);
                regionalCells.add(regional90DegGrid.combine());
            } else {
                regionalCells.add(regional5DegGrid.combine());
            }
        }

        return regionalCells;
    }

    private static CellGrid getRegionalGrid(CellGrid combined5DegGrid, Grid waterCoverage5DegGrid, RegionMask regionMask) {
        CellGrid regional5DGrid = new CellGrid(combined5DegGrid.getGridDef());
        for (int cellY = 0; cellY < combined5DegGrid.getGridDef().getHeight(); cellY++) {
            for (int cellX = 0; cellX < combined5DegGrid.getGridDef().getWidth(); cellX++) {
                if (regionMask.getSampleBoolean(cellX, cellY)) {
                    Cell cell = combined5DegGrid.getCell(cellX, cellY);
                    if (cell != null && cell.getAccuCount() > 0) {
                        Cell cellClone = cell.clone();  // :-)
                        cellClone.setWeight(waterCoverage5DegGrid.getSampleDouble(cellX, cellY));
                        regional5DGrid.setCell(cellX, cellY, cellClone);
                    }
                }
            }
        }
        return regional5DGrid;
    }

    private static CellGrid averageTo90DegGrid(CellGrid regional5DegGrid, Grid waterCoverage90DegGrid, RegionMask combinedRegionMask) {
        CellGrid regional90DegGrid = new CellGrid(GLOBAL_90_DEG_GRID_DEF);
        for (int cellY = 0; cellY < regional5DegGrid.getGridDef().getHeight(); cellY++) {
            for (int cellX = 0; cellX < regional5DegGrid.getGridDef().getWidth(); cellX++) {
                if (combinedRegionMask.getSampleBoolean(cellX, cellY)) {
                    Cell regional5DegCell = regional5DegGrid.getCell(cellX, cellY);
                    if (regional5DegCell != null && regional5DegCell.getAccuCount() > 0) {
                        int cellX90D = (cellX * regional90DegGrid.getGridDef().getWidth()) / regional5DegGrid.getGridDef().getWidth();
                        int cellY90D = (cellY * regional90DegGrid.getGridDef().getHeight()) / regional5DegGrid.getGridDef().getHeight();
                        Cell regional90DCell = regional90DegGrid.getCellSafe(cellX90D, cellY90D);
                        regional90DCell.setWeight(waterCoverage90DegGrid.getSampleDouble(cellX90D, cellY90D));
                        regional90DCell.accumulate(regional5DegCell);
                    }
                }
            }
        }
        return regional90DegGrid;
    }

    private static CellGrid computeCombinedGrid(FileStore fileStore,
                                                Climatology climatology,
                                                OutputType outputType, SstDepth sstDepth,
                                                Date date1, Date date2,
                                                RegionMask combinedRegionMask) throws IOException {
        List<File> files = fileStore.getFiles(date1, date2);

        DateFormat isoDateFormat = UTC.getIsoFormat();
        LOGGER.info(String.format("Computing output time step from %s to %s, %d file(s) found.",
                                  isoDateFormat.format(date1), isoDateFormat.format(date2), files.size()));

        CellGrid combined5DGrid = new CellGrid(GLOBAL_5_DEG_GRID_DEF);

        for (File file : files) {
            LOGGER.info(String.format("Processing input %s file '%s'", fileStore.getProductType(), file));
            long t0 = System.currentTimeMillis();
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            try {
                aggregateFile(fileStore.getProductType(), netcdfFile, climatology, outputType, sstDepth, combinedRegionMask, combined5DGrid);
            } finally {
                netcdfFile.close();
            }
            LOGGER.fine(String.format("Processing input %s file took %d ms", fileStore.getProductType(), System.currentTimeMillis() - t0));
        }

        return combined5DGrid;
    }

    private static void aggregateFile(ProductType productType, NetcdfFile netcdfFile, Climatology climatology, OutputType outputType, SstDepth sstDepth, RegionMask combinedRegionMask, CellGrid combined5DGrid) throws IOException {

        Date date = productType.getFileType().readDate(netcdfFile);
        int dayOfYear = UTC.getDayOfYear(date);
        LOGGER.fine("Day of year is " + dayOfYear);

        Grid[] grids = readGrids(productType, netcdfFile, sstDepth);

        // todo - Use all grids, currently we only use the first (always SST).
        Grid sstGrid = grids[0];
        Grid analysedSstGrid = climatology.getAnalysedSstGrid(dayOfYear);
        Grid seaCoverageGrid = climatology.getSeaCoverageGrid();

        aggregate(sstGrid, analysedSstGrid, seaCoverageGrid, combinedRegionMask, combined5DGrid, outputType);
    }

    private static Grid[] readGrids(ProductType productType, NetcdfFile netcdfFile, SstDepth sstDepth) throws IOException {
        long t0 = System.currentTimeMillis();
        VariableType[] variableTypes = productType.getFileType().getVariableTypes(sstDepth);
        LOGGER.fine(String.format("Reading %d grid(s)...", variableTypes.length));
        Grid[] grids = new Grid[variableTypes.length];
        for (int i = 0; i < grids.length; i++) {
            grids[i] = variableTypes[i].readGrid(netcdfFile);;
        }
        LOGGER.fine(String.format("Reading grid(s) took %d ms", (System.currentTimeMillis() - t0)));
        return grids;
    }

    private static void aggregate(Grid sstGrid, Grid analysedSstGrid, Grid seaCoverageGrid, RegionMask combinedRegionMask, CellGrid combined5DGrid, OutputType outputType) {
        long t0 = System.currentTimeMillis();
        LOGGER.fine("Aggregating grid(s)...");
        GridDef sourceGridDef = sstGrid.getGridDef();
        for (int cellY = 0; cellY < combinedRegionMask.getHeight(); cellY++) {
            for (int cellX = 0; cellX < combinedRegionMask.getWidth(); cellX++) {
                if (combinedRegionMask.getSampleBoolean(cellX, cellY)) {

                    Rectangle2D lonLatRectangle = combinedRegionMask.getGridDef().getLonLatRectangle(cellX, cellY);
                    Rectangle sourceGridRectangle = sourceGridDef.getGridRectangle(lonLatRectangle);

                    Cell combined5DCell = combined5DGrid.createCell();
                    aggregateRect(sstGrid, analysedSstGrid, seaCoverageGrid, sourceGridRectangle, outputType, combined5DCell);
                    if (combined5DCell.getAccuCount() > 0) {
                        combined5DGrid.setCell(cellX, cellY, combined5DCell);
                    }

                    // todo - for each uncertainty variables A-G: create a grid comprising max. 72 x 36 aggregation results.
                }
            }
        }
        LOGGER.fine(String.format("Aggregating grid(s) took %d ms", (System.currentTimeMillis() - t0)));
    }

    public static void aggregateRect(Grid grid, Grid refGrid, Grid weightGrid, Rectangle rect, OutputType outputType, Cell cell) {
        final int x0 = rect.x;
        final int y0 = rect.y;
        final int x1 = x0 + rect.width - 1;
        final int y1 = y0 + rect.height - 1;
        final boolean anomaly = outputType == OutputType.anomaly;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                final double weight = weightGrid.getSampleDouble(x, y);
                if (weight != 0.0) {
                    if (anomaly) {
                        cell.accumulate(grid.getSampleDouble(x, y) - refGrid.getSampleDouble(x, y), weight, 1L);
                    } else {
                        cell.accumulate(grid.getSampleDouble(x, y), weight, 1L);
                    }
                }
            }
        }
    }

}
