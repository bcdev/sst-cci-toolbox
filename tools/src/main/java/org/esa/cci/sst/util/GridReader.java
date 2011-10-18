package org.esa.cci.sst.util;

import ucar.nc2.NetcdfFile;

import java.io.IOException;

/**
 * A reader for grids.
 *
 * @author Norman Fomferra
 */
public interface GridReader {
    Grid readGrid(NetcdfFile netcdfFile) throws IOException;
}
