/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.ToolException;
import ucar.nc2.NetcdfFileWriteable;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Tool for writing the matchup data file. Comprises a main method, and is configured by the file
 * <code>mms-config.properties</code>, which has to be provided in the working directory.
 */
public class MmdGeneratorTool extends BasicTool {

    public MmdGeneratorTool() {
        super("mmscreatemmd.sh", "0.1");
    }

    /**
     * Main method. Generates a matchup data file based on the databases' contents. Configured by the file
     * <code>mms-config.properties</code>.
     *
     * @param args Program arguments, not considered.
     *
     * @throws Exception if something goes wrong.
     */
    public static void main(String[] args) throws Exception {
        NetcdfFileWriteable file = null;

        final MmdGeneratorTool tool = new MmdGeneratorTool();

        try {
            final boolean performWork = tool.setCommandLineArgs(args);
            if (!performWork) {
                return;
            }
            tool.initialize();
            file = createOutputFile(tool.getConfiguration());
            final MmdGenerator generator = new MmdGenerator(tool);
            final MmdStructureGenerator mmdStructureGenerator = new MmdStructureGenerator(tool, generator);
            mmdStructureGenerator.createMmdStructure(file);
            file.create();
            generator.writeMatchups(file);
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Throwable t) {
            tool.getErrorHandler().terminate(new ToolException(t.getMessage(), t, ToolException.UNKNOWN_ERROR));
        } finally {
            if (file != null) {
                file.close();
            }
        }
    }

    private static NetcdfFileWriteable createOutputFile(final Properties properties) throws IOException {
        final String mmdPath = properties.getProperty(Constants.PROPERTY_MMD_OUTPUT_DESTDIR, ".");
        final String mmdFileName = properties.getProperty(Constants.PROPERTY_MMD_OUTPUT_FILENAME, "mmd.nc");
        final String destFile = new File(mmdPath, mmdFileName).getAbsolutePath();
        final NetcdfFileWriteable file = NetcdfFileWriteable.createNew(destFile, false);
        file.setLargeFile(true);
        return file;
    }

}
