package org.esa.cci.sst;

import org.junit.Ignore;

import java.io.File;
import java.io.FileFilter;
import java.util.Properties;

/**
 * matchup query pattern to be used with psql (very draft):
 * select o1.id, o2.id, o1.time, o2.time from mm_observation o1, mm_observation o2 where o1.id > 656636 and o1.datafile_id = 417051 and o2.datafile_id = 657751 and (o1.time,o1.time+'12:00:00') overlaps (o2.time-'1 month', o2.time+'12:00:00'-'1 month') and st_distance(o1.location,o2.location) < 10400000;
 *
 * @author Martin Boettcher
 * @author Norman Fomferra
 */
@Ignore
public class IngestionToolIntegrationTest {

    public static void main(String[] args) {
        try {
            IngestionTool tool = new IngestionTool();
            //tool.clearObservations();
            tool.addConfigurationProperties(new File("./mms-test.properties"));
            tool.setCommandLineArgs(args);

            final Properties configuration = tool.getConfiguration();
            int n = 0;
            for (int i = 0; i < 100; i++) {
                final String schemaName = configuration.getProperty(String.format("mms.test.inputSets.%d.schemaName", i));
                final String inputDirectory = configuration.getProperty(String.format("mms.test.inputSets.%d.inputDirectory", i));
                final String filenamePattern = configuration.getProperty(String.format("mms.test.inputSets.%d.filenamePattern", i));
                if (schemaName != null && inputDirectory != null) {
                    ingestDirectoryContent(tool,
                                           inputDirectory,
                                           filenamePattern != null ? filenamePattern : ".*",
                                           schemaName);
                    n++;
                }
            }
            if (n > 0) {
                System.out.println(n + " input set(s) ingested.");
            } else {
                System.err.println("No input sets given.");
                tool.printHelp("\nInput sets are specified as configuration properties as follows:\n"
                        + "\tmms.test.inputSets.<i>.schemaName = <schemaName>\n"
                        + "\tmms.test.inputSets.<i>.inputDirectory = <inputDirectory>\n"
                        + "\tmms.test.inputSets.<i>.filenamePattern = <filenamePattern> (opt)");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ingestDirectoryContent(IngestionTool tool, String dirPath, String filenamePattern, String schemaName) throws IngestionTool.ToolException {

        final File dir = new File(dirPath);
        final String pattern = filenamePattern;
        final FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().matches(pattern);
            }
        };

        final File[] inputFiles = dir.listFiles(fileFilter);
        if (inputFiles != null) {
            tool.setSchemaName(schemaName);
            tool.setInputFiles(inputFiles);
            tool.ingest();
        }
    }
}
