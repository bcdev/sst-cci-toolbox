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

package org.esa.cci.sst.orm;

import javax.persistence.*;
import java.util.List;
import java.util.Map;

/**
 * Wrapper around JPA EntityManager to abstract from the persistence
 * implementation (JPA in this case).
 *
 * @author Martin Boettcher
 */
public class PersistenceManager {

    private final EntityManager entityManager;

    public static PersistenceManager create(String persistenceUnitName, int retryCount, Map conf) {
        final EntityManagerFactory factory = Persistence.createEntityManagerFactory(persistenceUnitName, conf);
        for (int i = 0; i < 1 + retryCount; i++) {
            try {
                return new PersistenceManager(factory.createEntityManager());
            } catch (Exception e) {
                if (i == retryCount) {
                    throw e;
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {
                }
            }
        }
        return null; // cannot happen
    }

    public void close() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }

    public Storage getStorage() {
        return new JpaStorage(this);
    }

    public MatchupStorage getMatchupStorage() {
        return new JpaMatchupStorage(this);
    }

    public ColumnStorage getColumnStorage(){
        return new JpaColumnStorage(this);
    }

    public EntityTransaction transaction() {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        return transaction;
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

    public void detach(Object entity) {
        entityManager.detach(entity);
    }

    public void clear() {
        entityManager.clear();
    }

    Object pick(String queryString, Object... parameter) {
        final Query query = createQuery(queryString);
        for (int i = 0; i < parameter.length; ++i) {
            query.setParameter(i + 1, parameter[i]);
        }
        final List result = query.getResultList();

        if (result.isEmpty()) {
            return null;
        } else if (result.size() == 1) {
            return result.get(0);
        } else {
            throw new NonUniqueResultException("Single result instead of " + result.size() + " expected for query '" + query.toString() + "'.");
        }
    }

    private PersistenceManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
