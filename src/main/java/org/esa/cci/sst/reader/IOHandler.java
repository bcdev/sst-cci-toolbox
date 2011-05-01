package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Descriptor;
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

    Descriptor[] getVariableDescriptors() throws IOException;

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
    // todo - supply target descriptor instead of target variable name here (rq-20110420)
    void write(NetcdfFileWriteable targetFile, Observation sourceObservation, String sourceVariableName,
               String targetVariableName, int targetRecordNumber, final PGgeometry refPoint, final Date refTime) throws
                                                                                                                 IOException;


    /**
     * Reads a record of in-situ data.
     *
     * @param recordNo The record number.
     *
     * @return the in-situ data record with record number {@code recordNo}.
     */
    InsituRecord readInsituRecord(int recordNo) throws IOException, OperationNotSupportedException;

    DataFile getDataFile();

}
