package org.esa.cci.sst.regavg;

import org.esa.cci.sst.common.*;
import org.esa.cci.sst.common.auxiliary.Climatology;
import org.esa.cci.sst.common.auxiliary.LUT1;
import org.esa.cci.sst.common.calculator.CoverageUncertaintyProvider;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellAggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.CellGrid;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.RegionMask;
import org.esa.cci.sst.common.file.FileStore;
import org.esa.cci.sst.common.file.FileType;
import org.esa.cci.sst.regavg.auxiliary.LUT2;
import org.esa.cci.sst.regrid.SpatialResolution;
import org.esa.cci.sst.util.UTC;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The one and only aggregator for the RegionalAveraging Tool.
 * <p/>
 * {@author Bettina Scholze}
 * Date: 13.09.12 16:29
 */
public class Aggregator4Regav extends AbstractAggregator {

    private final RegionMaskList regionMaskList;
    private RegionMask combinedRegionMask;

    public Aggregator4Regav(RegionMaskList regionMaskList, FileStore fileStore, Climatology climatology, LUT1 lut1, LUT2 lut2, SstDepth sstDepth) {
        super(fileStore, climatology, lut1, lut2, sstDepth);
        this.regionMaskList = regionMaskList;
        this.combinedRegionMask = RegionMask.combine(regionMaskList);
    }

