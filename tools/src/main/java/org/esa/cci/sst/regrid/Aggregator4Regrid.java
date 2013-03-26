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

package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.*;
import org.esa.cci.sst.common.auxiliary.Climatology;
import org.esa.cci.sst.common.calculator.CoverageUncertaintyForRegridding;
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
import org.esa.cci.sst.regrid.auxiliary.LutForStdDeviation;
import org.esa.cci.sst.regrid.auxiliary.LutForSynopticAreas;
import org.esa.cci.sst.tool.ExitCode;
import org.esa.cci.sst.tool.ToolException;
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
    private final LutX0 lutCuTime;
    private final LutX0 lutCuSpace;
    private final LutForSynopticAreas lutForSynopticAreas;

    public Aggregator4Regrid(RegionMaskList regionMaskList, FileStore fileStore, Climatology climatology,
                             LutForSynopticAreas lutForSynopticAreas, LutForStdDeviation lutCuStddev, LutX0 lutCuTime, LutX0 lutCuSpace,
                             SstDepth sstDepth, double minCoverage, SpatialResolution spatialTargetResolution) {

        super(fileStore, climatology, lutCuStddev, sstDepth);
        this.combinedRegionMask = RegionMask.combine(regionMaskList);
        this.spatialTargetResolution = spatialTargetResolution;
        this.lutCuTime = lutCuTime;
        this.lutCuSpace = lutCuSpace;
        this.lutForSynopticAreas = lutForSynopticAreas;
        FileType.CellTypes.setMinCoverage(minCoverage);
    }

    @Override
    public List<RegriddingTimeStep> aggregate(Date startDate, Date endDate, TemporalResolution temporalResolution) throws IOException, ToolException {
        final List<RegriddingTimeStep> resultGridList = new ArrayList<RegriddingTimeStep>();
        final Calendar calendar = UTC.createCalendar(startDate);

        while (calendar.getTime().before(endDate) || calendar.getTime().equals(endDate)) {
            Date date1 = calendar.getTime();
            CellGrid<? extends AggregationCell> resultGrid;
            if (temporalResolution == TemporalResolution.daily) {
                calendar.add(Calendar.DATE, 1);
                Date date2 = calendar.getTime();
                resultGrid = aggregateTimeRangeAndRegrid(date1, date2, spatialTargetResolution, temporalResolution);
            } else if (temporalResolution == TemporalResolution.weekly5d) {
                calendar.add(Calendar.DATE, 5);
                Date date2 = calendar.getTime();
                resultGrid = aggregateTimeRangeAndRegrid(date1, date2, spatialTargetResolution, temporalResolution);
            } else if (temporalResolution == TemporalResolution.weekly7d) {
                calendar.add(Calendar.DATE, 7);
                Date date2 = calendar.getTime();
                resultGrid = aggregateTimeRangeAndRegrid(date1, date2, spatialTargetResolution, temporalResolution);
            } else if (temporalResolution == TemporalResolution.monthly) {
                calendar.add(Calendar.MONTH, 1);
                Date date2 = calendar.getTime();
                resultGrid = aggregateTimeRangeAndRegrid(date1, date2, spatialTargetResolution, temporalResolution);
            } else if (temporalResolution == TemporalResolution.seasonal) {
                calendar.add(Calendar.MONTH, 3);
                Date date2 = calendar.getTime();
                List<? extends TimeStep> monthlyTimeSteps = aggregate(date1, date2, TemporalResolution.monthly);
                resultGrid = aggregateMultiMonths(monthlyTimeSteps);
            } else if (temporalResolution == TemporalResolution.annual) {
                calendar.add(Calendar.YEAR, 1);
                Date date2 = calendar.getTime();
                List<? extends TimeStep> monthlyTimeSteps = aggregate(date1, date2, TemporalResolution.monthly);
                resultGrid = aggregateMultiMonths(monthlyTimeSteps);
            } else {
                throw new ToolException("Not supported: " + temporalResolution.toString(), ExitCode.USAGE_ERROR);
            }
            if (resultGrid != null) {
                resultGridList.add(new RegriddingTimeStep(date1, calendar.getTime(), resultGrid));
            }
        }
        return resultGridList;
    }

    CellGrid<SpatialAggregationCell> aggregateTimeRangeAndRegrid(Date date1, Date date2,
                                                                 SpatialResolution spatialResolution,
                                                                 TemporalResolution temporalResolution) throws IOException {

        final List<File> fileList = getFileStore().getFiles(date1, date2);
        if (fileList.isEmpty()) {
            LOGGER.warning("No matching files found in " + Arrays.toString(getFileStore().getInputPaths()) + " for period " +
                    SimpleDateFormat.getDateInstance().format(date1) + " - " + SimpleDateFormat.getDateInstance().format(date2));
            return null;
        }
        LOGGER.info(String.format("Computing output time step from %s to %s, %d file(s) found.",
                UTC.getIsoFormat().format(date1), UTC.getIsoFormat().format(date2), fileList.size()));

        final CellGrid<SpatialAggregationCell> regriddingCellGrid = prepareSpatialAggregationCellCellGrid(date1,
                spatialResolution, temporalResolution);

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

    CellGrid<SpatialAggregationCell> prepareSpatialAggregationCellCellGrid(Date date1, SpatialResolution spatialResolution, TemporalResolution temporalResolution) {
        GridDef gridDef = GridDef.createGlobal(spatialResolution.getResolution());

        FileType.CellTypes cellType = FileType.CellTypes.SPATIAL_CELL_REGRIDDING;
        temporalResolution.setDate1(date1);
        cellType.setCoverageUncertaintyProvider(createCoverageUncertaintyProvider(temporalResolution, spatialResolution));
        if (getFileType().hasSynopticUncertainties()) {
            cellType.setSynopticAreaCountEstimator(new SynopticAreaCountEstimator(lutForSynopticAreas));
        }
        final CellFactory<SpatialAggregationCell> regriddingCellFactory = getFileType().getCellFactory(cellType);
        return new CellGrid<SpatialAggregationCell>(gridDef, regriddingCellFactory);
    }

    CellGrid<AggregationCell> aggregateMultiMonths(List<? extends TimeStep> monthlyTimeSteps) {

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

    private CoverageUncertaintyForRegridding createCoverageUncertaintyProvider(TemporalResolution temporalResolution,
                                                                               SpatialResolution spatialResolution) {

        return new CoverageUncertaintyForRegridding(temporalResolution, spatialResolution, lutCuTime, lutCuSpace);
    }

}
