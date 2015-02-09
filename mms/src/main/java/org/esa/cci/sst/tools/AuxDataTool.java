package org.esa.cci.sst.tools;

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.GlobalObservation;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.orm.Storage;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.tools.matchup.MatchupIO;
import org.esa.cci.sst.tools.overlap.PolarOrbitingPolygon;
import org.esa.cci.sst.tools.samplepoint.ObservationFinder;
import org.esa.cci.sst.util.ConfigUtil;
import org.esa.cci.sst.util.FileUtil;
import org.esa.cci.sst.util.Month;
import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgis.Point;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AuxDataTool extends BasicTool {

    private static final String NAME = "auxdata-tool.sh";
    private static final String VERSION = "1.0";

    private String sensorName1;
    private String sensorName2;
    private String usecaseRootPath;
    private String insituSensorName;
    private CoincidenceDefinitions coincidenceDefinitions;

    protected AuxDataTool() {
        super(NAME, VERSION);
    }

    public static void main(String[] args) {
        final AuxDataTool auxDataTool = new AuxDataTool();

        try {
            if (!auxDataTool.setCommandLineArgs(args)) {
                return;
            }

            auxDataTool.initialize();
            auxDataTool.run();
        } catch (IOException e) {
            auxDataTool.getErrorHandler().terminate(new ToolException(e.getMessage(), e, ToolException.TOOL_IO_ERROR));
        }
    }

    @Override
    public void initialize() {
        super.initialize();

        final Configuration config = getConfig();

        final String[] sensorNames = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR).split(",", 2);
        sensorName1 = sensorNames[0];
        if (sensorNames.length > 1) {
            sensorName2 = sensorNames[1];
        }

        usecaseRootPath = ConfigUtil.getUsecaseRootPath(config);
        insituSensorName = config.getOptionalStringValue(Configuration.KEY_MMS_SAMPLING_INSITU_SENSOR);
        coincidenceDefinitions = getCoincidenceDefinitions(config);
    }

    private void run() throws IOException {
        final List<Matchup> matchups = loadMatchups();

        final Storage storage = getStorage();
        final PersistenceManager persistenceManager = getPersistenceManager();

        for (final Matchup matchup : matchups) {
            final Date matchupTime = matchup.getRefObs().getTime();
            final long matchupMillis = matchupTime.getTime();

            final List<Coincidence> coincidences = matchup.getCoincidences();

            try {
                persistenceManager.transaction();

                for (int i = 0; i < coincidenceDefinitions.size(); i++) {
                    addRelatedObservations(coincidenceDefinitions.get(i), storage, matchup);
                }
            } finally {
                persistenceManager.commit();
            }
        }

        storeMatchups(matchups);
    }

    private void addRelatedObservations(CoincidenceDefinition coincidenceDefinition,
                                        Storage storage,
                                        Matchup matchup) {
        final ReferenceObservation referenceObservation = matchup.getRefObs();
        final long matchupMillis = referenceObservation.getTime().getTime();
        final int timeDelta = coincidenceDefinition.getTimeDelta();
        final Date startDate = new Date(matchupMillis - timeDelta * 1000);
        final Date stopDate = new Date(matchupMillis + timeDelta * 1000);
        final List<Coincidence> coincidences = matchup.getCoincidences();
        final String sensorName = coincidenceDefinition.getSensorName();
        final Class<? extends Observation> observationClass = coincidenceDefinition.getObservationClass();

        if (observationClass == GlobalObservation.class) {
            // TODO - use coinciding global observations query
            final List<GlobalObservation> observations = storage.getGlobalObservations(sensorName, startDate, stopDate);
            if (observations.size() > 0) {
                final GlobalObservation observation = observations.get(0); // results are sorted by time
                final Coincidence coincidence = new Coincidence();
                coincidence.setObservation(observation);
                coincidence.setTimeDifference(matchupMillis - observation.getTime().getTime());
                coincidences.add(coincidence);
            }
        } else if (observationClass == RelatedObservation.class) {
            final Point referencePoint = referenceObservation.getPoint().getGeometry().getPoint(0);
            final double lat = referencePoint.getY();
            final double lon = referencePoint.getX();
            if (Constants.SENSOR_NAME_SEAICE.equals(sensorName) && Math.abs(lat) < 30.0) {
                return;
            }
            // TODO - use coinciding related observations query
            final List<RelatedObservation> observations = storage.getRelatedObservations(sensorName,
                                                                                         startDate,
                                                                                         stopDate);
            for (final RelatedObservation observation : observations) {
                final Geometry location = observation.getLocation().getGeometry();
                final PolarOrbitingPolygon polarOrbitingPolygon = new PolarOrbitingPolygon(0, 0, location);
                if (polarOrbitingPolygon.isPointInPolygon(lat, lon)) {
                    final Coincidence coincidence = new Coincidence();
                    coincidence.setObservation(observation);
                    coincidence.setTimeDifference(matchupMillis - observation.getTime().getTime());
                    coincidences.add(coincidence);
                }
            }
        }
    }

    private List<Matchup> loadMatchups() throws IOException {
        final Month centerMonth = ConfigUtil.getCenterMonth(Configuration.KEY_MMS_SAMPLING_START_TIME,
                                                            Configuration.KEY_MMS_SAMPLING_STOP_TIME,
                                                            getConfig());
        final String[] sensorNamesArray = ArchiveUtils.createSensorNamesArray(sensorName1, sensorName2,
                                                                              insituSensorName);
        final String inputFilePath = ArchiveUtils.createCleanFilePath(usecaseRootPath, sensorNamesArray,
                                                                      centerMonth.getYear(), centerMonth.getMonth());
        final File inFile = new File(inputFilePath);

        return MatchupIO.read(new FileInputStream(inFile));
    }

    private void storeMatchups(List<Matchup> matchups) throws IOException {
        final Month centerMonth = ConfigUtil.getCenterMonth(Configuration.KEY_MMS_SAMPLING_START_TIME,
                                                            Configuration.KEY_MMS_SAMPLING_STOP_TIME,
                                                            getConfig());
        final String[] sensorNamesArray = ArchiveUtils.createSensorNamesArray(sensorName1, sensorName2,
                                                                              insituSensorName);
        final String targetFilePath = ArchiveUtils.createCleanEnvFilePath(usecaseRootPath, sensorNamesArray,
                                                                          centerMonth.getYear(),
                                                                          centerMonth.getMonth());
        final File targetFile = FileUtil.createNewFile(targetFilePath);

        MatchupIO.write(matchups, new FileOutputStream(targetFile), getConfig(), getPersistenceManager());
    }

    // package access for testing only tb 2014-12-08
    static ObservationFinder.Parameter createQueryParameter(long time, int delta, String sensorName) {
        final ObservationFinder.Parameter parameter = new ObservationFinder.Parameter();
        parameter.setStartTime(time);
        parameter.setStopTime(time);
        parameter.setSearchTimePast(delta);
        parameter.setSearchTimeFuture(delta);
        parameter.setSensorName(sensorName);
        return parameter;
    }

    private CoincidenceDefinitions getCoincidenceDefinitions(Configuration config) {
        final CoincidenceDefinitions coincidenceDefinitions = new CoincidenceDefinitions();
        for (int i = 0; i < 100; i++) {
            final String sensorKey = String.format("mms.matchup.%d.sensor", i);
            final String sensorName = config.getStringValue(sensorKey, null);
            if (sensorName == null) {
                continue;
            }
            final String timedeltaKey = String.format("mms.matchup.%d.timedelta", i);
            final int timeDelta = config.getIntValue(timedeltaKey, 43200);
            final Sensor sensor = getStorage().getSensor(sensorName);
            if (sensor == null) {
                continue;
            }
            final Class<? extends Observation> observationClass = getObservationClass(sensor);
            if (observationClass == ReferenceObservation.class) {
                continue;
            }
            if (!coincidenceDefinitions.contains(sensorName)) {
                coincidenceDefinitions.add(sensorName, observationClass, timeDelta);
            }
        }
        return coincidenceDefinitions;
    }

    @SuppressWarnings({"unchecked"})
    private static Class<? extends Observation> getObservationClass(Sensor sensor) {
        try {
            return (Class<? extends Observation>) Class.forName(
                    String.format("%s.%s", Sensor.class.getPackage().getName(), sensor.getObservationType()));
        } catch (ClassCastException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class CoincidenceDefinition {

        private String sensorName;
        private Class<? extends Observation> observationClass;
        private int timeDelta;

        public CoincidenceDefinition(String sensorName, Class<? extends Observation> observationClass,
                                     Integer timeDelta) {
            this.sensorName = sensorName;
            this.observationClass = observationClass;
            this.timeDelta = timeDelta;
        }

        public int getTimeDelta() {
            return timeDelta;
        }

        public Class<? extends Observation> getObservationClass() {
            return observationClass;
        }

        public String getSensorName() {
            return sensorName;
        }
    }

    private static class CoincidenceDefinitions {

        private final List<String> sensorNames;
        private final List<Class<? extends Observation>> observationClasses;
        private final List<Integer> timeDeltas;

        CoincidenceDefinitions() {
            sensorNames = new ArrayList<>();
            observationClasses = new ArrayList<>();
            timeDeltas = new ArrayList<>();
        }

        public boolean contains(String sensorName) {
            return sensorNames.contains(sensorName);
        }

        public void add(String sensorName, Class<? extends Observation> observationClass, int timeDelta) {
            sensorNames.add(sensorName);
            observationClasses.add(observationClass);
            timeDeltas.add(timeDelta);
        }

        public int size() {
            return sensorNames.size();
        }

        public CoincidenceDefinition get(int i) {
            return new CoincidenceDefinition(sensorNames.get(i), observationClasses.get(i), timeDeltas.get(i));
        }
    }
}
