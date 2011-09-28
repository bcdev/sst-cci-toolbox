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

package org.esa.cci.sst.tools.ingestion;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.data.SensorBuilder;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.MmdReader;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.ExtractDefinitionBuilder;

import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

/**
 * Tool for re-ingesting mmd files with flags to be updated in the database.
 *
 * @author Martin Böttcher
 */
public class FlagsUpdateTool extends BasicTool {

    public static void main(String[] args) {
        final FlagsUpdateTool tool = new FlagsUpdateTool();
        final boolean performWork = tool.setCommandLineArgs(args);
        if (!performWork) {
            return;
        }
        tool.initialize();
        final PersistenceManager persistenceManager = tool.getPersistenceManager();
        try {
            persistenceManager.transaction();
            tool.run();
            persistenceManager.commit();
        } catch (Exception e) {
            try {
                persistenceManager.rollback();
            } catch (Exception _) {
                // ignore
            }
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
    }

    private FlagsUpdateTool() {
        super("flags-tool.sh", "0.1");
    }

    private void run() throws IOException {
        // create reader for MMD'
        final String mmdFilename = getConfiguration().getProperty(Constants.PROPERTY_MMS_REINGESTION_FILENAME);
        final String archiveRootPath = getConfiguration().getProperty("mms.archive.rootdir", ".");
        final MmdReader reader = createReader(mmdFilename, archiveRootPath);
        // determine MMD' variables
        final Item referenceFlagColumn = reader.getColumn("matchup.reference_flag");
        if (referenceFlagColumn != null) {
            getLogger().fine("going to update column referenceflag of matchup reference observations");
        }
        // loop over matchups
        final int numRecords = reader.getNumRecords();
        int count = 0;
        for (int recordNo = 0; recordNo < numRecords; ++recordNo) {
            // look up observation
            final int matchupId = reader.getMatchupId(recordNo);
            final ReferenceObservation observation = (ReferenceObservation) getPersistenceManager().pick("select o from ReferenceObservation o where o.id = ?1", new Integer(matchupId));
            if (observation == null) {
                getLogger().warning(String.format("matchup %d of record %d not found - skipped", matchupId, recordNo));
                continue;
            }
            // update matchup reference observation fields of MMD' variables
            if (referenceFlagColumn != null) {
                final byte referenceFlagValue = reader.read("matchup.reference_flag", new ExtractDefinitionBuilder().recordNo(recordNo).shape(new int[]{1}).build()).getByte(0);
                observation.setReferenceFlag( referenceFlagValue);
            }
            if (referenceFlagColumn != null /* or ... */) {
                ++count;
            }
        }
        getLogger().info(String.format("%d observations updated", count));
    }

    private MmdReader createReader(String mmdFilename, String archiveRootPath) throws IOException {
        final File mmdFile = new File(mmdFilename);
        final File archiveRoot = new File(archiveRootPath);
        final Sensor dummySensor = new SensorBuilder().build();
        final DataFile dataFile = new DataFile(mmdFile, dummySensor);
        final MmdReader reader = new MmdReader(dummySensor.getName());
        reader.setConfiguration(getConfiguration());
        reader.init(dataFile, archiveRoot);
        return reader;
    }
}
