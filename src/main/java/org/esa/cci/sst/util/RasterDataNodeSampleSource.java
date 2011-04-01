package org.esa.cci.sst.util;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import javax.media.jai.PlanarImage;
import java.awt.image.Raster;

/**
 * A simple facade wrapping a {@link RasterDataNode}.
 *
 * @author Ralf Quast
 */
public class RasterDataNodeSampleSource implements SampleSource {

    private final RasterDataNode node;

    /**
     * Construct a new instance of this class.
     *
     * @param node The raster data node to be wrapped.
     */
    public RasterDataNodeSampleSource(RasterDataNode node) {
        this.node = node;
    }

    /**
     * Returns the scene raster width of the wrapped node.
     *
     * @return the scene raster width of the wrapped node.
     */
    @Override
    public int getWidth() {
        return node.getSceneRasterWidth();
    }

    /**
     * Returns the scene raster height of the wrapped node.
     *
     * @return the scene raster height of the wrapped node.
     */
    @Override
    public int getHeight() {
        return node.getSceneRasterHeight();
    }

    @Override
    public double getSample(int x, int y) {
        return getGeophysicalSampleDouble(x, y, 0);
    }

    private double getGeophysicalSampleDouble(int x, int y, int level) {
        // this code is copied from {@code org.esa.beam.util.ProductUtils#getGeophysicalSampleDouble}
        final PlanarImage image = ImageManager.getInstance().getSourceImage(node, level);
        final int tileX = image.XToTileX(x);
        final int tileY = image.YToTileY(y);
        final Raster data = image.getTile(tileX, tileY);
        if (data == null) {
            return Double.NaN;
        }

        final double sample;
        if (node.getDataType() == ProductData.TYPE_INT8) {
            sample = (byte) data.getSample(x, y, 0);
        } else if (node.getDataType() == ProductData.TYPE_UINT32) {
            sample = data.getSample(x, y, 0) & 0xFFFFFFFFL;
        } else {
            sample = data.getSampleDouble(x, y, 0);
        }
        if (node.isScalingApplied()) {
            return node.scale(sample);
        }
        return sample;
    }

}