    @Override
    public List<AveragingTimeStep> aggregate(Date startDate, Date endDate, TemporalResolution temporalResolution) throws IOException {
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

    private List<RegionalAggregation> aggregateRegions(Date date1, Date date2) throws IOException {
        //Compute the cell 5 grid for *all* combined regions first
        //regrid spatially from 0.1/0.5 ° to 5 °, and aggregate given time range (<= monthly)
        SpatialResolution targetResolution = SpatialResolution.DEGREE_5_00;
        final ArrayList<CellGrid<? extends AggregationCell>> combinedCell5Grids = aggregateTimeRangeAndRegrid(date1, date2);

        CoverageUncertaintyProvider coverageUncertaintyProvider = createCoverageUncertaintyProvider(date1, targetResolution);
        FileType.CellTypes cell90Type = FileType.CellTypes.CELL_90.setCoverageUncertaintyProvider(coverageUncertaintyProvider);

        return aggregateRegions(combinedCell5Grids,
                regionMaskList,
                getFileType().getSameMonthAggregationFactory(),
                getFileType().getCellFactory(cell90Type),
                getClimatology().getSeaCoverageCell5Grid(),
                getClimatology().getSeaCoverageCell90Grid());
    }

    //For the RegionalAveraging Tool
    private ArrayList<CellGrid<? extends AggregationCell>> aggregateTimeRangeAndRegrid(Date date1, Date date2) throws IOException {
        //todo check if time range is less or equal a month
        final List<File> fileList = getFileStore().getFiles(date1, date2);
        if (fileList.isEmpty()) {
            throw new IOException("No matching files found in " + Arrays.toString(getFileStore().getInputPaths()) + " for period " +
                    SimpleDateFormat.getDateInstance().format(date1) + " - " + SimpleDateFormat.getDateInstance().format(date1));
        }

        LOGGER.info(String.format("Computing output time step from %s to %s, %d file(s) found.",
                UTC.getIsoFormat().format(date1), UTC.getIsoFormat().format(date2), fileList.size()));

        SpatialResolution targetResolution = SpatialResolution.DEGREE_5_00;
        GridDef globalGridDef5 = GridDef.createGlobal(targetResolution.getValue());
        GridDef globalGridDef1 = GridDef.createGlobal(SpatialResolution.DEGREE_1_00.getValue());
        final CoverageUncertaintyProvider coverageUncertaintyProvider = createCoverageUncertaintyProvider(date1, targetResolution);

        FileType.CellTypes cellType = FileType.CellTypes.SPATIAL_CELL_5.setCoverageUncertaintyProvider(coverageUncertaintyProvider);
        final CellFactory<SpatialAggregationCell> spatialCellFactory = getFileType().getCellFactory(cellType);
        final CellFactory<SpatialAggregationCell> synopticCell1Factory = getFileType().getCellFactory(FileType.CellTypes.SYNOPTIC_CELL_1);
        final CellFactory<CellAggregationCell> synopticCell5Factory = getFileType().getCellFactory(FileType.CellTypes.SYNOPTIC_CELL_5);
        final CellGrid<SpatialAggregationCell> cellGridSpatial = new CellGrid<SpatialAggregationCell>(globalGridDef5, spatialCellFactory);
        final CellGrid<SpatialAggregationCell> cellGridSynoptic1 = new CellGrid<SpatialAggregationCell>(globalGridDef1, synopticCell1Factory);
        final CellGrid<CellAggregationCell> cellGridSynoptic5 = new CellGrid<CellAggregationCell>(globalGridDef5, synopticCell5Factory);

        for (File file : fileList) { //loop time (fileList contains files in required time range)
            LOGGER.info(String.format("Processing input %s file '%s'", getFileStore().getProductType(), file));
            long t0 = System.currentTimeMillis();
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            try {
                SpatialAggregationContext aggregationCellContext = createAggregationCellContext(netcdfFile);
                LOGGER.fine("Aggregating grid(s)...");
                long t01 = System.currentTimeMillis();

                aggregateSources(aggregationCellContext, combinedRegionMask, cellGridSpatial);
                if (getFileType().hasSynopticUncertainties()) {
                    //aggregate to synoptic areas (1 °, monthly)
                    aggregateSources(aggregationCellContext, combinedRegionMask, cellGridSynoptic1);
                    //1 ° -> 5 °
                    aggregateCellGridToCoarserCellGrid(cellGridSynoptic1, getClimatology().getSeaCoverageCell1Grid(), cellGridSynoptic5); //todo ??? climatology resolution ???
                }

                LOGGER.fine(String.format("Aggregating grid(s) took %d ms", (System.currentTimeMillis() - t01)));
            } finally {
                netcdfFile.close();
            }
            LOGGER.fine(String.format("Processing input %s file took %d ms", getFileStore().getProductType(),
                    System.currentTimeMillis() - t0));
        }

        ArrayList<CellGrid<? extends AggregationCell>> arrayGrids = new ArrayList<CellGrid<? extends AggregationCell>>();
        arrayGrids.add(cellGridSpatial);
        arrayGrids.add(cellGridSynoptic5);
        return arrayGrids;
    }


    List<RegionalAggregation> aggregateRegions(ArrayList<CellGrid<? extends AggregationCell>> combinedCell5Grids,
                                               RegionMaskList regionMaskList,
                                               AggregationFactory<SameMonthAggregation> aggregationFactory,
                                               CellFactory<CellAggregationCell> cell90Factory,
                                               Grid seaCoverageCell5Grid,
                                               Grid seaCoverageCell90Grid) {

        final boolean hasSynopticUncertainties = getFileType().hasSynopticUncertainties();
        final CellGrid<? extends AggregationCell> combinedCell5Grid = combinedCell5Grids.get(0);
        final List<RegionalAggregation> regionalAggregations = new ArrayList<RegionalAggregation>();

        for (RegionMask regionMask : regionMaskList) {
            // Extract the cell 5 grid for the current region
            CellGrid<? extends AggregationCell> cell5Grid = getCell5GridForRegion(combinedCell5Grid, regionMask);
            // Same for synoptic grid
            CellGrid<? extends AggregationCell> cell5GridSynoptic = null;
            if (hasSynopticUncertainties) {
                CellGrid<? extends AggregationCell> combinedCell5GridSynoptic = combinedCell5Grids.get(1);
                cell5GridSynoptic = getCell5GridForRegion(combinedCell5GridSynoptic, regionMask);
            }
            // Check if region is Globe or Hemisphere, if so apply special averaging for all 90 deg grid boxes.
            final SameMonthAggregation aggregation = aggregationFactory.createAggregation();
            boolean mustAggregateTo90 = mustAggregateTo90(regionMask);
            if (mustAggregateTo90) {
                final CellGrid<CellAggregationCell> cell90Grid = new CellGrid<CellAggregationCell>(GridDef.createGlobal(90.0), cell90Factory);
                // aggregateCell5GridToCell90Grid
                aggregateCellGridToCoarserCellGrid(cell5Grid, seaCoverageCell5Grid, cell90Grid);
                if (hasSynopticUncertainties) {
                    aggregateCellGridToCoarserCellGrid(cell5GridSynoptic, seaCoverageCell5Grid, cell90Grid);
                }
                // Removes spatial extent
                aggregateCell5OrCell90Grid(cell90Grid, seaCoverageCell90Grid, aggregation);
            } else {
                // Removes spatial extent
                aggregateCell5OrCell90Grid(cell5Grid, seaCoverageCell5Grid, aggregation);
                if (hasSynopticUncertainties) {
                    aggregateCell5OrCell90Grid(cell5GridSynoptic, seaCoverageCell5Grid, aggregation);
                }
            }
            regionalAggregations.add(aggregation);
        }
        return regionalAggregations;
    }


    static boolean mustAggregateTo90(RegionMask regionMask) {
        return regionMask.getCoverage() == RegionMask.Coverage.Globe
                || regionMask.getCoverage() == RegionMask.Coverage.N_Hemisphere
                || regionMask.getCoverage() == RegionMask.Coverage.S_Hemisphere;
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

    static <C extends AggregationCell> CellGrid<C> getCell5GridForRegion(CellGrid<C> combinedGrid5, RegionMask regionMask) {

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

    private List<RegionalAggregation> aggregateMonthlyTimeSteps(List<AveragingTimeStep> monthlyTimeSteps) {

        return aggregateMonthlyTimeSteps(monthlyTimeSteps,
                regionMaskList.size(), getFileType().getMultiMonthAggregationFactory());
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
}
