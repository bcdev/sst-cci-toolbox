package org.esa.beam.dataio.cci.sst;

import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.VariableIF;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.io.IOException;

/**
 * Used for creating rendered images from a variable in OSI and
 * PMW netCDF files.
 *
 * @author Ralf Quast
 * @see org.esa.beam.dataio.netcdf.util.NetcdfOpImage
 */
abstract class VariableOpImage extends SingleBandedOpImage {

    private final VariableIF variable;

    protected VariableOpImage(VariableIF variable, int dataBufferType, int sourceWidth, int sourceHeight,
                              Dimension tileSize, ResolutionLevel level) {
        super(dataBufferType, sourceWidth, sourceHeight, tileSize, null, level);
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
        final int indexX = getIndexX(rank);
        final int indexY = getIndexY(rank);

        shape[indexX] = getSourceWidth(rectangle.width);
        shape[indexY] = getSourceHeight(rectangle.height);

        origin[indexX] = getSourceX(rectangle.x);
        origin[indexY] = getSourceY(rectangle.y);

        final double scale = getScale();
        stride[indexX] = (int) scale;
        stride[indexY] = (int) scale;

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
        tile.setDataElements(rectangle.x, rectangle.y, rectangle.width, rectangle.height, transformStorage(array));
    }

    /**
     * Returns the index of the x dimension of the variable, which
     * provides the image data.
     *
     * @param rank The rank of the array, which contains the image
     *             data.
     *
     * @return the index of the x dimension.
     */
    protected abstract int getIndexX(int rank);

    /**
     * Returns the index of the y dimension of the variable, which
     * provides the image data.
     *
     * @param rank The rank of the array, which contains the image
     *             data.
     *
     * @return the index of the y dimension.
     */
    protected abstract int getIndexY(int rank);

    /**
     * Transforms the primitive storage of the array supplied as argument.
     * <p/>
     * The default implementation merely returns the primitive storage of
     * the array supplied as argument, which is fine when the sequence of
     * variable dimensions is (..., y, x).
     * <p/>
     * Implementations have to transpose the storage when the sequence of
     * variable dimensions is (..., x, y) instead of (..., y, x).
     * <p/>
     *
     * @param array An array.
     *
     * @return the transformed primitive storage of the array supplied as
     *         argument.
     */
    protected Object transformStorage(Array array) {
        return array.getStorage();
    }

}
