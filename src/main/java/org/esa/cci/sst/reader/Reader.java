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

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Observation;
import ucar.ma2.Array;

import java.io.IOException;

/**
 * Creates Observations and reads actual data from input files.
 *
 * @author Martin Boettcher
 */
public interface Reader {

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
     * Reads numRecords from file attribute.
     *
     * @return the number of records contained in the observation file.
     */
    int getNumRecords();

    /**
     * Retrieves variables for a common observation.
     * Sets geo-location to polygon enclosing subscene.
     * Sets reference point to pixel corresponding to in-situ measurement.
     * The returned {@link org.esa.cci.sst.data.ReferenceObservation} instance shall have a reference to {@code dataFileEntry}
     * passed into {@link #init(org.esa.cci.sst.data.DataFile)}.
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords.
     *
     * @return Observation with values read from observation file, never null.
     *
     * @throws java.io.IOException if observation could not be read.
     */
    Observation readObservation(int recordNo) throws IOException;

    /**
     * Reads actual data from the variable given by <code>role</code>.
     * An ${@link ExtractDefinition} specifies the section to read.
     *
     * @param role              The name of the variable to read from.
     * @param extractDefinition The extract definition specifying the section from which to read.
     *
     * @return Actual data.
     *
     * @throws IOException If variable could not be read in the specified section.
     */
    Array read(String role, ExtractDefinition extractDefinition) throws IOException;

    /**
     * Returns the column for the given variable name.
     *
     * @param role The variable name.
     *
     * @return The column for the given variable name.
     */
    Item getColumn(String role);

    /**
     * Returns an array of columns for the variables that are used by the
     * files supported by this reader.
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

    /**
     * Returns the pixel position at the given geographic position. The pixel position denotes scan line and element
     * line, where <code>PixelPos.x</code> corresponds to element line and <code>PixelPos.y</code> corresponds to scan
     * line.
     *
     * @param geoPos The geo position to get the pixel position for.
     *
     * @return The pixel position at the given geo position.
     *
     * @throws java.io.IOException If IO fails.
     */
    PixelPos getPixelPos(GeoPos geoPos) throws IOException;

    /**
     * Returns the value of the time delta corresponding to the given record number and scan line.
     *
     * @param recordNo The record number.
     * @param scanLine The scan line.
     *
     * @return The value of the time delta.
     *
     * @throws java.io.IOException If IO fails.
     */
    int getDTime(int recordNo, int scanLine) throws IOException;

    /**
     * Returns the time value corresponding to the given record number and scan line.
     *
     * @param recordNo The record number.
     * @param scanLine The scan line.
     *
     * @return The time.
     *
     * @throws java.io.IOException If IO fails.
     */
    int getTime(int recordNo, int scanLine) throws IOException;

}
