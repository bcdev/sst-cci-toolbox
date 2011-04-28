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
class VariableOpImage extends SingleBandedOpImage {

    private final VariableIF variable;

    VariableOpImage(VariableIF variable, int dataBufferType, int sourceWidth, int sourceHeight, Dimension tileSize) {
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
        tile.setDataElements(rectangle.x, rectangle.y, rectangle.width, rectangle.height, getStorage(array));
    }

    /**
     * Returns the storage of the array supplied as argument. May
     * be overridden in order to e.g. transpose the storage.
     *
     * @param array An array.
     *
     * @return the transformed storage.
     */
    protected Object getStorage(Array array) {
        return array.getStorage();
    }
}
