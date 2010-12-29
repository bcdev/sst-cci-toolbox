package org.esa.cci.sst;

/**
 * TODO add API doc
 *
 * matchup query pattern to be used with psql (very draft):
 *   select o1.id, o2.id, o1.time, o2.time from mm_observation o1, mm_observation o2 where o1.id > 656636 and o1.datafile_id = 417051 and o2.datafile_id = 657751 and (o1.time,o1.time+'12:00:00') overlaps (o2.time-'1 month', o2.time+'12:00:00'-'1 month') and st_distance(o1.location,o2.location) < 10400000;
 * @author Martin Boettcher
 */
public class IngestionToolTest {

    static final String DATASCHEMA_FILENAME = "dataschema.properties";
    static final String AATSR_MATCHUP_FILE_PATH = "/home/boe/samples/aatsr_l2p_mdb_all_data.nc";
    static final String METOP_MATCHUP_FILE_PATH = "/home/boe/samples/mdb1_metop02_20100101.nc";
    static final String SEVIRI_MATCHUP_FILE_PATH = "/home/boe/samples/sstmdb1_meteosat09_20100101.nc";

    public static void main(String[] args) {
        try {
            IngestionToolTest t = new IngestionToolTest();
            t.testIngestAatsrFile();
            t.testIngestMetopFile();
            t.testIngestSeviriFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testIngestAatsrFile() throws Exception {
        System.getProperties().load(ClassLoader.getSystemResourceAsStream(DATASCHEMA_FILENAME));
        new IngestionTool2().ingest(AATSR_MATCHUP_FILE_PATH, "aatsr", "aatsr");
    }

    public void testIngestMetopFile() throws Exception {
        System.getProperties().load(ClassLoader.getSystemResourceAsStream(DATASCHEMA_FILENAME));
        new IngestionTool2().ingest(METOP_MATCHUP_FILE_PATH, "metop", "metop");
    }

    public void testIngestSeviriFile() throws Exception {
        System.getProperties().load(ClassLoader.getSystemResourceAsStream(DATASCHEMA_FILENAME));
        new IngestionTool2().ingest(SEVIRI_MATCHUP_FILE_PATH, "seviri", "seviri");
    }
}
