package org.esa.cci.sst.common;

import org.esa.cci.sst.common.auxiliary.Climatology;
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
 * The base class for the averaging and regridding aggregators.
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public abstract class AbstractAggregator {

    private static final Logger LOGGER = Logger.getLogger("org.esa.cci.sst");

    private final FileStore fileStore;
    private final Climatology climatology;
    private final SstDepth sstDepth;
    private final FileType fileType;

    protected AbstractAggregator(FileStore fileStore, Climatology climatology, SstDepth sstDepth) {
        this.fileStore = fileStore;
        this.climatology = climatology;
        this.sstDepth = sstDepth;
        this.fileType = fileStore.getProductType().getFileType();
    }

    abstract public List<? extends TimeStep> aggregate(
            Date startDate, Date endDate, TemporalResolution temporalResolution) throws IOException, ToolException;

    protected SpatialAggregationContext createSpatialAggregationContext(NetcdfFile file) throws IOException {
        final Date date = fileType.readDate(file);
        final int dayOfYear = UTC.getDayOfYear(date);
        LOGGER.fine("Day of year is " + dayOfYear);

        long t0 = System.currentTimeMillis();
        LOGGER.fine("Reading source grid(s)...");
        final Grid[] sourceGrids = fileType.readSourceGrids(file, sstDepth);
        LOGGER.fine(String.format("Reading source grid(s) took %d ms", (System.currentTimeMillis() - t0)));

        return new SpatialAggregationContext(fileType.getGridDef(),
                                             sourceGrids,
                                             climatology.getSst(dayOfYear),
                                             climatology.getSeaCoverage());
    }

    protected static <C extends SpatialAggregationCell> void aggregateSourcePixels(SpatialAggregationContext context,
                                                                                   RegionMask regionMask,
                                                                                   CellGrid<C> cellGrid) {
        final GridDef sourceGridDef = context.getSourceGridDef();
        final int w = regionMask.getWidth();
        final int h = regionMask.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (regionMask.getSampleBoolean(x, y)) {
                    final Rectangle2D lonLatRectangle = regionMask.getGridDef().getLonLatRectangle(x, y);
                    final Rectangle gridRectangle = sourceGridDef.getGridRectangle(lonLatRectangle);
                    final SpatialAggregationCell cell = cellGrid.getCellSafe(x, y);

                    cell.accumulate(context, gridRectangle);
                }
            }
        }
    }

    protected final FileStore getFileStore() {
        return fileStore;
    }

    protected final Climatology getClimatology() {
        return climatology;
    }

    protected final FileType getFileType() {
        return fileType;
    }
}
