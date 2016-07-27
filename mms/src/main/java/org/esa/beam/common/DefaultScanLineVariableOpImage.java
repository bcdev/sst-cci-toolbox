package org.esa.beam.common;

import org.esa.beam.jai.ResolutionLevel;
import ucar.nc2.VariableIF;

import java.awt.*;

public class DefaultScanLineVariableOpImage extends ScanLineVariableOpImage {

    public DefaultScanLineVariableOpImage(VariableIF variable, int dataBufferType, int sourceWidth, int sourceHeight, Dimension tileSize) {
        super(variable, dataBufferType, sourceWidth, sourceHeight, tileSize, ResolutionLevel.MAXRES);
    }

    @Override
    protected int getIndexY(int rank) {
        return rank - 1;
    }
}
