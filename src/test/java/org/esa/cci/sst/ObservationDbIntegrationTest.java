/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst;

import org.apache.openjpa.persistence.Extent;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.esa.cci.sst.data.ReferenceObservation;
import org.postgis.PGgeometry;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.util.List;

/**
 * @author Martin Boettcher
 */
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
        final ReferenceObservation i = new ReferenceObservation();
        i.setName("helgoland");
        i.setLocation(new PGgeometry("SRID=4326;POINT(-110 30)"));
        m.persist(i);
        final ReferenceObservation i2 = new ReferenceObservation();
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
        final ReferenceObservation i = new ReferenceObservation();
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
        final Query query = m.createQuery("select o from ReferenceObservation o where o.location is null");
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
        final Extent<ReferenceObservation> observationExtent = kem.createExtent(ReferenceObservation.class, false);
        for (ReferenceObservation o : observationExtent) {
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
        final Query query = m.createQuery("select o from ReferenceObservation o");
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
        final Query query = m.createNativeQuery("select o.id, o.name, o.location, o.time, o.recordno from mm_observation o where st_intersects(o.location, st_geographyfromtext('polygon((-100 20,-120 20,-120 40,-100 40,-100 20))'))", ReferenceObservation.class);
        final List results = query.getResultList();
        for (Object o : results) {
            System.out.println(o);
        }
        m.getTransaction().commit();
        //
        m.close();
    }
}
