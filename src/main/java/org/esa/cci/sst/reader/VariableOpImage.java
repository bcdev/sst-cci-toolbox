package org.esa.cci.sst.reader;

import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Variable;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.io.IOException;

/**
 * Used for creating rendered images from a netCDF variable (based on BEAM
 * implementation, but specialized and simplified).
 *
 * @author Ralf Quast
 */
class VariableOpImage extends SingleBandedOpImage {

    private final Variable variable;

    VariableOpImage(Variable variable, int dataBufferType, int sourceWidth, int sourceHeight, Dimension tileSize) {
        super(dataBufferType, sourceWidth, sourceHeight, tileSize, null, ResolutionLevel.MAXRES);
        this.variable = variable;
    }

    @Override
    protected final void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle rectangle) {
        final int rank = variable.getRank();
        final int[] origin = new int[rank];
        final int[] shape = new int[rank];
        final int[] stride = new int[rank];
        for (int i = 0; i < rank; i++) {
            shape[i] = 1;
            origin[i] = 0;
            stride[i] = 1;
        }
        final int xIndex = rank - 2;
        final int yIndex = rank - 1;

        shape[yIndex] = rectangle.height;
        shape[xIndex] = rectangle.width;

        origin[yIndex] = rectangle.y;
        origin[xIndex] = rectangle.x;

        double scale = getScale();
        stride[yIndex] = (int) scale;
        stride[xIndex] = (int) scale;

        Array array;
        synchronized (variable.getParentGroup().getNetcdfFile()) {
            try {
                final Section section = new Section(origin, shape, stride);
                array = variable.read(section);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } catch (InvalidRangeException e) {
                throw new IllegalArgumentException(e);
            }
        }
        tile.setDataElements(rectangle.x, rectangle.y, rectangle.width, rectangle.height, transform(array));
    }

    /**
     * Transforms the storage of the netCDF array supplied as argument. This
     * implementation applies the identity transformation.
     *
     * @param array The netCDF array.
     *
     * @return the transformed storage.
     */
    protected Object transform(Array array) {
        return array.getStorage();
    }
}
