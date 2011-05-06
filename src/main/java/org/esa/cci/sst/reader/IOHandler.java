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

import org.esa.cci.sst.data.ColumnI;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.postgis.PGgeometry;
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

    /**
     * Returns an array of columns for the variables that are used by the
     * files supported by this IO handler.
     *
     * @return an array of columns.
     *
     * @throws IOException when an error occurred.
     */
    ColumnI[] getColumns() throws IOException;

    /**
     * Writes the variable from the observation in the file.
     *
     * @param targetFile         The file to write into.
     * @param sourceObservation  The observation to write.
     * @param sourceVariableName The name of the source variable to write.
     * @param targetVariableName The name of the target variable to write.
     * @param targetRecordNumber The current matchup index.
     * @param refPoint           The geo-location of the reference observation.
     * @param refTime            The reference time.
     *
     * @throws java.io.IOException If observation data could not be written into the file.
     */
    // todo - supply target column instead of target variable name here (rq-20110420)
    void write(NetcdfFileWriteable targetFile, Observation sourceObservation, String sourceVariableName,
               String targetVariableName, int targetRecordNumber, final PGgeometry refPoint, final Date refTime) throws
                                                                                                                 IOException;


    /**
     * Reads a record of in-situ data.
     * <p/>
     * todo - get rid of this method (rq-20100503)
     *
     * @param recordNo The record number.
     *
     * @return the in-situ data record with record number {@code recordNo}.
     *
     * @throws IOException                    when an error occurred.
     * @throws OperationNotSupportedException when the implementing class does not support this operation.
     */
    InsituRecord readInsituRecord(int recordNo) throws IOException, OperationNotSupportedException;

    /**
     * Returns the data file.
     *
     * @return the data file.
     */
    DataFile getDataFile();
}
