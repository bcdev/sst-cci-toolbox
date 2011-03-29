package org.esa.cci.sst;

import org.esa.beam.util.io.CsvReader;
import org.esa.cci.sst.util.DefaultMmdGenerator;
import ucar.nc2.NetcdfFileWriteable;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool for writing the matchup data file. Comprises a main method, and is configured by the file
 * <code>mms-config.properties</code>, which has to be provided in the working directory.
 */
public class MmdGeneratorTool {

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

        final Properties properties = new Properties();
        try {
            final InputStream is = new FileInputStream("mms-config.properties");
            properties.load(is);
            final String mmdFileName = properties.getProperty("mmd.output.filename", "mmd.nc");
            generator = getMmdGenerator(properties);
            file = NetcdfFileWriteable.createNew(mmdFileName, true);
            generator.createMmdStructure(file);
            generator.writeMatchups(file);
        } catch (Exception e) {
            // todo - ts - replace with logging
            e.printStackTrace();
        } finally {
            if (file != null) {
                file.close();
            }
            if (generator != null) {
                generator.close();
            }
        }
    }

    private static MmdGenerator getMmdGenerator(final Properties configuration) throws IOException {
        final String propertiesFilePath = configuration.getProperty("mmd.output.variables");
        final InputStream is = new FileInputStream(propertiesFilePath);
        final Properties variableProperties = new Properties();
        variableProperties.load(is);
        return new DefaultMmdGenerator(configuration, variableProperties);
    }

    public interface MmdGenerator {

        static final String COUNT_MATCHUPS_QUERY =
                "select count( m ) "
                + " from Matchup m";

        static final String ALL_MATCHUPS_QUERY =
                "select m"
                + " from Matchup m"
                + " order by m.id";

        /**
         * Creates the netcdf structure for the given file, that is, variables, dimensions, and (global) attributes.
         *
         * @param file The target MMD file.
         *
         * @throws Exception If something goes wrong.
         */
        void createMmdStructure(NetcdfFileWriteable file) throws Exception;

        /**
         * Writes the matchups from the database into the MMD file.
         *
         * @param file The target MMD file.
         *
         * @throws Exception If something goes wrong.
         */
        void writeMatchups(NetcdfFileWriteable file) throws Exception;

        /**
         * Closes the generator and performs some cleanup.
         */
        void close();
    }

}
