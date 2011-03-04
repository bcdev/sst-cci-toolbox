package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.Variable;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriteable;

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
     * @param dataFileEntry   data file entry to be referenced in each observation created by reader
     */
    void init(DataFile dataFileEntry) throws IOException;

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
     * Sets reference point to pixel corresponding to in-situ measurement.
     * The returned {@link org.esa.cci.sst.data.ReferenceObservation} instance shall have a reference to {@code dataFileEntry}
     * passed into {@link #init(org.esa.cci.sst.data.DataFile)}.
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     * @return Observation with values read from observation file
     */
    Observation readObservation(int recordNo) throws IOException, InvalidRangeException;

    Variable[] getVariables() throws IOException;

    /**
     * Writes the variable from the observation in the file.
     * @param observation The observation to write.
     * @param variable The variable to write.
     * @param file The file to write into.
     * @param matchupIndex
     * @param dimensionSizes
     */
    void write(Observation observation, Variable variable, NetcdfFileWriteable file, int matchupIndex,
               int[] dimensionSizes) throws IOException;
}
