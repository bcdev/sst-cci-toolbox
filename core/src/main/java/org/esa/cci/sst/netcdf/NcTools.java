package org.esa.cci.sst.netcdf;

import org.esa.cci.sst.common.GridDef;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.IOException;

public class NcTools {

    public static Variable getVariable(NetcdfFile netcdfFile, String variableName) throws IOException {
        final Variable variable = netcdfFile.findVariable(variableName);
        if (variable == null) {
            throw new IOException(String.format("Missing variable '%s' in file '%s'.", variableName, netcdfFile.getLocation()));
        }
        return variable;
    }

    public static Rectangle getGridRectangle(NetcdfFile netcdfFile, Variable variable, GridDef expectedGridDef) throws IOException {
        final int rank = variable.getRank();
        if (rank < 2) {
            throw new IOException(String.format("Variable '%s' in file '%s': expected rank 2 or higher, but found %d.",
                    variable.getShortName(), netcdfFile.getLocation(), rank));
        }

        final int w = variable.getDimension(rank - 1).getLength();
        final int h = variable.getDimension(rank - 2).getLength();
        if (w != expectedGridDef.getWidth() || h != expectedGridDef.getHeight()) {
            throw new IOException(String.format("Variable '%s' in file '%s': unexpected grid size.", variable.getShortName(), netcdfFile.getLocation()));
        }
        return new Rectangle(0, 0, w, h);
    }
}
