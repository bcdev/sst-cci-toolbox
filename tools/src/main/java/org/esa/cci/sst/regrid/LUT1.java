package org.esa.cci.sst.regrid;

import org.esa.cci.sst.util.ArrayGrid;
import org.esa.cci.sst.util.Grid;
import org.esa.cci.sst.util.GridDef;
import org.esa.cci.sst.util.NcUtils;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.net.URL;

/**
 * {@author Bettina Scholze}
 * Date: 06.08.12 12:16
 */
public class LUT1 {

    private final ArrayGrid magnitudeGrid;
    private final ArrayGrid exponentGrid;


    public static LUT1 read(GridDef gridDef) {
        URL resource = LUT1.class.getResource("coverage_uncertainty_parameters.nc"); //todo always same resolution 0.05

        NetcdfFile netcdfFile = null;
        try {
            netcdfFile = NetcdfFile.open(resource.toString());
            ArrayGrid magnitudeGrid = NcUtils.readSimpleGrid("MAGNITUDE", netcdfFile, gridDef);
            ArrayGrid exponentGrid = NcUtils.readSimpleGrid("EXPONENT", netcdfFile, gridDef);
            return new LUT1(magnitudeGrid, exponentGrid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (netcdfFile != null) {
                    netcdfFile.close();
                }
            } catch (IOException e) {
                //ignore
            }
        }
    }

    private LUT1(ArrayGrid magnitudeGrid, ArrayGrid exponentGrid) {
        this.magnitudeGrid = magnitudeGrid;
        this.exponentGrid = exponentGrid;
    }

    public Grid getMagnitudeGrid() {
        return magnitudeGrid;
    }

    public Grid getExponentGrid() {
        return exponentGrid;
    }
}