package org.esa.cci.sst;

import org.apache.openjpa.persistence.Extent;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.esa.cci.sst.data.Observation;
import org.junit.Ignore;
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
@Ignore
public class ObservationDbIntegrationTest {

    public static void main(String[] args) {
        try {
            ObservationDbIntegrationTest t = new ObservationDbIntegrationTest();
            //t.testCreateDbEntry();
            t.testCreateNullEntry();
            //t.testReadDbEntry();
            t.testReadNullEntry();
            //t.testSearchDbEntry();
            //t.testGeoSearchDbEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testCreateDbEntry() throws Exception {
        final EntityManagerFactory f = Persistence.createEntityManagerFactory("matchupdb");
        final EntityManager m = f.createEntityManager();
        // write new observation to database
        m.getTransaction().begin();
        final Observation i = new Observation();
        i.setName("helgoland");
        i.setLocation(new PGgeometry("SRID=4326;POINT(-110 30)"));
        m.persist(i);
        final Observation i2 = new Observation();
        i2.setName("isleofman");
        i2.setLocation(new PGgeometry("SRID=4326;POINT(10 50)"));
        m.persist(i2);
        m.getTransaction().commit();
        //
        m.close();
    }

    public void testCreateNullEntry() throws Exception {
        final EntityManagerFactory f = Persistence.createEntityManagerFactory("matchupdb");
        final EntityManager m = f.createEntityManager();
        // write new observation to database
        m.getTransaction().begin();
        final Observation i = new Observation();
        i.setName("nowhere");
        i.setLocation(null);
        //i.setLocation(new PGgeometry("SRID=4326;POINT(-110 30)"));
        //System.out.println(i.getLocation().getValue());
        Query q = m.createNativeQuery("insert into mm_observation values (?,?,?,?,?,?,?,?)");
        q.setParameter(1, 14);
        q.setParameter(2, i.isClearSky());
        q.setParameter(3, i.getLocation());
        q.setParameter(4, i.getName());
        q.setParameter(5, i.getRecordNo());
        q.setParameter(6, i.getSensor());
        q.setParameter(7, i.getTime());
        q.setParameter(8, 1);
        q.executeUpdate();

        //m.persist(i);
        m.getTransaction().commit();
        m.close();

    }

    public void testReadNullEntry() throws Exception {
        final EntityManagerFactory f = Persistence.createEntityManagerFactory("matchupdb");
        final EntityManager m = f.createEntityManager();
        // read from database
        m.getTransaction().begin();
        final Query query = m.createQuery("select o from Observation o where o.location is null");
        final List results = query.getResultList();
        for (Object o : results) {
            System.out.println(o);
        }
        m.getTransaction().commit();
        //
        m.close();
    }

    public void testReadDbEntry() throws Exception {
        final EntityManagerFactory f = Persistence.createEntityManagerFactory("matchupdb");
        final EntityManager m = f.createEntityManager();
        // read from database
        m.getTransaction().begin();
        final OpenJPAEntityManager kem = OpenJPAPersistence.cast(m);
        final Extent<Observation> observationExtent = kem.createExtent(Observation.class, false);
        for (Observation o : observationExtent) {
            System.out.println(o);
        }
        m.getTransaction().commit();
        //
        m.close();
    }

    public void testSearchDbEntry() throws Exception {
        final EntityManagerFactory f = Persistence.createEntityManagerFactory("matchupdb");
        final EntityManager m = f.createEntityManager();
        // search in database
        System.out.println("search results:");
        m.getTransaction().begin();
        final Query query = m.createQuery("select o from Observation o");
        final List results = query.getResultList();
        for (Object o : results) {
            System.out.println(o);
        }
        m.getTransaction().commit();
        //
        m.close();
    }

    public void testGeoSearchDbEntry() throws Exception {
        final EntityManagerFactory f = Persistence.createEntityManagerFactory("matchupdb");
        final EntityManager m = f.createEntityManager();
        // search in database
        System.out.println("geo-search results:");
        m.getTransaction().begin();
        final Query query = m.createNativeQuery("select o.id, o.name, o.location, o.time, o.recordno from mm_observation o where st_intersects(o.location, st_geographyfromtext('polygon((-100 20,-120 20,-120 40,-100 40,-100 20))'))", Observation.class);
        final List results = query.getResultList();
        for (Object o : results) {
            System.out.println(o);
        }
        m.getTransaction().commit();
        //
        m.close();
    }
}
