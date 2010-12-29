package org.esa.cci.sst;

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.DataSchema;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.reader.NetcdfMatchupReader;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

/**
 * Tool to ingest new MD files into the MMS database.
 */
public class IngestionTool1 {

    static final String PERSISTENCE_UNIT_NAME = "matchupdb";

    public void ingest(String matchupFilePath, String schemaName, String sensorType) throws Exception {
        NetcdfFile matchupFile = null;
        PersistenceManager persistenceManager = null;
        try {
            // open match-up file
            matchupFile = NetcdfFile.open(matchupFilePath);
            String noOfRecordsDimensionName = (String) System.getProperty(schemaName + ".noofrecordsdimensionname");
            final int numberOfRecords = matchupFile.findDimension(noOfRecordsDimensionName).getLength();

            // open database
            persistenceManager = new PersistenceManager(PERSISTENCE_UNIT_NAME);
            persistenceManager.transaction();

            // lookup or create data schema and data file entry
            DataSchema dataSchema = (DataSchema) persistenceManager.pick("select s from DataSchema s where s.name = ?1", schemaName);
            if (dataSchema == null) {
                dataSchema = new DataSchema();
                dataSchema.setName(schemaName);
                dataSchema.setSensorType(sensorType);
                persistenceManager.persist(dataSchema);
            }

            DataFile dataFile = new DataFile();
            dataFile.setPath(matchupFilePath);
            dataFile.setDataSchema(dataSchema);
            persistenceManager.persist(dataFile);

            // (maybe create observation variable)

            // read in-situ variables
            final NetcdfMatchupReader insituReader = createNetcdfMatchupReader(schemaName);
            insituReader.init(matchupFile, schemaName, "insitu");
            //insituReader.read();

            final NetcdfMatchupReader satelliteReader = createNetcdfMatchupReader(schemaName);
            satelliteReader.init(matchupFile, schemaName, "satellite");
            //satelliteReader.read();

            final NetcdfMatchupReader matchupReader = createNetcdfMatchupReader(schemaName);
            matchupReader.init(matchupFile, schemaName, "matchup");
            //matchupReader.read();

            // loop over records
            for (int recordNo = 0; recordNo < numberOfRecords; ++recordNo) {

                Observation insituObservation = createObservation(insituReader, dataFile, recordNo);
                persistenceManager.persist(insituObservation);

                final Observation satelliteObservation = createObservation(satelliteReader, dataFile, recordNo);
                persistenceManager.persist(satelliteObservation);

                final Coincidence insituCoincidence = createSelfCoincidence(insituObservation);
                persistenceManager.persist(insituCoincidence);

                final Coincidence satelliteCoincidence = createCoincidence(matchupReader, insituObservation, satelliteObservation, recordNo);
                persistenceManager.persist(satelliteCoincidence);
            }

            // make changes in database
            persistenceManager.commit();

        } catch (Exception e) {

            // do not make any change in case of errors
            if (persistenceManager != null) {
                persistenceManager.rollback();
            }
            throw e;

        } finally {

            // close match-up file
            if (matchupFile != null) {
                matchupFile.close();
            }
        }

    }

    private NetcdfMatchupReader createNetcdfMatchupReader(String schemaName)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        String readerNameProperty = schemaName + ".readerclass";
        String readerClassName = System.getProperty(readerNameProperty);
        return (NetcdfMatchupReader) Class.forName(readerClassName).newInstance();
    }

    private Observation createObservation(NetcdfMatchupReader reader, DataFile dataFile, int recordNo) throws IOException, InvalidRangeException {

        final Observation observation = new Observation();
        observation.setName(reader.getString("name", recordNo));
        observation.setSensor(reader.getString("sensor", recordNo));
        observation.setLocation(new PGgeometry(new Point(reader.getCoordinate("longitude", recordNo),
                                                         reader.getCoordinate("latitude", recordNo))));
        observation.setTime(reader.getDate("time", recordNo));
        observation.setDatafile(dataFile);
        observation.setRecordNo(recordNo);
        return observation;
    }

    private Coincidence createSelfCoincidence(Observation observation) {

        final Coincidence coincidence = new Coincidence();
        coincidence.setRefObs(observation);
        coincidence.setObservation(observation);
        return coincidence;
    }

    private Coincidence createCoincidence(NetcdfMatchupReader reader, Observation insituObservation, Observation satelliteObservation, int recordNo) throws IOException, InvalidRangeException {

        final Coincidence coincidence = new Coincidence();
        coincidence.setRefObs(insituObservation);
        coincidence.setObservation(satelliteObservation);
        coincidence.setDistance(reader.getFloat("distance", recordNo));
        coincidence.setTimeDifference(reader.getDouble("timedifference", recordNo));
        return coincidence;
    }
}
