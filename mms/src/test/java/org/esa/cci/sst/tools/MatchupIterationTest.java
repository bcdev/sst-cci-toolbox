package org.esa.cci.sst.tools;

import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Test;

import javax.persistence.Query;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * TODO add API doc
 *
 * @author Martin Boettcher
 */
public class MatchupIterationTest extends BasicTool {

    private static final String ATSR_MD = "atsr_md";

    private static final String SENSOR_OBSERVATION_QUERY =
            "select o"
                    + " from ReferenceObservation o"
                    + " where o.sensor = ?1"
                    + " and o.time >= ?2 and o.time < ?3"
                    + " order by o.time, o.id";

    private static final int CHUNK_SIZE = 1024; //*16;

    public MatchupIterationTest() {
        super(null, "0.2");
    }

    public static void main(String[] args) {
        try {
            new MatchupIterationTest().testIterateIncrementally();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testIterateIncrementally() throws ParseException {
        setCommandLineArgs(new String[0]);
        initialize();
        Date matchupStartTime = TimeUtil.parseCcsdsUtcFormat("2010-12-01T00:00:00Z");
        Date matchupStopTime = TimeUtil.parseCcsdsUtcFormat("2011-01-01T00:00:00Z");

        getPersistenceManager().transaction();
        final Query query = getPersistenceManager().createQuery(SENSOR_OBSERVATION_QUERY);
        query.setParameter(1, ATSR_MD);
        query.setParameter(2, matchupStartTime);
        query.setParameter(3, matchupStopTime);
        query.setMaxResults(CHUNK_SIZE);

        int count = 0;
        for (int cursor = 0; ; ) {
            final List<ReferenceObservation> atsrObservations = query.setFirstResult(cursor).getResultList();
            if (atsrObservations.size() == 0) {
                break;
            }
            cursor += atsrObservations.size();

            for (final ReferenceObservation atsrObservation : atsrObservations) {
                System.out.println(count + " " + atsrObservation.getId() + " " + TimeUtil.formatCcsdsUtcFormat(atsrObservation.getTime()));

                ++count;
                if (count % 1024 == 0) {
                    getPersistenceManager().commit();
                    getPersistenceManager().transaction();
                    System.out.format("{0} processed", count);
                }
            }
        }
        getPersistenceManager().commit();
    }

}
