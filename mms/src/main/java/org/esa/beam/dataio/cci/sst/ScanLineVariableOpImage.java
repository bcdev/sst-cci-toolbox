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
 * Used for creating rendered images from a scan-line variable in
 * netCDF files.
 *
 * @author Ralf Quast
 */
abstract class ScanLineVariableOpImage extends SingleBandedOpImage {

    private final VariableIF variable;

    ScanLineVariableOpImage(VariableIF variable, int dataBufferType, int sourceWidth, int sourceHeight,
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

        final int indexY = getIndexY(rank);
        final double scale = getScale();

        shape[indexY] = getSourceHeight(rectangle.height);
        origin[indexY] = getSourceY(rectangle.y) + getSourceOriginY();
        stride[indexY] = (int) scale;

        Array array;
        synchronized (variable.getParentGroup().getNetcdfFile()) {
            try {
                final Section section = new Section(origin, shape, stride);
                array = variable.read(section);
            } catch (IOException | InvalidRangeException e) {
                throw new RuntimeException(e);
            }
        }
        for (int j = 0; j < rectangle.width; j++) {
           tile.setDataElements(rectangle.x + j, rectangle.y, 1, rectangle.height, transformStorage(array));
        }
    }

    protected abstract int getIndexY(int rank);

    protected int getSourceOriginY() {
        return 0;
    }

    protected Object transformStorage(Array array) {
        return array.getStorage();
    }

}
