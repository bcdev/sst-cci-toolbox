package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.tools.BasicTool;
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
            file = createOutputFile(tool);
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

    private static NetcdfFileWriteable createOutputFile(final MmdGeneratorTool tool) throws IOException {
        final Properties properties = tool.getConfiguration();
        final String mmdPath = properties.getProperty("mmd.output.destdir", ".");
        final String mmdFileName = properties.getProperty("mmd.output.filename", "mmd.nc");
        final String destFile = new File(mmdPath, mmdFileName).getAbsolutePath();
        final NetcdfFileWriteable file = NetcdfFileWriteable.createNew(destFile, false);
        file.setLargeFile(true);
        return file;
    }

}
