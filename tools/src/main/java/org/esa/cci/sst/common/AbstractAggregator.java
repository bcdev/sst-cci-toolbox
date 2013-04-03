package org.esa.cci.sst.common;

import org.esa.cci.sst.common.auxiliary.Climatology;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellAggregationCell;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.CellGrid;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.RegionMask;
import org.esa.cci.sst.common.file.FileStore;
import org.esa.cci.sst.common.file.FileType;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.util.UTC;
import ucar.nc2.NetcdfFile;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * The base class for the averaging and re-gridding aggregators.
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public abstract class AbstractAggregator {

    private static final Logger LOGGER = Logger.getLogger("org.esa.cci.sst");

    private final FileStore fileStore;
    private final LUT regriddingLUT1;
    private FileType fileType;
    private final SstDepth sstDepth;
    private final Climatology climatology;

    protected AbstractAggregator(FileStore fileStore, Climatology climatology, LUT regriddingLUT1, SstDepth sstDepth) {
        this.fileStore = fileStore;
        this.climatology = climatology;
        this.regriddingLUT1 = regriddingLUT1;
        this.sstDepth = sstDepth;
        if (fileStore != null) {
            this.fileType = fileStore.getProductType().getFileType();
        }
    }

    abstract public List<? extends TimeStep> aggregate(
            Date startDate, Date endDate, TemporalResolution temporalResolution) throws IOException, ToolException;

    protected final SpatialAggregationContext createAggregationCellContext(NetcdfFile netcdfFile) throws IOException {
        final Date date = fileType.readDate(netcdfFile);
        final int dayOfYear = UTC.getDayOfYear(date);
        LOGGER.fine("Day of year is " + dayOfYear);

        final Grid[] sourceGrids = readSourceGrids(netcdfFile);

        return new SpatialAggregationContext(fileStore.getProductType().getGridDef(),
                                             sourceGrids,
                                             climatology.getSst(dayOfYear),
                                             climatology.getSeaCoverage(),
                                             regriddingLUT1 == null ? null : regriddingLUT1.getGrid());
    }

    private Grid[] readSourceGrids(NetcdfFile netcdfFile) throws IOException {
        long t0 = System.currentTimeMillis();
        LOGGER.fine("Reading source grid(s)...");
        final Grid[] grids = fileType.readSourceGrids(netcdfFile, sstDepth);
        LOGGER.fine(String.format("Reading source grid(s) took %d ms", (System.currentTimeMillis() - t0)));

        return grids;
    }

    protected static <C extends SpatialAggregationCell> void aggregateSources(SpatialAggregationContext context,
                                                                              RegionMask regionMask,
                                                                              CellGrid<C> cellGrid) {
        final GridDef sourceGridDef = context.getSourceGridDef();
        final int width = regionMask.getWidth();
        final int height = regionMask.getHeight();
        for (int cellY = 0; cellY < height; cellY++) {
            for (int cellX = 0; cellX < width; cellX++) {
                if (regionMask.getSampleBoolean(cellX, cellY)) {
                    final Rectangle2D lonLatRectangle = regionMask.getGridDef().getLonLatRectangle(cellX, cellY);
                    final Rectangle sourceGridRectangle = sourceGridDef.getGridRectangle(lonLatRectangle);
                    final SpatialAggregationCell cell = cellGrid.getCellSafe(cellX, cellY);
                    cell.accumulate(context, sourceGridRectangle);
                }
            }
        }
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
