package org.esa.cci.sst;

import ucar.nc2.NetcdfFileWriteable;

import java.util.Properties;

/**
 * Tool for writing the matchup data file. Comprises a main method, and is configured by the file
 * <code>mms-config.properties</code>, which has to be provided in the working directory.
 */
public class MmdGeneratorTool extends MmsTool {

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
        MmdGenerator generator = null;

        final MmdGeneratorTool tool = new MmdGeneratorTool();

        try {
            final boolean performWork = tool.setCommandLineArgs(args);
            if (!performWork) {
                return;
            }
            tool.initialize();
            final Properties properties = tool.getConfiguration();
            final String mmdFileName = properties.getProperty("mmd.output.filename", "mmd.nc");
            file = NetcdfFileWriteable.createNew(mmdFileName, true); // todo - fill = true is really expensive
            generator = new DefaultMmdGenerator(tool);
            generator.createMmdStructure(file);
            generator.writeMatchups(file);
        } catch (ToolException e) {
            tool.getErrorHandler().handleError(e, e.getMessage(), e.getExitCode());
        } catch (Throwable t) {
            tool.getErrorHandler().handleError(t, t.getMessage(), 1);
        } finally {
            if (file != null) {
                file.close();
            }
            if (generator != null) {
                generator.close();
            }
        }
    }

}
