package org.esa.cci.sst;

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.DataSchema;
import org.esa.cci.sst.data.Observation;
import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Tool to ingest new MD files into the MMS database.
 */
public class IngestionTool {

    public void ingest(String matchupFilePath, String schemaName) throws IOException {
        NetcdfFile matchupFile = null;
        EntityManagerFactory emFactory = null;
        EntityManager entityManager = null;
        try {
            // open match-up file
            matchupFile = NetcdfFile.open(matchupFilePath);
            final int numberOfRecords = matchupFile.findDimension("match_up").getLength();

            // open database
            emFactory = Persistence.createEntityManagerFactory("matchupdb");
            entityManager = emFactory.createEntityManager();
            entityManager.getTransaction().begin();

            // lookup or create data schema and data file entry
            DataSchema dataSchema;
            final Query query = entityManager.createQuery("select s from DataSchema s where s.name = :name");  // move to new orm class
            query.setParameter("name", schemaName);
            List result = query.getResultList();
            if (! result.isEmpty()) {
                dataSchema = (DataSchema) result.get(0);
            } else {
                dataSchema = new DataSchema();
                dataSchema.setName(schemaName);
                dataSchema.setSensorType(schemaName);  // TODO introduce sensor type parameter
                entityManager.persist(dataSchema);
            }

            DataFile dataFile = new DataFile();
            dataFile.setPath(matchupFilePath);
            dataFile.setDataSchema(dataSchema);
            entityManager.persist(dataFile);

            // (maybe create observation variables)

            // read in-situ variables
            final Variable insituCallsignVariable = matchupFile.findVariable("insitu_CALLSIGN");
            final Variable insituLatitudeVariable = matchupFile.findVariable("insitu_LATITUDE");
            final Variable insituLongitudeVariable = matchupFile.findVariable("insitu_LONGITUDE");
            final Variable insituTimeJulianVariable = matchupFile.findVariable("insitu_TIME_JULIAN");
            final Variable insituUniqueIdentifierVariable = matchupFile.findVariable("insitu_UNIQUE_IDENTIFIER");

            final ArrayChar.D2 insituCallsign = (ArrayChar.D2) insituCallsignVariable.read();
            final ArrayFloat.D1 insituLatitude = (ArrayFloat.D1) insituLatitudeVariable.read();
            final ArrayFloat.D1 insituLongitude = (ArrayFloat.D1) insituLongitudeVariable.read();
            final ArrayDouble.D1 insituTimeJulian = (ArrayDouble.D1) insituTimeJulianVariable.read();
            final ArrayChar.D2 insituUniqueIdentifier = (ArrayChar.D2) insituUniqueIdentifierVariable.read();

            // read satellite variables
            final Variable satelliteLatitudeVariable = matchupFile.findVariable("aatsr_LATITUDE");
            final Variable satelliteLongitudeVariable = matchupFile.findVariable("aatsr_LONGITUDE");
            final Variable satelliteTimeJulianVariable = matchupFile.findVariable("aatsr_TIME_JULIAN");
            final Variable satelliteOrbitFilenameVariable = matchupFile.findVariable("aatsr_ORBIT_FILENAME");

            final ArrayFloat.D1 satelliteLatitude = (ArrayFloat.D1) satelliteLatitudeVariable.read();
            final ArrayFloat.D1 satelliteLongitude = (ArrayFloat.D1) satelliteLongitudeVariable.read();
            final ArrayDouble.D1 satelliteTimeJulian = (ArrayDouble.D1) satelliteTimeJulianVariable.read();
            final ArrayChar.D2 satelliteOrbitFilename = (ArrayChar.D2) satelliteOrbitFilenameVariable.read();

            // read matchup variables
            final Variable matchupDistanceVariable = matchupFile.findVariable("matchup_DISTANCE");
            final Variable matchupTimeDifferenceVariable = matchupFile.findVariable("matchup_TIME_DIFFERENCE");

            final ArrayFloat.D1 matchupDistance = (ArrayFloat.D1) matchupDistanceVariable.read();
            final ArrayDouble.D1 matchupTimeDifference = (ArrayDouble.D1) matchupTimeDifferenceVariable.read();

            // loop over records
            for (int recordNo = 0; recordNo < Math.min(3, numberOfRecords); ++recordNo) {

                // extract in-situ values
                String insituCallsignValue = insituCallsign.getString(recordNo);
                float insituLatitudeValue = insituLatitude.get(recordNo);
                float insituLongitudeValue = insituLongitude.get(recordNo);
                double insituTimeValue =  insituTimeJulian.get(recordNo);
                String insituUniqueIdentifierValue = insituUniqueIdentifier.getString(recordNo);

                // create in-situ observation entry
                final Observation insituObservation = new Observation();
                insituObservation.setName(insituUniqueIdentifierValue);
                insituObservation.setLocation(new PGgeometry(new Point(insituLongitudeValue, insituLatitudeValue)));
                insituObservation.setTime(dateOfJulianDate(insituTimeValue));
                insituObservation.setDatafile(dataFile);
                insituObservation.setRecordNo(recordNo);
                entityManager.persist(insituObservation);

                // extract satellite values
                float satelliteLatitudeValue = satelliteLatitude.get(recordNo);
                float satelliteLongitudeValue = satelliteLongitude.get(recordNo);
                double satelliteTimeValue =  satelliteTimeJulian.get(recordNo);
                String satelliteOrbitFilenameValue = satelliteOrbitFilename.getString(recordNo);

                // create satellite observation entry
                final Observation satelliteObservation = new Observation();
                satelliteObservation.setName(satelliteOrbitFilenameValue);
                satelliteObservation.setLocation(new PGgeometry(new Point(satelliteLongitudeValue, satelliteLatitudeValue)));
                satelliteObservation.setTime(dateOfJulianDate(satelliteTimeValue));
                satelliteObservation.setDatafile(dataFile);
                satelliteObservation.setRecordNo(recordNo);
                entityManager.persist(satelliteObservation);

                // extract matchup values
                float matchupDistanceValue = matchupDistance.get(recordNo);
                double matchupTimeDifferenceValue = matchupTimeDifference.get(recordNo);

                // create coincidence entry
                final Coincidence insituCoincidence = new Coincidence();
                insituCoincidence.setRefObs(insituObservation);
                insituCoincidence.setObservation(insituObservation);
                insituCoincidence.setDistance(0.0f);
                insituCoincidence.setTimeDifference(0.0d);
                entityManager.persist(insituCoincidence);

                final Coincidence satelliteCoincidence = new Coincidence();
                satelliteCoincidence.setRefObs(insituObservation);
                satelliteCoincidence.setObservation(satelliteObservation);
                satelliteCoincidence.setDistance(matchupDistanceValue);
                satelliteCoincidence.setTimeDifference(matchupTimeDifferenceValue);
                entityManager.persist(satelliteCoincidence);
            }

            // make changes in database
            entityManager.getTransaction().commit();

        } catch (Exception e) {

            // do not make any change in case of errors
            if (entityManager != null) {
                entityManager.getTransaction().rollback();
            }

        } finally {

            // close match-up file
            if (matchupFile != null) {
                matchupFile.close();
            }
        }

    }

    private Date dateOfJulianDate(double julianDate) {
        return new Date((long) ((julianDate - 2440587.5) * 86400000l));
    }

/*
    public static void main(String[] args) throws IOException {
        NetcdfFile netcdfFile = NetcdfFile.open(args[0]);
        List<Variable> variableList = netcdfFile.getVariables();
        for (Variable variable : variableList) {
            Dimension recDim = variable.getDimension(0);
            System.out.println(recDim);
        }
        netcdfFile.close();
    }
*/
}
