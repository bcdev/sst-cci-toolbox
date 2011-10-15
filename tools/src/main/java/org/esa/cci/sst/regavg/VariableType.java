package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.Grid;
import org.esa.cci.sst.util.GridDef;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

/**
* Provides data and behaviour for a specific variable-type.
*
* @author Norman Fomferra
*/
public interface VariableType {

    Grid readGrid(NetcdfFile netcdfFile, GridDef sourceGridDef) throws IOException;

    Accumulator createAccumulator();
}
