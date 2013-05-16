package org.esa.cci.sst.regavg;

import org.esa.cci.sst.common.AbstractAggregator;
import org.esa.cci.sst.common.AggregationContext;
import org.esa.cci.sst.common.AggregationFactory;
import org.esa.cci.sst.common.RegionMaskList;
import org.esa.cci.sst.common.RegionalAggregation;
import org.esa.cci.sst.common.SpatialResolution;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.TemporalResolution;
import org.esa.cci.sst.common.auxiliary.Climatology;
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
import org.esa.cci.sst.regavg.auxiliary.LUT1;
import org.esa.cci.sst.regavg.auxiliary.LUT2;
import org.esa.cci.sst.tool.ExitCode;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.util.UTC;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * The one and only aggregator for the RegionalAveraging Tool.
 * <p/>
 * {@author Bettina Scholze}
 * Date: 13.09.12 16:29
 */
public class AveragingAggregator extends AbstractAggregator {

    private static final Logger LOGGER = Logger.getLogger("org.esa.cci.sst");

    private final RegionMaskList regionMaskList;
    private final AggregationContext aggregationContext;
    private RegionMask combinedRegionMask;
    private LUT1 lut1;
    private LUT2 lut2;

    public AveragingAggregator(RegionMaskList regionMaskList, FileStore fileStore, Climatology climatology, LUT1 lut1,
                               LUT2 lut2, SstDepth sstDepth) {
        super(fileStore, climatology, sstDepth);
        this.lut1 = lut1;
        this.lut2 = lut2;
        this.regionMaskList = regionMaskList;
        this.combinedRegionMask = RegionMask.combine(regionMaskList);

        aggregationContext = new AggregationContext();
    }

    public static <CSource extends AggregationCell, CTarget extends CellAggregationCell> CellGrid<CTarget> aggregateCellGridToCoarserCellGrid(
            CellGrid<CSource> cellSourceGrid,
            Grid seaCoverageGridInSourceResolution,
            CellGrid<CTarget> cellTargetGrid) {
        final int width = cellSourceGrid.getGridDef().getWidth();
        final int height = cellSourceGrid.getGridDef().getHeight();
        for (int cellSourceY = 0; cellSourceY < height; cellSourceY++) {
            for (int cellSourceX = 0; cellSourceX < width; cellSourceX++) {
                CSource cellSource = cellSourceGrid.getCell(cellSourceX, cellSourceY);
                if (cellSource != null && !cellSource.isEmpty()) {
                    int cellTargetX = (cellSourceX * cellTargetGrid.getGridDef().getWidth()) / width;
                    int cellTargetY = (cellSourceY * cellTargetGrid.getGridDef().getHeight()) / height;
                    CTarget cellTarget = cellTargetGrid.getCellSafe(cellTargetX, cellTargetY);
                    double seaCoverage = seaCoverageGridInSourceResolution.getSampleDouble(cellSourceX, cellSourceY);
                    // noinspection unchecked
                    cellTarget.accumulate(cellSource, seaCoverage);
                }
            }
        }
        return cellTargetGrid;
    }

