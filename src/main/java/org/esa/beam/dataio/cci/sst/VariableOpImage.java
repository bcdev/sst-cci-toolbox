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
                              Dimension tileSize) {
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
        // sequence of dimensions in OSI & PMW (..., x, y) does not comply with CF conventions
        final int indexX = getIndexX(rank);
        final int indexY = getIndexY(rank);

        shape[indexY] = rectangle.height;
        shape[indexX] = rectangle.width;

        origin[indexY] = rectangle.y;
        origin[indexX] = rectangle.x;

        final double scale = getScale();
        stride[indexY] = (int) scale;
        stride[indexX] = (int) scale;

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
        tile.setDataElements(rectangle.x, rectangle.y, rectangle.width, rectangle.height, getStorage(array));
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
     * Returns the primitive storage of the array supplied as argument. May
     * be overridden in order to e.g. transpose the storage.
     *
     * @param array An array.
     *
     * @return the array primitive storage.
     */
    protected abstract Object getStorage(Array array);
}
