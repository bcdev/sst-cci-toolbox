package org.esa.cci.sst.orm;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Wrapper around JPA EntityManager to abstract from the persistence
 * implementation (JPA in this case).
 *
 * @author Martin Boettcher
 */
public class PersistenceManager {

    private EntityManagerFactory emFactory = null;
    private EntityManager entityManager = null;

    public PersistenceManager(String persistenceUnitName) {
        this(persistenceUnitName, new Properties());
    }

    public PersistenceManager(String persistenceUnitName, Map conf) {
        emFactory = Persistence.createEntityManagerFactory(persistenceUnitName, conf);
        entityManager = emFactory.createEntityManager();
    }

    public void init(String persistenceUnitName) {

        emFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
        entityManager = emFactory.createEntityManager();
    }

    public void transaction() {
        entityManager.getTransaction().begin();
    }

    public void commit() {
        entityManager.getTransaction().commit();
    }

    public void rollback() {
        entityManager.getTransaction().rollback();
    }

    public void persist(Object data) {
        entityManager.persist(data);
    }

    public Query createQuery(String queryString) {
        return entityManager.createQuery(queryString);
    }

    public Query createNativeQuery(String queryString) {
        return entityManager.createNativeQuery(queryString);
    }

    public Query createNativeQuery(String queryString, Class resultClass) {
        return entityManager.createNativeQuery(queryString, resultClass);
    }

    public Object pick(String queryString, String... parameter) {

        final Query query = createQuery(queryString);
        for (int i=0; i<parameter.length; ++i) {
            query.setParameter(i+1, parameter[i]);
        }
        List result = query.getResultList();

        if (result.isEmpty()) {
            return null;
        } else if (result.size() == 1) {
            return result.get(0);
        } else {
            throw new NonUniqueResultException("single result instead of " + result.size() + " expected for query " + query.toString());
        }
    }

    public Object pickNative(String queryString, Object... parameter) {

        final Query query = createNativeQuery(queryString);
        for (int i=0; i<parameter.length; ++i) {
            query.setParameter(i+1, parameter[i]);
        }
        List result = query.getResultList();

        if (result.isEmpty()) {
            return null;
        } else if (result.size() == 1) {
            return result.get(0);
        } else {
            throw new NonUniqueResultException("single result instead of " + result.size() + " expected for query " + query.toString());
        }
    }

    public void clearEntityManager() {
        entityManager.clear();
    }
}
