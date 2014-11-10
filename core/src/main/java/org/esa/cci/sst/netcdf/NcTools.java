package org.esa.cci.sst.netcdf;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;

public class NcTools {

    public static Variable getVariable(NetcdfFile netcdfFile, String variableName) throws IOException {
        final Variable variable = netcdfFile.findVariable(variableName);
        if (variable == null) {
            throw new IOException(String.format("Missing variable '%s' in file '%s'.", variableName, netcdfFile.getLocation()));
        }
        return variable;
    }
}
