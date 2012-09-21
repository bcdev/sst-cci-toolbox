package org.esa.cci.sst.regrid.auxiliary;

import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.regrid.SpatialResolution;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;

/**
 * Lookup table as demanded by Regridding Tool specification equation 1.5.
 * Used to calculate eta, the estimated effective number of synoptic areas in the new grid box.
 * <p/>
 * {@author Bettina Scholze}
 * Date: 19.09.12 15:26
 */
public class LUT3 {
    Grid gridDxy;
    Grid gridDt;

    public static LUT3 read(File lut3File, SpatialResolution spatialResolution) {
        //todo readGrid()
        //todo bs scale(spatialResolution) if not on demanded resolution
        throw new NotImplementedException();
    }

    public double getDxy(int x, int y) {
//        return gridDxy.getSampleDouble(x, y); //todo bs uncomment to implement it
        return 0.0;
    }

    public double getDt(int x, int y) {
//        return gridDt.getSampleDouble(x, y); //todo bs uncomment to implement it
        return 0.0;
    }
}
