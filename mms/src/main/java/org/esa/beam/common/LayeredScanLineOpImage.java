package org.esa.beam.common;

import org.esa.beam.jai.ResolutionLevel;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Variable;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.WritableRaster;
import java.io.IOException;

public class LayeredScanLineOpImage extends ScanLineVariableOpImage {

    private final int zLayer;

    public LayeredScanLineOpImage(Variable variable, int bufferType, int width, int height, int zLayer, Dimension tileSize) {
        super(variable, bufferType, width, height, tileSize, ResolutionLevel.MAXRES);
        this.zLayer = zLayer;
    }

    @Override
    protected int getIndexY(int rank) {
        return 0;
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle rectangle) {
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
        origin[1] = zLayer;
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


}
