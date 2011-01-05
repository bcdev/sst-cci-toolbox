package org.esa.cci.sst;

import org.esa.cci.sst.reader.AatsrMatchupReader;
import org.esa.cci.sst.reader.MetopMatchupReader;
import org.esa.cci.sst.reader.ObservationReader;
import org.esa.cci.sst.reader.SeviriMatchupReader;

import java.io.File;
import java.io.FileFilter;

/**
 * TODO add API doc
 *
 * matchup query pattern to be used with psql (very draft):
 *   select o1.id, o2.id, o1.time, o2.time from mm_observation o1, mm_observation o2 where o1.id > 656636 and o1.datafile_id = 417051 and o2.datafile_id = 657751 and (o1.time,o1.time+'12:00:00') overlaps (o2.time-'1 month', o2.time+'12:00:00'-'1 month') and st_distance(o1.location,o2.location) < 10400000;
 * @author Martin Boettcher
 */
public class IngestionToolTest {

    static final String AATSR_MATCHUP_DIR_PATH  = "/mnt/hgfs/c/sst-mmd-data/mmd_test_month/ATSR_MD";
    static final String METOP_MATCHUP_DIR_PATH  = "/mnt/hgfs/c/sst-mmd-data/mmd_test_month/METOP_MD";
    static final String SEVIRI_MATCHUP_DIR_PATH = "/mnt/hgfs/c/sst-mmd-data/mmd_test_month/SEVIRI_MD";

    static final String AATSR_MATCHUP_FILENAME_PATTERN = ".*";
    static final String METOP_MATCHUP_FILENAME_PATTERN = ".*0[123]\\.nc\\.gz";
    static final String SEVIRI_MATCHUP_FILENAME_PATTERN = ".*0[123]\\.nc\\.gz";

    private IngestionTool tool = new IngestionTool();

    public static void main(String[] args) {
        try {
            IngestionToolTest t = new IngestionToolTest();
            t.tool.clearObservations();
            t.ingestDirectoryContent(AATSR_MATCHUP_DIR_PATH, AATSR_MATCHUP_FILENAME_PATTERN, "aatsr", new AatsrMatchupReader());
            t.ingestDirectoryContent(METOP_MATCHUP_DIR_PATH, METOP_MATCHUP_FILENAME_PATTERN, "metop", new MetopMatchupReader());
            t.ingestDirectoryContent(SEVIRI_MATCHUP_DIR_PATH, SEVIRI_MATCHUP_FILENAME_PATTERN, "seviri", new SeviriMatchupReader());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ingestDirectoryContent(String dirPath, String filenamePattern, String schemaName, ObservationReader reader) throws Exception {

        final File dir = new File(dirPath);
        final String pattern = filenamePattern;
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().matches(pattern);
            }
        };
        for (File file : dir.listFiles(fileFilter)) {
            System.out.printf("reading %s\n", file.getName());
            tool.ingest(file, schemaName, reader);
        }
    }
}
