package org.esa.cci.sst.common;

import org.esa.cci.sst.common.auxiliary.Climatology;
import org.esa.cci.sst.common.auxiliary.LUT1;
import org.esa.cci.sst.common.calculator.CoverageUncertaintyProvider;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellAggregationCell;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.CellGrid;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.RegionMask;
import org.esa.cci.sst.common.file.FileStore;
import org.esa.cci.sst.common.file.FileType;
import org.esa.cci.sst.regavg.auxiliary.LUT2;
import org.esa.cci.sst.regrid.SpatialResolution;
import org.esa.cci.sst.tool.Tool;
import org.esa.cci.sst.util.UTC;
import ucar.nc2.NetcdfFile;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * {@author Bettina Scholze}
 * Date: 13.09.12 15:47
 */
public abstract class AbstractAggregator {
    protected static final Logger LOGGER = Tool.LOGGER;

    private final FileStore fileStore;
    private final FileType fileType;
    private final SstDepth sstDepth;
    private final Climatology climatology;
    private final LUT1 lut1;
    private final LUT2 lut2;

    public AbstractAggregator(FileStore fileStore,
                              Climatology climatology, LUT1 lut1, LUT2 lut2, SstDepth sstDepth) {

        this.fileStore = fileStore;
        this.fileType = fileStore.getProductType().getFileType();
        this.climatology = climatology;
        this.lut1 = lut1;
        this.lut2 = lut2;
        this.sstDepth = sstDepth;
    }

    abstract public List<? extends TimeStep> aggregate(Date startDate, Date endDate, TemporalResolution temporalResolution) throws IOException;

    // Hardly testable, because NetCDF file of given fileType required
    protected SpatialAggregationContext createAggregationCellContext(NetcdfFile netcdfFile) throws IOException {
        final Date date = fileType.readDate(netcdfFile);
        final int dayOfYear = UTC.getDayOfYear(date);
        LOGGER.fine("Day of year is " + dayOfYear);

        return new SpatialAggregationContext(fileStore.getProductType().getGridDef(),
                readSourceGrids(netcdfFile),
                climatology.getAnalysedSstGrid(dayOfYear),
                climatology.getSeaCoverageSourceGrid());
    }

    private Grid[] readSourceGrids(NetcdfFile netcdfFile) throws IOException {
        long t0 = System.currentTimeMillis();
        LOGGER.fine("Reading source grid(s)...");
        Grid[] grids = fileType.readSourceGrids(netcdfFile, sstDepth);
        LOGGER.fine(String.format("Reading source grid(s) took %d ms", (System.currentTimeMillis() - t0)));
        return grids;
    }

    protected static <C extends SpatialAggregationCell> void aggregateSources(SpatialAggregationContext aggregationContext,
                                                                              RegionMask regionMask, CellGrid<C> cellGrid) {

        final GridDef sourceGridDef = aggregationContext.getSourceGridDef();
        final int width = regionMask.getWidth();
        final int height = regionMask.getHeight();
        for (int cellY = 0; cellY < height; cellY++) {
            for (int cellX = 0; cellX < width; cellX++) {
                if (regionMask.getSampleBoolean(cellX, cellY)) {
                    Rectangle2D lonLatRectangle = regionMask.getGridDef().getLonLatRectangle(cellX, cellY);
                    Rectangle sourceGridRectangle = sourceGridDef.getGridRectangle(lonLatRectangle);
                    SpatialAggregationCell cell = cellGrid.getCellSafe(cellX, cellY);
                    cell.accumulate(aggregationContext, sourceGridRectangle);
                }
            }
        }
    }

    protected static <CSource extends AggregationCell, CTarget extends CellAggregationCell> CellGrid<CTarget> aggregateCellGridToCoarserCellGrid(
            CellGrid<CSource> cellSourceGrid, Grid seaCoverageGridInSourceResolution, CellGrid<CTarget> cellTargetGrid) {

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

    protected CoverageUncertaintyProvider createCoverageUncertaintyProvider(Date date, SpatialResolution spatialResolution) {
        int month = UTC.createCalendar(date).get(Calendar.MONTH);

        return new CoverageUncertaintyProvider(month, spatialResolution) {
            @Override
            protected double getMagnitude5(int cellX, int cellY) {
                return lut1.getMagnitudeGrid5().getSampleDouble(cellX, cellY);
            }

            @Override
            protected double getExponent5(int cellX, int cellY) {
                return lut1.getExponentGrid5().getSampleDouble(cellX, cellY);
            }

            @Override
            protected double getMagnitude90(int cellX, int cellY, int month11) {
                return lut2.getMagnitude90(month11, cellX, cellY);
            }
        };
    }

    public FileStore getFileStore() {
        return fileStore;
    }

    public FileType getFileType() {
        return fileType;
    }

    public Climatology getClimatology() {
        return climatology;
    }
}
