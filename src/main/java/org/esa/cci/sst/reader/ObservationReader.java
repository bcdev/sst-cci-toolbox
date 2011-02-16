package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.Variable;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;

/**
 * Reads records from an input file and creates Observations.
 *
 * @author Martin Boettcher
 */
public interface ObservationReader {
    /**
     * Opens observation file and initialises cache buffer.
     * Overrides shall store the given {@code dataFileEntry} in order to be used in
     * {@link #readObservation(int)}.
     *
     * @param observationFile file of observations in format corresponding to reader
     * @param dataFileEntry   data file entry to be referenced in each observation created by reader
     */
    void init(File observationFile, DataFile dataFileEntry) throws IOException;

    /**
     * Closes observation file
     * @throws IOException
     */
    void close() throws IOException;

    /**
     * Reads numRecords from file attribute
     *
     * @return the number of records contained in the observation file
     */
    int getNumRecords();

    /**
     * Reads observation time of record
     * @param recordNo
     * @return  time in Java Date.getTime() format (milliseconds since 1970)
     * @throws IOException
     * @throws InvalidRangeException
     */
    long getTime(int recordNo) throws IOException, InvalidRangeException;

    /**
     * Reads record and retrieves variables for a common observation.
     * Sets geo-location to polygon enclosing subscene.
     * The returned {@link Observation} instance shall have a reference to {@code dataFileEntry}
     * passed into {@link #init(java.io.File, org.esa.cci.sst.data.DataFile)}.
     *
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     * @return Observation with values read from observation file
     */
    Observation readObservation(int recordNo) throws IOException, InvalidRangeException;

    /**
     * Reads record and retrieves variables for a reference observation.
     * Sets geo-location to pixel corresponding to in-situ measurement.
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     * @return Observation with values read from observation file
     */
    Observation readRefObs(int recordNo) throws IOException, InvalidRangeException;

    Variable[] getVariables() throws IOException;
}
