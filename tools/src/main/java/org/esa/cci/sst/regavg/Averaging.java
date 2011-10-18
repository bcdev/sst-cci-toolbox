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

    private final FileStore fileStore;
    private final Climatology climatology;
    private final LUT1 lut1;
    private final LUT2 lut2;
    private final SstDepth sstDepth;

    public Averaging(FileStore fileStore,
                     Climatology climatology,
                     LUT1 lut1,
                     LUT2 lut2,
                     SstDepth sstDepth) {
        this.fileStore = fileStore;
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
            List<Cell> result;
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
                result = aggregateMonthlyResults(intermediateResults, regionMaskList);
            } else /*if (temporalResolution == TemporalResolution.annual)*/ {
                calendar.add(Calendar.YEAR, 1);
                Date date2 = calendar.getTime();
                List<OutputTimeStep> intermediateResults = aggregateTimeRanges(date1, date2, TemporalResolution.monthly, regionMaskList);
                result = aggregateMonthlyResults(intermediateResults, regionMaskList);
            }
            results.add(new OutputTimeStep(date1, calendar.getTime(), result));
        }
        return results;
    }

    private List<Cell> aggregateTimeRange(Date date1,
                                          Date date2,
                                          RegionMaskList regionMaskList) throws IOException {
        RegionMask combinedRegionMask = RegionMask.combine(regionMaskList);
        CellGrid combined5DGrid = aggregateTimeRangeTo5DegGrid(date1, date2, combinedRegionMask);
        List<Cell> regionalCells = new ArrayList<Cell>();
        for (RegionMask regionMask : regionMaskList) {
            CellGrid regional5DegGrid = getRegionalGrid(combined5DGrid, regionMask);
            Cell aggregate = aggregateToSingleValue(regional5DegGrid, regionMask);
            regionalCells.add(aggregate);
        }
        return regionalCells;
    }

    private CellGrid aggregateTimeRangeTo5DegGrid(Date date1, Date date2,
                                                  RegionMask combinedRegionMask) throws IOException {
        List<File> files = fileStore.getFiles(date1, date2);

        DateFormat isoDateFormat = UTC.getIsoFormat();
        LOGGER.info(String.format("Computing output time step from %s to %s, %d file(s) found.",
                                  isoDateFormat.format(date1), isoDateFormat.format(date2), files.size()));

        CellGrid combined5DegGrid = new CellGrid(GLOBAL_5_DEG_GRID_DEF, fileStore.getProductType().getFileType().getCellFactory());

        for (File file : files) {
            LOGGER.info(String.format("Processing input %s file '%s'", fileStore.getProductType(), file));
            long t0 = System.currentTimeMillis();
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            try {
                aggregateFileTo5DegGrid(fileStore.getProductType(), netcdfFile, combinedRegionMask, combined5DegGrid);
            } finally {
                netcdfFile.close();
            }
            LOGGER.fine(String.format("Processing input %s file took %d ms", fileStore.getProductType(), System.currentTimeMillis() - t0));
        }

        return combined5DegGrid;
    }

    private void aggregateFileTo5DegGrid(ProductType productType,
                                         NetcdfFile netcdfFile,
                                         RegionMask combinedRegionMask,
                                         CellGrid combined5DGrid) throws IOException {

        Date date = productType.getFileType().readDate(netcdfFile);
        int dayOfYear = UTC.getDayOfYear(date);
        LOGGER.fine("Day of year is " + dayOfYear);
        SstCellContext sstCellContext = new SstCellContext(productType.getGridDef(),
                                                           readSourceGrids(productType, netcdfFile, sstDepth),
                                                           climatology.getAnalysedSstGrid(dayOfYear),
                                                           climatology.getSeaCoverageGrid());
        LOGGER.fine("Aggregating grid(s)...");
        long t0 = System.currentTimeMillis();
        GridDef sourceGridDef = sstCellContext.getSourceGridDef();
        for (int cellY = 0; cellY < combinedRegionMask.getHeight(); cellY++) {
            for (int cellX = 0; cellX < combinedRegionMask.getWidth(); cellX++) {
                if (combinedRegionMask.getSampleBoolean(cellX, cellY)) {
                    Rectangle2D lonLatRectangle = combinedRegionMask.getGridDef().getLonLatRectangle(cellX, cellY);
                    Rectangle sourceGridRectangle = sourceGridDef.getGridRectangle(lonLatRectangle);
                    Cell combined5DegCell = combined5DGrid.getCellSafe(cellX, cellY);
                    combined5DegCell.aggregateSourceRect(sstCellContext, sourceGridRectangle);
                }
            }
        }
        LOGGER.fine(String.format("Aggregating grid(s) took %d ms", (System.currentTimeMillis() - t0)));
    }

    private static CellGrid aggregate5DegTo90DegGrid(CellGrid regional5DegGrid) {
        CellGrid regional90DegGrid = new CellGrid(GLOBAL_90_DEG_GRID_DEF, regional5DegGrid.getCellFactory());
        for (int cellY = 0; cellY < regional5DegGrid.getGridDef().getHeight(); cellY++) {
            for (int cellX = 0; cellX < regional5DegGrid.getGridDef().getWidth(); cellX++) {
                Cell regional5DegCell = regional5DegGrid.getCell(cellX, cellY);
                if (regional5DegCell != null && !regional5DegCell.isEmpty()) {
                    int cellX90D = (cellX * regional90DegGrid.getGridDef().getWidth()) / regional5DegGrid.getGridDef().getWidth();
                    int cellY90D = (cellY * regional90DegGrid.getGridDef().getHeight()) / regional5DegGrid.getGridDef().getHeight();
                    Cell regional90DCell = regional90DegGrid.getCellSafe(cellX90D, cellY90D);
                    regional90DCell.accumulate(regional5DegCell);
                }
            }
        }
        return regional90DegGrid;
    }

    private Cell aggregateToSingleValue(CellGrid regional5DegGrid, RegionMask regionMask) {
        // Check if region is Globe or Hemisphere (actually: check if we have fully covered
        // 90 deg grid boxes), if so apply special averaging for all 90 deg grid boxes.
        if (regionMask.getCoverage() == RegionMask.Coverage.Globe
                || regionMask.getCoverage() == RegionMask.Coverage.N_Hemisphere
                || regionMask.getCoverage() == RegionMask.Coverage.S_Hemisphere) {
            CellGrid regional90DegGrid = aggregate5DegTo90DegGrid(regional5DegGrid);
            return regional90DegGrid.aggregate(climatology.getSeaCoverage90DegGrid());
        } else {
            return regional5DegGrid.aggregate(climatology.getSeaCoverage5DegGrid());
        }
    }

    private List<Cell> aggregateMonthlyResults(List<OutputTimeStep> monthlyResults,
                                               RegionMaskList regionMaskList) {
        // todo - the following code is only valid for simple mean (SST, SST anomaly), but not for the uncertainties.
        // Aggregate these according to Nick's equations:
        // Variable A, Variable B, Variable C, Variable E, Variable F, Variable G  – according to Equation 1.1
        // Variable D  – according to Equation 1.2
        //
        ArrayList<Cell> combinedCells = new ArrayList<Cell>();
        for (int i = 0; i < regionMaskList.size(); i++) {
            Cell combinedCell = fileStore.getProductType().getFileType().getCellFactory().createCell();
            for (OutputTimeStep monthlyResult : monthlyResults) {
                Cell cell = monthlyResult.regionalAverages.get(i);
                combinedCell.accumulateAverage(cell, 1.0);
            }
            combinedCells.add(combinedCell);
        }
        return combinedCells;
    }

    private static CellGrid getRegionalGrid(CellGrid combined5DegGrid, RegionMask regionMask) {
        CellGrid regional5DGrid = new CellGrid(combined5DegGrid.getGridDef(), combined5DegGrid.getCellFactory());
        for (int cellY = 0; cellY < combined5DegGrid.getGridDef().getHeight(); cellY++) {
            for (int cellX = 0; cellX < combined5DegGrid.getGridDef().getWidth(); cellX++) {
                if (regionMask.getSampleBoolean(cellX, cellY)) {
                    Cell cell = combined5DegGrid.getCell(cellX, cellY);
                    if (cell != null && !cell.isEmpty()) {
                        regional5DGrid.setCell(cellX, cellY, cell.clone());
                    }
                }
            }
        }
        return regional5DGrid;
    }

    private static Grid[] readSourceGrids(ProductType productType, NetcdfFile netcdfFile, SstDepth sstDepth) throws IOException {
        long t0 = System.currentTimeMillis();
        LOGGER.fine("Reading source grid(s)...");
        Grid[] grids = productType.getFileType().readSourceGrids(netcdfFile, sstDepth);
        LOGGER.fine(String.format("Reading source grid(s) took %d ms", (System.currentTimeMillis() - t0)));
        return grids;
    }

}
