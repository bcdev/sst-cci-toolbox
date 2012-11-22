package org.esa.cci.sst.common.auxiliary;

import org.esa.cci.sst.common.cellgrid.ArrayGrid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.YFlipperArrayGrid;
import org.esa.cci.sst.tool.Tool;
import org.esa.cci.sst.util.NcUtils;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Lookup table as demanded by Regridding Tool specification equations 1.6x.
 * Used to calculate the sampling/coverage uncertainty for the new grid box.
 * <p/>

 * {@author Bettina Scholze}
 * Date: 09.11.12 15:30
 */
public class LutForStdDeviation {
    private static final Logger LOGGER = Tool.LOGGER;

    private static final GridDef LUT_GRID_DEF = GridDef.createGlobal(0.05);
    private GridDef gridDef;
    private ArrayGrid stdDeviationSourceGrid; //0.1 ° or 0.5 ° same as input files


    private LutForStdDeviation(GridDef sourceProductGridDef) {
        this.gridDef = sourceProductGridDef;
    }

    public static LutForStdDeviation create(File file, GridDef sourceProductGridDef) throws IOException {
        LutForStdDeviation lut = new LutForStdDeviation(sourceProductGridDef);
        lut.readGrid(file);
        return lut;
    }

    public ArrayGrid getStdDeviationGrid() {
        return new YFlipperArrayGrid(stdDeviationSourceGrid);
    }

    private void readGrid(File file) throws IOException {
        long t0 = System.currentTimeMillis();
        LOGGER.info(String.format("Processing input LUT for coverage uncertainty file '%s'", file.getPath()));
        NetcdfFile netcdfFile = NetcdfFile.open("file:" + file.getPath().replace('\\', '/'));
        try {
            readGrid(netcdfFile);
        } finally {
            netcdfFile.close();
        }
        LOGGER.fine(String.format("Processing input LUT for coverage uncertainty file took %d ms", System.currentTimeMillis() - t0));
    }

    private void readGrid(NetcdfFile netcdfFile) throws IOException {
        long t0 = System.currentTimeMillis();

        LOGGER.fine("Reading 'analysed_sst_anomaly'...");
        stdDeviationSourceGrid = NcUtils.readGrid(netcdfFile, "analysed_sst_anomaly", LUT_GRID_DEF, 0);
        LOGGER.fine(String.format("Reading 'analysed_sst_anomaly' took %d ms", System.currentTimeMillis() - t0));

        if (!LUT_GRID_DEF.equals(gridDef)) { //Arc products with 0.1 °
            t0 = System.currentTimeMillis();
            stdDeviationSourceGrid = ArrayGrid.scaleDown(stdDeviationSourceGrid, gridDef);
            LOGGER.fine(String.format("Scaling 'analysed_sst_anomaly' took %d ms", System.currentTimeMillis() - t0));
        }
    }
}
