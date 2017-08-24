package org.esa.cci.sst.util;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;

public class NetCDFUtil {

    public static Variable findVariable(NetcdfFile file, String... names) throws IOException {
        for (final String name : names) {
            final Variable v = file.findVariable(NetcdfFile.makeValidPathName(name));
            if (v != null) {
                return v;
            }
        }
        throw new IOException(MessageFormat.format("Expected to find any variable in ''{0}''.", Arrays.toString(names)));
    }
}
