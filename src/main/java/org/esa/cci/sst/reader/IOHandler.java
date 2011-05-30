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

package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Observation;
import org.postgis.PGgeometry;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFileWriteable;

import javax.naming.OperationNotSupportedException;
import java.io.IOException;
import java.util.Date;

/**
 * Reads records from an input file and creates Observations.
 *
 * @author Martin Boettcher
 */
public interface IOHandler {

    /**
     * Opens observation file and initialises cache buffer.
     * Overrides shall store the given {@code dataFile} in order to be used in
     * {@link #readObservation(int)}.
     *
     * @param dataFile The data file entry to be referenced in each observation created by reader.
     *
     * @throws java.io.IOException when an error has occurred.
     */
    void init(DataFile dataFile) throws IOException;

    /**
     * Closes observation file
     */
    void close();

    /**
     * Reads numRecords from file attribute
     *
     * @return the number of records contained in the observation file
     */
    int getNumRecords();

    /**
     * Reads record and retrieves variables for a common observation.
     * Sets geo-location to polygon enclosing subscene.
     * Sets reference point to pixel corresponding to in-situ measurement.
     * The returned {@link org.esa.cci.sst.data.ReferenceObservation} instance shall have a reference to {@code dataFileEntry}
     * passed into {@link #init(org.esa.cci.sst.data.DataFile)}.
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     *
     * @return Observation with values read from observation file, never null
     *
     * @throws java.io.IOException if observation could not be read.
     */
    Observation readObservation(int recordNo) throws IOException;

    Array read(String role, ExtractDefinition extractDefinition) throws IOException;

    Item getColumn(String role);

    /**
     * Returns an array of columns for the variables that are used by the
     * files supported by this IO handler.
     *
     * @return an array of columns.
     */
    Item[] getColumns();

    /**
     * Returns the data file.
     *
     * @return the data file.
     */
    DataFile getDatafile();
}
