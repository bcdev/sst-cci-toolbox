package org.esa.cci.sst.util;

import org.esa.beam.util.io.CsvReader;
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
 * Tool responsible for writing the matchup data file. Comprises a main method, and is configured by the file
 * <code>mms-config.properties</code>, which has to be provided in the working directory.
 */
public class MmdGeneratorTool {

    private static String outputVarsFilename;

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

    static boolean isOutputVariable(final String varName) {
        final List<String> outputVariables = getOutputVariables(outputVarsFilename);
        if(outputVariables == null) {
            return false;
        } else {
            for (String variable : outputVariables) {
                if(varName.equalsIgnoreCase(variable)) {
                    return true;
                }
                String toPattern = variable.replace("*", ".*");
                toPattern = toPattern.replace("?", ".?");
                Pattern pattern = Pattern.compile(toPattern);
                final Matcher matcher = pattern.matcher(varName);
                if(matcher.find()) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean isOutputVariable(final String varName, final String filename) {
        outputVarsFilename = filename;
        return isOutputVariable(varName);
    }

    interface MmdGenerator {

        static final String DIMENSION_ROLE_LENGTH = "length";
        static final String DIMENSION_ROLE_MATCHUP = "match_up";
        static final int AATSR_MD_CS_LENGTH = 8;
        static final int AATSR_MD_UI_LENGTH = 30;
        static final int AATSR_MD_LENGTH = 65;
        static final int METOP_LENGTH = 21;
        static final int METOP_LEN_ID = 11;
        static final int METOP_LEN_FILENAME = 65;
        static final int SEVIRI_LENGTH = 5;
        static final int SEVIRI_LEN_ID = 11;
        static final int SEVIRI_LEN_FILENAME = 65;
        static final int AATSR_LENGTH = 101;
        static final int AVHRR_WIDTH = 25;
        static final int AVHRR_HEIGHT = 31;
        static final int AMSRE_LENGTH = 11;
        static final int TMI_LENGTH = 11;
        // todo - clarify if this is ok
        static final int AAI_LENGTH = 1;
        // todo - clarify if this is ok
        static final int SEA_ICE_LENGTH = 11;

        static final String COUNT_MATCHUPS_QUERY =
                "select count( m ) "
                + " from Matchup m";

        static final String ALL_MATCHUPS_QUERY =
                "select m"
                + " from Matchup m"
                + " order by m.id";

        void createMmdStructure(NetcdfFileWriteable file) throws Exception;

        void writeMatchups(NetcdfFileWriteable file) throws Exception;

        void close() throws IOException;
    }

}
