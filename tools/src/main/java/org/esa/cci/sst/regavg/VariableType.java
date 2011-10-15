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
public abstract class VariableType {

    private final String name;

    protected VariableType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract Grid readGrid(NetcdfFile netcdfFile, GridDef sourceGridDef) throws IOException;

    public abstract Accumulator createAccumulator();
}
