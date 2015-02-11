package org.esa.cci.sst.tools.matchup;

import org.esa.cci.sst.data.*;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.util.StopWatch;
import org.postgis.PGgeometry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class MatchupOutMain {

    private static final double to_MB = 1.0 / (1024.0 * 1024.0);

    public static void main(String[] args) throws SQLException, IOException {
        if (args.length != 2) {
            System.err.println("Need to supply number of matchups and target directory");
            System.exit(1);
        }

        final int numMatchups = Integer.parseInt(args[0]);

        final Runtime runtime = Runtime.getRuntime();
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        traceMemory(runtime);

        final List<Matchup> matchups = new ArrayList<>();
        for (int i = 0; i < numMatchups; i++) {
            final Matchup matchup = new Matchup();

            final ReferenceObservation refObs = new ReferenceObservation();
            final DataFile dataFile = new DataFile();
            final Sensor sensor = new Sensor();
            sensor.setName("sensor_test");
            sensor.setPattern(i);
            sensor.setObservationType("blu" + i);
            dataFile.setSensor(sensor);
            dataFile.setPath("/data/repository/sensor/file_" + i);
            refObs.setDatafile(dataFile);

            refObs.setPoint(new PGgeometry("POINT(12 45)"));
            refObs.setDataset((byte) i);
            refObs.setReferenceFlag((byte) i);
            refObs.setTime(new Date());
            refObs.setTimeRadius(1000.0 + i);

            final RelatedObservation relatedObservation = new RelatedObservation();
            relatedObservation.setTimeRadius(500 + i);
            relatedObservation.setTime(new Date());
            final Coincidence coincidence = new Coincidence();
            coincidence.setObservation(relatedObservation);
            coincidence.setMatchup(matchup);
            coincidence.setTimeDifference(10.6 + i);
            final ArrayList<Coincidence> coincidences = new ArrayList<>();
            matchup.setCoincidences(coincidences);

            matchup.setRefObs(refObs);
            matchup.setId(200000 + i);
            matchup.setInvalid(false);
            matchup.setPattern(34 + i);

            matchups.add(matchup);
        }

        stopWatch.stop();
        System.out.println("assemble data structures " + ((double) stopWatch.getElapsedMillis()) / 1000.0 + " sec");
        traceMemory(runtime);

        final File targetDirectory = new File(args[1]);

        final File file = new File(targetDirectory, "test_data_" + new Date().getTime() + ".json");
        if (!file.createNewFile()) {
            System.err.println("Unable to create file: " + file.getAbsolutePath());
            System.exit(1);
        }
        try {
            final Properties properties = new Properties();
            properties.setProperty(Configuration.KEY_MMS_SAMPLING_START_TIME, "2010-06-01T00:00:00Z");
            properties.setProperty(Configuration.KEY_MMS_SAMPLING_STOP_TIME, "2010-07-01T00:00:00Z");
            properties.setProperty(Configuration.KEY_MMS_SAMPLING_SENSOR, "avhrr.n18");
            final Configuration configuration = new Configuration();
            configuration.add(properties);

            stopWatch.start();
            final FileOutputStream outputStream = new FileOutputStream(file);
            MatchupIO.write(matchups, outputStream, configuration, null);
            outputStream.close();

            stopWatch.stop();

            System.out.println("write data to disk " + ((double) stopWatch.getElapsedMillis()) / 1000.0 + " sec");
            traceMemory(runtime);
            System.out.println("file size: " + file.length() * to_MB + " MB");

        } finally {
            file.delete();
        }
    }

    private static void traceMemory(Runtime runtime) {
        System.out.println("memory: " + (runtime.totalMemory() - runtime.freeMemory()) * to_MB + " MB");
    }
}
