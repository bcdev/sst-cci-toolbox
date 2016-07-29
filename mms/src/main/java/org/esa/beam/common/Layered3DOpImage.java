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

public class Layered3DOpImage extends ImageVariableOpImage {

    private final int zLayer;

    public Layered3DOpImage(Variable variable, int bufferType, int width, int height, int zLayer, Dimension tileSize) {
        super(variable, bufferType, width, height, tileSize, ResolutionLevel.MAXRES);
        this.zLayer = zLayer;
    }

    @Override
    protected int getIndexX(int rank) {
        return rank - 2;
    }

    @Override
    protected int getIndexY(int rank) {
        return rank - 3;
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
        final int indexX = getIndexX(rank);
        final int indexY = getIndexY(rank);

        shape[indexX] = getSourceShapeX(rectangle.width);
        shape[indexY] = getSourceShapeY(rectangle.height);

        origin[2] = zLayer;
        origin[indexX] = getSourceOriginX(rectangle.x) + getSourceOriginX();
        origin[indexY] = getSourceOriginY(rectangle.y) + getSourceOriginY();

        final double scale = getScale();
        stride[indexX] = (int) scale * getSourceStrideX();
        stride[indexY] = (int) scale * getSourceStrideY();

        Array array;
        synchronized (variable.getParentGroup().getNetcdfFile()) {
            try {
                final Section section = new Section(origin, shape, stride);
                array = variable.read(section);
            } catch (IOException | InvalidRangeException e) {
                throw new RuntimeException(e);
            }
        }
        tile.setDataElements(rectangle.x, rectangle.y, rectangle.width, rectangle.height, transformStorage(array));
    }

}
