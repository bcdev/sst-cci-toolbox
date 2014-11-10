package org.esa.cci.sst.common;

import org.esa.cci.sst.common.auxiliary.Climatology;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.CellGrid;
import org.esa.cci.sst.common.cellgrid.RegionMask;
import org.esa.cci.sst.common.file.FileStore;
import org.esa.cci.sst.common.file.FileType;
import ucar.nc2.NetcdfFile;

import java.awt.Rectangle;
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
            Date startDate, Date endDate, TemporalResolution temporalResolution) throws IOException;

    protected final void readSourceGrids(NetcdfFile dataFile, AggregationContext context) throws IOException {
        final long t0 = System.currentTimeMillis();
        LOGGER.fine("Reading source grid(s)...");
        fileType.readSourceGrids(dataFile, sstDepth, context);
        final long t1 = System.currentTimeMillis();
        LOGGER.fine(String.format("Reading source grid(s) took %d ms", t1 - t0));
    }

    protected static <C extends SpatialAggregationCell> void aggregateSourcePixels(AggregationContext context,
                                                                                   RegionMask regionMask,
                                                                                   CellGrid<C> targetGrid) {
        final GridDef sourceGridDef = context.getSourceGridDef();
        final GridDef targetGridDef = regionMask.getGridDef();

        final int w = regionMask.getWidth();
        final int h = regionMask.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (regionMask.getSampleBoolean(x, y)) {
                    final Rectangle sourceRectangle = sourceGridDef.getGridRectangle(x, y, targetGridDef);
                    C targetCell = targetGrid.getCell(x, y);
                    if (targetCell != null) {
                        targetCell.accumulate(context, sourceRectangle);
                    } else {
                        targetCell = targetGrid.createCell(x, y);
                        targetCell.accumulate(context, sourceRectangle);
                        if (!targetCell.isEmpty()) {
                            targetGrid.setCell(targetCell);
                        }
                    }
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
