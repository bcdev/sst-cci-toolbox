package org.esa.cci.sst;

import org.esa.beam.util.io.CsvReader;
import org.esa.cci.sst.util.DefaultMmdGenerator;
import org.esa.cci.sst.util.SelectedVarsMmdGenerator;
import ucar.nc2.NetcdfFileWriteable;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
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

    private static String outputVarsFilename;

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
            final FileInputStream stream = new FileInputStream("mms-config.properties");
            properties.load(stream);
            String mmdFilename = properties.getProperty("mmd.output.filename");
            if (mmdFilename == null) {
                // fallback
                mmdFilename = "mmd.nc";
            }
            generator = getMmdGenerator(properties);
            file = NetcdfFileWriteable.createNew(mmdFilename, false);
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

    static List<String> getOutputVariables(final String filename) {
        final List<String> outputVariables = new ArrayList<String>();
        if (filename == null) {
            return null;
        }
        final List<String[]> stringRecords;
        try {
            final CsvReader csvReader = new CsvReader(new FileReader(filename), new char[]{' ', ',', '\t', '\n'},
                                                      true, "#");
            stringRecords = csvReader.readStringRecords();
        } catch (IOException e) {
            // todo - replace with logging
            e.printStackTrace();
            return outputVariables;
        }
        for (String[] s : stringRecords) {
            outputVariables.add(s[0]);
        }
        return outputVariables;
    }


    static boolean isOutputVariable(final String varName) {
        final List<String> outputVariables = getOutputVariables(outputVarsFilename);
        if (outputVariables == null) {
            return false;
        } else {
            for (String variable : outputVariables) {
                if (varName.equalsIgnoreCase(variable)) {
                    return true;
                }
                String toPattern = variable.replace("*", ".*");
                toPattern = toPattern.replace("?", ".?");
                Pattern pattern = Pattern.compile(toPattern);
                final Matcher matcher = pattern.matcher(varName);
                if (matcher.find()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isOutputVariable(final String varName, final String filename) {
        outputVarsFilename = filename;
        return isOutputVariable(varName);
    }

    private static MmdGenerator getMmdGenerator(final Properties properties) throws IOException {
        outputVarsFilename = properties.getProperty("mmd.output.variables.filename");
        final List<String> outputVariables = getOutputVariables(outputVarsFilename);
        if (outputVariables == null) {
            // todo - ts - replace with logging
            System.out.println("Writing all variables.");
            return new DefaultMmdGenerator(properties);
        } else {
            // todo - ts - replace with logging
            System.out.println("Writing specified variables:");
            for (String outputVariable : outputVariables) {
                System.out.println("\toutputVariable = " + outputVariable);
            }
            return new SelectedVarsMmdGenerator(properties, outputVariables);
        }
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
