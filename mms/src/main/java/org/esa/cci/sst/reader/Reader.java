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

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.cci.sst.common.ExtractDefinition;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.util.SamplingPoint;
import ucar.ma2.Array;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Creates Observations and reads actual data from input files.
 *
 * @author Martin Boettcher
 */
public interface Reader extends Closeable {

    /**
     * Opens observation file and initialises cache buffer.
     * Overrides shall store the given {@code dataFile} in order to be used in
     * {@link #readObservation(int)}.
     *
     * @param dataFile The data file entry to be referenced in each observation created by reader.
     * @param archiveRoot  archive root directory to be used as prefix for relative paths in datafiles
     *
     * @throws java.io.IOException when an error has occurred.
     */
    void init(DataFile dataFile, File archiveRoot) throws IOException;  // @todo 3 tb/** rename to "open" tb 2014-02-17

    /**
     * Closes observation file
     */
    @Override
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
     * passed into {@link #init(org.esa.cci.sst.data.DataFile,java.io.File)}.
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
     * An ${@link org.esa.cci.sst.common.ExtractDefinition} specifies the section to read.
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
     * Retrieves the list of sampling points contained in the file. i.e. all observations in terms of
     * location and time.
     * @return the list of sampling points or empty list - never null.
     */
    List<SamplingPoint> readSamplingPoints() throws IOException;

    /**
     * Returns a geo-coding for the record given by the record number.
     *
     * @param recordNo The number of the record the geo-coding shall received for.
     *
     * @return A geo-coding.
     *
     * @throws java.io.IOException If some IO error occurs.
     */
    GeoCoding getGeoCoding(int recordNo) throws IOException;

    /**
     * Returns the value of the time delta corresponding to the given record number and scan line.
     *
     * @param recordNo The record number.
     * @param scanLine The scan line.
     *
     * @return The value of the time delta in milliseconds from the center pixel.
     *
     * @throws java.io.IOException If IO fails.
     */
    double getDTime(int recordNo, int scanLine) throws IOException;

    /**
     * Returns the time value corresponding to the given record number and scan line.
     *
     * @param recordNo The record number.
     * @param scanLine The scan line.
     *
     * @return The time, given in milliseconds since 01.01.1970.
     *
     * @throws java.io.IOException If IO fails.
     */
    long getTime(int recordNo, int scanLine) throws IOException;

    /**
     * Returns the number of lines which are skipped by the reader.
     *
     * @return The number of lines skipped by the reader.
     */
    int getLineSkip();

    InsituSource getInsituSource();

    int getScanLineCount();

    int getElementCount();

    String getDatasetName();

    /**
     * If the reader is a product reader, this method returns the product the reader has read. Otherwise,
     * <code>null</code> is returned.
     *
     * @return The product the reader has read, or <code>null</code> if there is no product.
     */
    Product getProduct();

}