    @Override
    public List<AveragingTimeStep> aggregate(Date startDate, Date endDate, TemporalResolution temporalResolution) throws
                                                                                                                  IOException,
                                                                                                                  ToolException {
        final List<AveragingTimeStep> results = new ArrayList<AveragingTimeStep>();
        final Calendar calendar = UTC.createCalendar(startDate);

        while (calendar.getTime().before(endDate) || calendar.getTime().equals(endDate)) {
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
            } else if (temporalResolution == TemporalResolution.annual) {
                calendar.add(Calendar.YEAR, 1);
                Date date2 = calendar.getTime();
                List<AveragingTimeStep> monthlyTimeSteps = aggregate(date1, date2, TemporalResolution.monthly);
                result = aggregateMonthlyTimeSteps(monthlyTimeSteps);
            } else {
                throw new ToolException("Not supported: " + temporalResolution.toString(), ExitCode.USAGE_ERROR);
            }

            results.add(new AveragingTimeStep(date1, calendar.getTime(), result));
        }
        return results;
    }

    private List<RegionalAggregation> aggregateRegions(Date date1, Date date2) throws IOException {
        //Compute the cell 5 grid for *all* combined regions first
        //regrid spatially from 0.1/0.5 ° to 5 °, and aggregate given time range (<= monthly)
        SpatialResolution targetResolution = SpatialResolution.DEGREE_5_00;
        final ArrayList<CellGrid<? extends AggregationCell>> combinedCell5Grids = aggregateTimeRangeAndRegrid(date1,
                                                                                                              date2);

        final CoverageUncertaintyProvider coverageUncertaintyProvider = createCoverageUncertaintyProvider(date1);
        aggregationContext.setCoverageUncertaintyProvider(coverageUncertaintyProvider);

        return aggregateRegions(combinedCell5Grids,
                                regionMaskList,
                                getFileType().getSameMonthAggregationFactory(),
                                getFileType().getCellFactory90(aggregationContext),
                                getClimatology().getSeaCoverageCell5Grid(),
                                getClimatology().getSeaCoverageCell90Grid());
    }

    private ArrayList<CellGrid<? extends AggregationCell>> aggregateTimeRangeAndRegrid(Date date1, Date date2) throws
                                                                                                               IOException {
        //todo check if time range is less or equal a month
        final List<File> fileList = getFileStore().getFiles(date1, date2);

        LOGGER.info(String.format("Computing output time step from %s to %s, %d file(s) found.",
                                  UTC.getIsoFormat().format(date1), UTC.getIsoFormat().format(date2), fileList.size()));

        SpatialResolution targetResolution = SpatialResolution.DEGREE_5_00;
        GridDef globalGridDef5 = GridDef.createGlobal(targetResolution.getResolution());
        final CoverageUncertaintyProvider coverageUncertaintyProvider = createCoverageUncertaintyProvider(date1);
        aggregationContext.setCoverageUncertaintyProvider(coverageUncertaintyProvider);
        final CellFactory<SpatialAggregationCell> spatialCellFactory = getFileType().getCellFactory5(
                aggregationContext);
        final CellGrid<SpatialAggregationCell> spatialCellGrid = CellGrid.create(globalGridDef5, spatialCellFactory);

        for (File file : fileList) { //loop time (fileList contains files in required time range)
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

                aggregateSourcePixels(aggregationContext, combinedRegionMask, spatialCellGrid);

                LOGGER.fine(String.format("Aggregating grid(s) took %d ms", (System.currentTimeMillis() - t01)));
            } catch (IOException e) {
                LOGGER.warning(e.getMessage());
            } finally {
                dataFile.close();
            }
            LOGGER.fine(String.format("Processing input %s file took %d ms", getFileStore().getProductType(),
                                      System.currentTimeMillis() - t0));
        }

        final ArrayList<CellGrid<? extends AggregationCell>> cellGrids = new ArrayList<CellGrid<? extends AggregationCell>>();
        cellGrids.add(spatialCellGrid);
        return cellGrids;
    }


    List<RegionalAggregation> aggregateRegions(ArrayList<CellGrid<? extends AggregationCell>> combinedCell5Grids,
                                               RegionMaskList regionMaskList,
                                               AggregationFactory<SameMonthAggregation<AggregationCell>> aggregationFactory,
                                               CellFactory<CellAggregationCell<AggregationCell>> cell90Factory,
                                               Grid seaCoverageCell5Grid,
                                               Grid seaCoverageCell90Grid) {

        final CellGrid<? extends AggregationCell> combinedCell5Grid = combinedCell5Grids.get(0);
        final List<RegionalAggregation> regionalAggregations = new ArrayList<RegionalAggregation>();

        for (RegionMask regionMask : regionMaskList) {
            // Extract the cell 5 grid for the current region
            CellGrid<? extends AggregationCell> cell5Grid = getCell5GridForRegion(combinedCell5Grid, regionMask);
            // Same for synoptic grid
            CellGrid<? extends AggregationCell> cell5GridSynoptic = null;
            // Check if region is Globe or Hemisphere, if so apply special averaging for all 90 deg grid boxes.
            final SameMonthAggregation aggregation = aggregationFactory.createAggregation();
            boolean mustAggregateTo90 = mustAggregateTo90(regionMask);
            if (mustAggregateTo90) {
                final CellGrid<CellAggregationCell<AggregationCell>> cell90Grid = CellGrid.create(
                        GridDef.createGlobal(90.0), cell90Factory);
                // aggregateCell5GridToCell90Grid
                aggregateCellGridToCoarserCellGrid(cell5Grid, seaCoverageCell5Grid, cell90Grid);
                // Removes spatial extent
                aggregateCell5OrCell90Grid(cell90Grid, seaCoverageCell90Grid, aggregation);
            } else {
                // Removes spatial extent
                aggregateCell5OrCell90Grid(cell5Grid, seaCoverageCell5Grid, aggregation);
            }
            regionalAggregations.add(aggregation);
        }
        return regionalAggregations;
    }


    static boolean mustAggregateTo90(RegionMask regionMask) {
        return regionMask.getCoverage() == RegionMask.Coverage.GLOBE
               || regionMask.getCoverage() == RegionMask.Coverage.N_HEMISPHERE
               || regionMask.getCoverage() == RegionMask.Coverage.S_HEMISPHERE;
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

    static <C extends AggregationCell> CellGrid<C> getCell5GridForRegion(CellGrid<C> combinedGrid5,
                                                                         RegionMask regionMask) {

        final CellGrid<C> regionalGrid5 = CellGrid.create(combinedGrid5.getGridDef(), combinedGrid5.getCellFactory());
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
            AggregationFactory<MultiMonthAggregation<RegionalAggregation>> aggregationFactory) {

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

    protected CoverageUncertaintyProvider createCoverageUncertaintyProvider(Date date) {
        final int month = UTC.createCalendar(date).get(Calendar.MONTH);

        return new AveragingCoverageUncertaintyProvider(month) {
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
