package org.esa.cci.sst;

import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

/**
 * Tool to ingest new MD files into the MMS database.
 */
public class IngestionTool {
    public static void main(String[] args) throws IOException {
        NetcdfFile netcdfFile = NetcdfFile.open(args[0]);
        List<Variable> variableList = netcdfFile.getVariables();
        for (Variable variable : variableList) {
            Dimension recDim = variable.getDimension(0);
        }

    }
}
