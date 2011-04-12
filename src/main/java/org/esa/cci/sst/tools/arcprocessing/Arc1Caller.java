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

package org.esa.cci.sst.tools.arcprocessing;

import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.MmsTool;

import javax.persistence.Query;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for calling ARC1 for AVHRR GAC files.
 *
 * @author Thomas Storm
 */
class Arc1Caller {

    private static final String GET_AVHRR_FILES = "SELECT path " +
                                                  "FROM mm_datafile " +
                                                  "WHERE path LIKE '%AVHRR_GAC%'";

    private static final String ARC1_EXECUTABLE = "/exports/work/geos_gc_sst_cci/avhrr/scripts/start_LOC.bash %s";
    private final MmsTool tool;
    private List<File> processedFiles;

    Arc1Caller(final MmsTool tool) {
        this.tool = tool;
    }

    List<File> processAllAvhrrFiles() {
        final List<String> avhrrFiles = getAvhrrFilePaths();
        final List<String> calls = createCalls(avhrrFiles);
        performCalls(calls);
        return processedFiles;
    }

    private List<String> createCalls(final List<String> avhrrFiles) {
        final List<String> calls = new ArrayList<String>();
        for (String avhrrFile : avhrrFiles) {
            String call = createCall(avhrrFile);
            calls.add(call);
        }
        return calls;
    }

    private String createCall(final String avhrrFile) {
        return String.format(ARC1_EXECUTABLE, avhrrFile);
    }

    @SuppressWarnings({"unchecked"})
    List<String> getAvhrrFilePaths() {
        final PersistenceManager persistenceManager = tool.getPersistenceManager();
        persistenceManager.transaction();
        List<String> avhrrFiles;
        try {
            final Query query = persistenceManager.createNativeQuery(GET_AVHRR_FILES);
            avhrrFiles = query.getResultList();
        } finally {
            persistenceManager.commit();
        }

        return avhrrFiles;
    }

    private void performCalls(final List<String> calls) {
        for (String call : calls) {
            performCall(call);
        }
    }

    private void performCall(final String call) {
    }

}
