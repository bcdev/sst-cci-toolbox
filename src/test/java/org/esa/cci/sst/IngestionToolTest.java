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
 * @author Martin Boettcher
 */
public class IngestionToolTest {

    static final String AATSR_MATCHUP_FILE_PATH = "/home/boe/samples/aatsr_l2p_mdb_rad_all_data.nc";

    @Test
    public void testIngestAatsrFile() throws Exception {
        new IngestionTool().ingest(AATSR_MATCHUP_FILE_PATH, "aatsr");
    }

}
