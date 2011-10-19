package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.ArrayGrid;
import org.esa.cci.sst.util.Grid;
import org.esa.cci.sst.util.GridDef;
import org.esa.cci.sst.util.NcUtils;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;

/**
 * Represents LUT1.
 * Enables calculation of coverage/sampling uncertainty for an average via the number of values comprising that average.
 * LUT1 contains values of two parameters (magnitude and exponent) for each 5° monthly grid box.
 *
 * @author Norman Fomferra
 */
public class LUT1 {

    private final ArrayGrid magnitudeGrid;
    private final ArrayGrid exponentGrid;

    public static LUT1 read(File file) throws IOException {
        NetcdfFile netcdfFile = NetcdfFile.open("file:" + file.getPath().replace("\\", "/"));
        try {
            GridDef gridDef = GridDef.createGlobal(5.0);
            ArrayGrid magnitudeGrid = NcUtils.readGrid(netcdfFile, "MAGNITUDE", gridDef);
            magnitudeGrid.flipY();
            ArrayGrid exponentGrid = NcUtils.readGrid(netcdfFile, "EXPONENT", gridDef);
            exponentGrid.flipY();
            return new LUT1(magnitudeGrid, exponentGrid);
        } finally {
            netcdfFile.close();
        }
    }

    private LUT1(ArrayGrid magnitudeGrid, ArrayGrid exponentGrid) {
        //To change body of created methods use File | Settings | File Templates.
        this.magnitudeGrid = magnitudeGrid;
        this.exponentGrid = exponentGrid;
    }

    public Grid getMagnitudeGrid5() {
        return magnitudeGrid;
    }

    public Grid getExponentGrid5() {
        return exponentGrid;
    }
}
