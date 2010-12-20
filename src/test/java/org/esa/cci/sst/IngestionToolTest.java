package org.esa.cci.sst;

import org.apache.openjpa.persistence.Extent;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.esa.cci.sst.data.Observation;
import org.junit.Test;
import org.postgis.PGgeometry;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.util.List;

/**
 * TODO add API doc
 *
 * matchup query pattern to be used with psql (very draft):
 *   select o1.id, o2.id, o1.time, o2.time from mm_observation o1, mm_observation o2 where o1.id > 656636 and o1.datafile_id = 417051 and o2.datafile_id = 657751 and (o1.time,o1.time+'12:00:00') overlaps (o2.time-'1 month', o2.time+'12:00:00'-'1 month') and st_distance(o1.location,o2.location) < 10400000;
 * @author Martin Boettcher
 */
public class IngestionToolTest {

    static final String DATASCHEMA_FILENAME = "dataschema.properties";
    static final String AATSR_MATCHUP_FILE_PATH = "/home/boe/samples/aatsr_l2p_mdb_rad_all_data.nc";
    static final String SEVIRI_MATCHUP_FILE_PATH = "/home/boe/samples/sstmdb1_meteosat09_20100101.nc";

    @Test
    public void testIngestAatsrFile() throws Exception {
        System.getProperties().load(ClassLoader.getSystemResourceAsStream(DATASCHEMA_FILENAME));
        new IngestionTool().ingest(AATSR_MATCHUP_FILE_PATH, "aatsr", "aatsr");
    }

    @Test
    public void testIngestSeviriFile() throws Exception {
        System.getProperties().load(ClassLoader.getSystemResourceAsStream(DATASCHEMA_FILENAME));
        new IngestionTool().ingest(SEVIRI_MATCHUP_FILE_PATH, "seviri", "seviri");
    }
}
