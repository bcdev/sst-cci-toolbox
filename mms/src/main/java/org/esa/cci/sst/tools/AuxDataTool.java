package org.esa.cci.sst.tools;

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.GlobalObservation;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.RelatedObservation;
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
import java.util.Date;
import java.util.List;

public class AuxDataTool extends BasicTool {

    private static final String NAME = "auxdata-tool.sh";
    private static final String VERSION = "1.0";

    private String sensorName1;
    private String sensorName2;
    private String archiveRootPath;
    private String insituSensorName;
    private int aaiTimeDeltaSeconds;
    private int seaiceTimeDeltaSeconds;
    private String aaiSensorName;
    private String seaiceSensorName;

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

        archiveRootPath = config.getStringValue(Configuration.KEY_MMS_ARCHIVE_ROOT);
        insituSensorName = config.getOptionalStringValue(Configuration.KEY_MMS_SAMPLING_INSITU_SENSOR);

        aaiSensorName = config.getStringValue("mms.matchup.43.sensor");
        seaiceSensorName = config.getStringValue("mms.matchup.44.sensor");

        aaiTimeDeltaSeconds = config.getIntValue("mms.timedelta.aai");
        seaiceTimeDeltaSeconds = config.getIntValue("mms.timedelta.seaice");
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

                addAerosolCoincidence(storage, coincidences, matchupMillis);
                addSeaiceCoincidence(storage, matchupMillis, coincidences, matchup);

            } finally {
                persistenceManager.commit();
            }
        }

        storeMatchups(matchups);
    }

    private void addSeaiceCoincidence(Storage storage, long matchupMillis, List<Coincidence> coincidences, Matchup matchup) {
        final PGgeometry matchupPoint = matchup.getRefObs().getPoint();
        final Point point = matchupPoint.getGeometry().getPoint(0);
        final double lat = point.getY();
        final double lon = point.getX();
        if (Math.abs(lat) > 30.0) {
            final Date seaiceStartDate = new Date(matchupMillis - seaiceTimeDeltaSeconds * 1000);
            final Date seaiceStopDate = new Date(matchupMillis + seaiceTimeDeltaSeconds * 1000);

            final List<RelatedObservation> seaiceObservations = storage.getRelatedObservations(seaiceSensorName, seaiceStartDate, seaiceStopDate);
            for (final RelatedObservation seaiceObservation : seaiceObservations) {
                final Geometry location = seaiceObservation.getLocation().getGeometry();
                final PolarOrbitingPolygon polarOrbitingPolygon = new PolarOrbitingPolygon(0, 0, location);
                if (polarOrbitingPolygon.isPointInPolygon(lat, lon)) {
                    final Coincidence coincidence = new Coincidence();
                    coincidence.setObservation(seaiceObservation);
                    coincidence.setTimeDifference(matchupMillis - seaiceObservation.getTime().getTime());
                    coincidences.add(coincidence);
                }
            }
        }
    }

    private void addAerosolCoincidence(Storage storage, List<Coincidence> coincidences, long matchupMillis) {
        final Date aaiStartDate = new Date(matchupMillis - aaiTimeDeltaSeconds * 1000);
        final Date aaiStopDate = new Date(matchupMillis + aaiTimeDeltaSeconds * 1000);
        final List<GlobalObservation> aaiObservations = storage.getGlobalObservations(aaiSensorName, aaiStartDate, aaiStopDate);
        if (aaiObservations.size() > 0) {
            // @todo 2 tb/tb what shall we do if we have more than one result? 2014-12-10
            final GlobalObservation aaiObservation = aaiObservations.get(0);
            final Coincidence coincidence = new Coincidence();
            coincidence.setObservation(aaiObservation);
            coincidence.setTimeDifference(matchupMillis - aaiObservation.getTime().getTime());
            coincidences.add(coincidence);
        }
    }

    private List<Matchup> loadMatchups() throws IOException {
        final Month centerMonth = ConfigUtil.getCenterMonth(Configuration.KEY_MMS_SAMPLING_START_TIME,
                Configuration.KEY_MMS_SAMPLING_STOP_TIME,
                getConfig());
        final String[] sensorNamesArray = ArchiveUtils.createSensorNamesArray(sensorName1, sensorName2,
                                                                              insituSensorName);
        final String inputFilePath = ArchiveUtils.createCleanFilePath(archiveRootPath, sensorNamesArray, centerMonth.getYear(), centerMonth.getMonth());
        final File inFile = new File(inputFilePath);

        return MatchupIO.read(new FileInputStream(inFile));
    }

    private void storeMatchups(List<Matchup> matchups) throws IOException {
        final Month centerMonth = ConfigUtil.getCenterMonth(Configuration.KEY_MMS_SAMPLING_START_TIME,
                Configuration.KEY_MMS_SAMPLING_STOP_TIME,
                getConfig());
        final String[] sensorNamesArray = ArchiveUtils.createSensorNamesArray(sensorName1, sensorName2,
                                                                              insituSensorName);
        final String outputFilePath = ArchiveUtils.createCleanEnvFilePath(archiveRootPath, sensorNamesArray, centerMonth.getYear(), centerMonth.getMonth());
        final File outFile = FileUtil.createNewFile(outputFilePath);

        MatchupIO.write(matchups, new FileOutputStream(outFile), getConfig(), getPersistenceManager());
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
}
