package org.esa.beam.common;

import org.esa.beam.jai.ResolutionLevel;
import ucar.nc2.Variable;

import java.awt.*;

public class Default2DOpImage extends ImageVariableOpImage {

    public Default2DOpImage(Variable variable, int bufferType, int w, int h, Dimension tileSize) {
        super(variable, bufferType, w, h, tileSize, ResolutionLevel.MAXRES);
    }

    @Override
    protected int getIndexX(int rank) {
        return rank - 1;
    }

    @Override
    protected int getIndexY(int rank) {
        return rank - 2;
    }
}
