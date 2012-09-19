package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.*;
import org.esa.cci.sst.common.auxiliary.Climatology;
import org.esa.cci.sst.common.auxiliary.LUT1;
import org.esa.cci.sst.common.calculator.SynopticAreaCountEstimator;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellAggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.CellGrid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.RegionMask;
import org.esa.cci.sst.common.file.FileStore;
import org.esa.cci.sst.common.file.FileType;
import org.esa.cci.sst.regavg.auxiliary.LUT2;
import org.esa.cci.sst.regrid.auxiliary.LUT3;
import org.esa.cci.sst.util.UTC;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The one and only aggregator for the Regridding Tool.
 * <p/>
 * {@author Bettina Scholze}
 * Date: 13.09.12 16:29
 */
public class Aggregator4Regrid extends AbstractAggregator {

    private RegionMask combinedRegionMask;
    private SpatialResolution spatialTargetResolution;
    private final LUT3 lut3;

    public Aggregator4Regrid(RegionMaskList regionMaskList, FileStore fileStore, Climatology climatology,
                             LUT1 lut1, LUT2 lut2, LUT3 lut3, SstDepth sstDepth,
                             double minCoverage, SpatialResolution spatialTargetResolution) {

        super(fileStore, climatology, lut1, lut2, sstDepth);
        this.combinedRegionMask = RegionMask.combine(regionMaskList);
        this.spatialTargetResolution = spatialTargetResolution;
        this.lut3 = lut3;
        FileType.CellTypes.setMinCoverage(minCoverage);
    }

    @Override
    public List<RegriddingTimeStep> aggregate(Date startDate, Date endDate, TemporalResolution temporalResolution) throws IOException {
        final List<RegriddingTimeStep> resultGridList = new ArrayList<RegriddingTimeStep>();
        final Calendar calendar = UTC.createCalendar(startDate);

        while (calendar.getTime().before(endDate)) {
            Date date1 = calendar.getTime();
            CellGrid<? extends AggregationCell> resultGrid;
            if (temporalResolution == TemporalResolution.daily) {
                calendar.add(Calendar.DATE, 1);
                Date date2 = calendar.getTime();
                resultGrid = aggregateTimeRangeAndRegrid(date1, date2, spatialTargetResolution);
            } else if (temporalResolution == TemporalResolution.monthly) {
                calendar.add(Calendar.MONTH, 1);
                Date date2 = calendar.getTime();
                resultGrid = aggregateTimeRangeAndRegrid(date1, date2, spatialTargetResolution);
            } else if (temporalResolution == TemporalResolution.seasonal) {
                calendar.add(Calendar.MONTH, 3);
                Date date2 = calendar.getTime();
                List<? extends TimeStep> monthlyTimeSteps = aggregate(date1, date2, TemporalResolution.monthly);
                resultGrid = aggregateMultiMonths(monthlyTimeSteps);
            } else /*if (temporalResolution == TemporalResolution.annual)*/ {
                calendar.add(Calendar.YEAR, 1);
                Date date2 = calendar.getTime();
                List<? extends TimeStep> monthlyTimeSteps = aggregate(date1, date2, TemporalResolution.monthly);
                resultGrid = aggregateMultiMonths(monthlyTimeSteps);
            }
            if (resultGrid != null) {
                resultGridList.add(new RegriddingTimeStep(date1, calendar.getTime(), resultGrid));
            }
        }
        return resultGridList;
    }

    private CellGrid<SpatialAggregationCell> aggregateTimeRangeAndRegrid(Date date1, Date date2,
                                                                         SpatialResolution spatialResolution) throws IOException {
        //todo bs: check if time range is less or equal a month
        final List<File> fileList = getFileStore().getFiles(date1, date2);
        if (fileList.isEmpty()) {
            LOGGER.warning("No matching files found in " + Arrays.toString(getFileStore().getInputPaths()) + " for period " +
                    SimpleDateFormat.getDateInstance().format(date1) + " - " + SimpleDateFormat.getDateInstance().format(date2));
            return null;
        }
        LOGGER.info(String.format("Computing output time step from %s to %s, %d file(s) found.",
                UTC.getIsoFormat().format(date1), UTC.getIsoFormat().format(date2), fileList.size()));

        GridDef gridDef = GridDef.createGlobal(spatialResolution.getValue());
        FileType.CellTypes cellType = FileType.CellTypes.SPATIAL_CELL_REGRIDDING;
        cellType.setCoverageUncertaintyProvider(createCoverageUncertaintyProvider(date1, spatialResolution));
        if (getFileType().hasSynopticUncertainties()) {
            cellType.setSynopticAreaCountEstimator(createSynopticAreaCountEstimator(spatialResolution));
        }
        final CellFactory<SpatialAggregationCell> regriddingCellFactory = getFileType().getCellFactory(cellType);
        final CellGrid<SpatialAggregationCell> regriddingCellGrid = new CellGrid<SpatialAggregationCell>(gridDef, regriddingCellFactory);

        for (File file : fileList) { //loop time (fileList contains files in required time range)
            LOGGER.info(String.format("Processing input %s file '%s'", getFileStore().getProductType(), file));
            long t0 = System.currentTimeMillis();
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            try {
                SpatialAggregationContext aggregationCellContext = createAggregationCellContext(netcdfFile);
                LOGGER.fine("Aggregating grid(s)...");
                long t01 = System.currentTimeMillis();
                aggregateSources(aggregationCellContext, combinedRegionMask, regriddingCellGrid);
                LOGGER.fine(String.format("Aggregating grid(s) took %d ms", (System.currentTimeMillis() - t01)));
            } finally {
                netcdfFile.close();
            }
            LOGGER.fine(String.format("Processing input %s file took %d ms", getFileStore().getProductType(),
                    System.currentTimeMillis() - t0));
        }

        return regriddingCellGrid;
    }

    private CellGrid<AggregationCell> aggregateMultiMonths(List<? extends TimeStep> monthlyTimeSteps) {

        final CellFactory<AggregationCell> cellFactory = getFileType().getCellFactory(FileType.CellTypes.TEMPORAL_CELL);
        GridDef gridDef = ((RegriddingTimeStep) monthlyTimeSteps.get(0)).getCellGrid().getGridDef();
        final CellGrid<AggregationCell> cellGrid = new CellGrid<AggregationCell>(gridDef, cellFactory);

        for (TimeStep timeStep : monthlyTimeSteps) {
            RegriddingTimeStep regriddingTimeStep = (RegriddingTimeStep) timeStep;
            int height = cellGrid.getHeight();
            int width = cellGrid.getWidth();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    CellAggregationCell cell = (CellAggregationCell) cellGrid.getCellSafe(x, y);
                    AggregationCell cellFromTimeStep = regriddingTimeStep.getCellGrid().getCell(x, y);
                    cell.accumulate(cellFromTimeStep, 1);
                }
            }
        }
        return cellGrid;
    }

    private SynopticAreaCountEstimator createSynopticAreaCountEstimator(SpatialResolution spatialResolution) {
        return new SynopticAreaCountEstimator(spatialResolution) {

            @Override
            public double getDxy(int x, int y) {
               return lut3.getGridDxy().getSampleDouble(x, y);
            }

            @Override
            public double getDt(int x, int y) {
                return lut3.getGridDt().getSampleDouble(x, y);
            }
        };
    }
}
