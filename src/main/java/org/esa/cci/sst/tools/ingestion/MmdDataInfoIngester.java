/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools.ingestion;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.DataSchema;
import org.esa.cci.sst.orm.PersistenceManager;

import javax.persistence.Query;
import java.text.MessageFormat;
import java.util.logging.Logger;

/**
 * Responsible for re-ingesting the datafile and dataschema of the mmd file.
 *
 * @author Thomas Storm
 */
class MmdDataInfoIngester {

    static final String DATAFILE_ALREADY_INGESTED = "SELECT COUNT (id) " +
                                                    "FROM mm_datafile " +
                                                    "WHERE path = '%s'";

    static final String DATASCHEMA_ALREADY_INGESTED = "SELECT COUNT (id) " +
                                                      "FROM mm_dataschema " +
                                                      "WHERE name = '%s'";

    private final MmdIngester ingester;

    MmdDataInfoIngester(MmdIngester ingester) {
        this.ingester = ingester;
    }

    void ingestDataInfo() {
        final PersistenceManager persistenceManager = ingester.getPersistenceManager();
        persistenceManager.transaction();
        try {
            ingestDataSchema();
            ingestDataFile();
        } finally {
            persistenceManager.commit();
        }
    }

    private void ingestDataSchema() {
        final DataSchema dataSchema = ingester.getDataSchema();
        final String queryString = String.format(DATASCHEMA_ALREADY_INGESTED, dataSchema.getName());
        ingestOnce(dataSchema, queryString);
    }

    private void ingestDataFile() {
        final DataFile dataFile = ingester.getDataFile();
        final String queryString = String.format(DATAFILE_ALREADY_INGESTED, dataFile.getPath());
        ingestOnce(dataFile, queryString);
    }

    private void ingestOnce(final Object data, final String queryString) {
        final PersistenceManager persistenceManager = ingester.getPersistenceManager();
        final Query query = persistenceManager.createNativeQuery(queryString, Integer.class);
        int result = (Integer) query.getSingleResult();
        if (result == 0) {
            persistenceManager.persist(data);
        } else {
            final Logger logger = ingester.getLogger();
            logger.info(
                    MessageFormat.format("Data of type ''{0}'' already ingested.", data.getClass().getSimpleName()));
        }
    }

}
