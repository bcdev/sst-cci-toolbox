package org.esa.cci.sst.tools;

import org.esa.cci.sst.data.*;
import org.esa.cci.sst.orm.ColumnStorage;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.orm.Storage;
import org.esa.cci.sst.tools.samplepoint.CloudySubsceneRemover;
import org.esa.cci.sst.tools.samplepoint.OverlapRemover;
import org.esa.cci.sst.tools.samplepoint.SamplePointImporter;
import org.esa.cci.sst.tools.samplepoint.TimeRange;
import org.esa.cci.sst.util.*;
import org.postgis.PGgeometry;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MatchupGenerator extends BasicTool {

    private String sensorName1;
    private String sensorName2;
    private int subSceneWidth;
    private int subSceneHeight;
    private String cloudFlagsVariableName;
    private int cloudFlagsMask;
    private double cloudyPixelFraction;
    private long referenceSensorPattern;
    private String referenceSensorName;

    public MatchupGenerator() {
        super("matchup-generator", "1.0");
    }

    public static void main(String[] args) {
        final MatchupGenerator tool = new MatchupGenerator();
        try {
            final boolean ok = tool.setCommandLineArgs(args);
            if (!ok) {
                tool.printHelp();
                return;
            }
            tool.initialize();
            tool.run();
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Exception e) {
            tool.getErrorHandler().terminate(new ToolException(e.getMessage(), e, ToolException.UNKNOWN_ERROR));
        } finally {
            tool.getPersistenceManager().close();
        }
    }

    @Override
    public void initialize() {
        super.initialize();

        final Configuration config = getConfig();

        sensorName1 = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR);
        sensorName2 = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR_2, null);
        subSceneWidth = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SUBSCENE_WIDTH, 7);
        subSceneHeight = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SUBSCENE_HEIGHT, 7);
        cloudFlagsVariableName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_CLOUD_FLAGS_VARIABLE_NAME);
        cloudFlagsMask = config.getIntValue(Configuration.KEY_MMS_SAMPLING_CLOUD_FLAGS_MASK);
        cloudyPixelFraction = config.getDoubleValue(Configuration.KEY_MMS_SAMPLING_CLOUDY_PIXEL_FRACTION, 0.0);
        referenceSensorName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_REFERENCE_SENSOR);
        referenceSensorPattern = config.getPattern(referenceSensorName, 0);
    }

    private void run() throws IOException {
        cleanupIfRequested();

        final Logger logger = getLogger();

        final List<SamplingPoint> samples = loadSamplePoints(logger);
        removeCloudySamples(logger, samples, sensorName1, true);

        if (sensorName2 != null) {
            removeCloudySamples(logger, samples, sensorName2, false);
        }

        removeOverlappingSamples(logger, samples);
        createMatchups(logger, samples);
    }

    static void createMatchups(List<SamplingPoint> samples, String referenceSensorName, String primarySensorName,
                               String secondarySensorName, long referenceSensorPattern, PersistenceManager pm,
                               Storage storage, Logger logger) {
        final Stack<EntityTransaction> rollbackStack = new Stack<>();
        try {
            // create reference observations
            logInfo(logger, "Starting creating reference observations...");
            rollbackStack.push(pm.transaction());
            final String sensorShortname = createSensorShortName(referenceSensorName, primarySensorName);
            final List<ReferenceObservation> referenceObservations = createReferenceObservations(samples,
                    sensorShortname,
                    storage);
            pm.commit();
            logInfo(logger, "Finished creating reference observations");

            logInfo(logger, "Starting persisting reference observations...");
            persistReferenceObservations(referenceObservations, pm, rollbackStack);
            logInfo(logger, "Finished persisting reference observations");

            logInfo(logger, "Starting creating matchup pattern ...");
            final long matchupPattern = defineMatchupPattern(primarySensorName, secondarySensorName, referenceSensorPattern, pm, rollbackStack);
            logInfo(logger, MessageFormat.format("Matchup pattern: {0}", Long.toHexString(matchupPattern)));

            // create matchups and coincidences
            logInfo(logger, "Starting creating matchups and coincidences...");

            rollbackStack.push(pm.transaction());
            final List<Matchup> matchups = new ArrayList<>(referenceObservations.size());
            final List<Coincidence> coincidences = new ArrayList<>(samples.size());
            final List<InsituObservation> insituObservations = new ArrayList<>(samples.size());
            for (int i = 0; i < samples.size(); i++) {
                final SamplingPoint p = samples.get(i);
                final ReferenceObservation r = referenceObservations.get(i);
                final Matchup matchup = new Matchup();
                matchup.setId(r.getId());
                matchup.setRefObs(r);
                matchup.setPattern(matchupPattern);
                matchups.add(matchup);
                final RelatedObservation o1 = storage.getRelatedObservation(p.getReference());

                final Coincidence coincidence = new Coincidence();
                coincidence.setMatchup(matchup);
                coincidence.setObservation(o1);
                coincidence.setTimeDifference(0.0);
                coincidences.add(coincidence);

                if (secondarySensorName != null) {
                    final RelatedObservation o2 = storage.getRelatedObservation(p.getReference2());
                    final Coincidence secondCoincidence = new Coincidence();
                    secondCoincidence.setMatchup(matchup);
                    secondCoincidence.setObservation(o2);
                    final Date matchupTime = matchup.getRefObs().getTime();
                    final Date relatedTime = new Date(p.getReference2Time());
                    secondCoincidence.setTimeDifference(TimeUtil.getTimeDifferenceInSeconds(matchupTime, relatedTime));
                    coincidences.add(secondCoincidence);
                }
                if (p.getInsituReference() != 0) {
                    final int datafileId = p.getInsituReference();
                    final DataFile insituDatafile = storage.getDatafile(datafileId);
                    final InsituObservation insituObservation = createInsituObservation(p, insituDatafile);
                    insituObservations.add(insituObservation);

                    final Coincidence insituCoincidence = new Coincidence();
                    insituCoincidence.setMatchup(matchup);
                    insituCoincidence.setObservation(insituObservation);
                    insituCoincidence.setTimeDifference(Math.abs(p.getReferenceTime() - p.getTime()) / 1000.0);

                    coincidences.add(insituCoincidence);
                }
            }
            pm.commit();

            logInfo(logger, "Finished creating matchups and coincidences");

            // persist matchups and coincidences
            logInfo(logger, "Starting persisting matchups and coincidences...");

            rollbackStack.push(pm.transaction());
            for (InsituObservation insituObservation : insituObservations) {
                storage.store(insituObservation);
            }
            for (Matchup m : matchups) {
                pm.persist(m);
            }
            for (Coincidence c : coincidences) {
                pm.persist(c);
            }
            pm.commit();

            logInfo(logger, "Finished persisting matchups and coincidences...");
        } catch (Exception e) {
            while (!rollbackStack.isEmpty()) {
                rollbackStack.pop().rollback();
            }
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
    }

    // package access for testing only tb 2014-04-03
    static long defineMatchupPattern(String primarySensorName, String secondarySensorName, long referenceSensorPattern, PersistenceManager pm, Stack<EntityTransaction> rollbackStack) {
        long matchupPattern;
        final Storage storage = pm.getStorage();
        rollbackStack.push(pm.transaction());

        final String primaryOrbitName = SensorNames.ensureOrbitName(primarySensorName);
        final Sensor primarySensor = storage.getSensor(primaryOrbitName);
        if (secondarySensorName != null) {
            final String secondaryOrbitName = SensorNames.ensureOrbitName(secondarySensorName);
            final Sensor secondarySensor = storage.getSensor(secondaryOrbitName);
            matchupPattern = referenceSensorPattern | primarySensor.getPattern() | secondarySensor.getPattern();
        } else {
            matchupPattern = referenceSensorPattern | primarySensor.getPattern();
        }
        pm.commit();
        return matchupPattern;
    }

    private static InsituObservation createInsituObservation(SamplingPoint p, DataFile insituDatafile) {
        final InsituObservation insituObservation = new InsituObservation();
        insituObservation.setName(p.getDatasetName());
        insituObservation.setDatafile(insituDatafile);
        insituObservation.setRecordNo(p.getIndex());
        insituObservation.setSensor(Constants.SENSOR_NAME_HISTORY);
        final PGgeometry insituLocation = GeometryUtil.createPointGeometry(p.getLon(), p.getLat());
        insituObservation.setLocation(insituLocation);
        insituObservation.setTime(new Date(p.getTime()));
        insituObservation.setTimeRadius(Math.abs(p.getReferenceTime() - p.getTime()) / 1000.0);
        return insituObservation;
    }

    // package access for testing only tb 2014-04-03
    static void persistReferenceObservations(List<ReferenceObservation> referenceObservations, PersistenceManager pm, Stack<EntityTransaction> rollbackStack) {
        rollbackStack.push(pm.transaction());
        for (final ReferenceObservation r : referenceObservations) {
            pm.persist(r);
        }
        pm.commit();
    }

    static String createSensorShortName(String referenceSensorName, String primarySensorName) {
        return referenceSensorName.substring(0, 3) + "_" + SensorNames.ensureStandardName(primarySensorName);
    }

    private static void logInfo(Logger logger, String message) {
        if (logger != null && logger.isLoggable(Level.INFO)) {
            logger.info(message);
        }
    }

    private static List<ReferenceObservation> createReferenceObservations(List<SamplingPoint> samples,
                                                                          String referenceSensorName,
                                                                          Storage storage) {
        final List<ReferenceObservation> referenceObservations = new ArrayList<>(samples.size());
        for (final SamplingPoint samplingPoint : samples) {
            final Observation o = storage.getObservation(samplingPoint.getReference());
            final DataFile datafile = o.getDatafile();

            final ReferenceObservation r = createReferenceObservation(referenceSensorName, samplingPoint, datafile);

            referenceObservations.add(r);
        }
        return referenceObservations;
    }

    // package access for testing only tb 2014-03-19
    static ReferenceObservation createReferenceObservation(String referenceSensorName, SamplingPoint samplingPoint,
                                                           DataFile datafile) {
        final ReferenceObservation r = new ReferenceObservation();
        r.setName(String.valueOf(samplingPoint.getIndex()));
        r.setSensor(referenceSensorName);

        final PGgeometry location = GeometryUtil.createPointGeometry(samplingPoint.getReferenceLon(),
                samplingPoint.getReferenceLat());
        r.setLocation(location);
        r.setPoint(location);

        final Date time = new Date(samplingPoint.getReferenceTime());
        r.setTime(time);
        if (samplingPoint.isInsitu()) {
            r.setTimeRadius(Math.abs(samplingPoint.getReferenceTime() - samplingPoint.getTime()) / 1000.0);
        } else {
            r.setTimeRadius(0.0);
        }

        r.setDatafile(datafile);
        r.setRecordNo(0);
        r.setDataset(samplingPoint.getInsituDatasetId().getValue());
        r.setReferenceFlag(Constants.MATCHUP_REFERENCE_FLAG_UNDEFINED);
        return r;
    }

    private OverlapRemover createOverlapRemover() {
        return new OverlapRemover(subSceneWidth, subSceneHeight);
    }

    private void cleanupIfRequested() {
        final Configuration config = getConfig();
        if (config.getBooleanValue(Configuration.KEY_MMS_SAMPLING_CLEANUP)) {
            cleanup();
        } else if (config.getBooleanValue(Configuration.KEY_MMS_SAMPLING_CLEANUP_INTERVAL)) {
            cleanupInterval();
        }
    }


    private void cleanup() {
        getPersistenceManager().transaction();

        Query delete = getPersistenceManager().createQuery("delete from Coincidence c");
        delete.executeUpdate();
        delete = getPersistenceManager().createQuery("delete from Matchup m");
        delete.executeUpdate();
        delete = getPersistenceManager().createQuery(
                // TODO - check this, because sensor name for reference observations is built according to the pattern referenceSensorName.substring(0, 3) + "_" + SensorNames.ensureStandardName(primarySensorName),
                "delete from Observation o where o.sensor = '" + Constants.SENSOR_NAME_DUMMY + "'");
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

    private void cleanupInterval() {
        getPersistenceManager().transaction();

        final TimeRange timeRange = ConfigUtil.getTimeRange(Configuration.KEY_MMS_SAMPLING_START_TIME,
                Configuration.KEY_MMS_SAMPLING_STOP_TIME,
                getConfig());
        final Date startDate = timeRange.getStartDate();
        final Date stopDate = timeRange.getStopDate();
        Query delete = getPersistenceManager().createNativeQuery(
                // TODO - check this, because sensor name for reference observations is built according to the pattern referenceSensorName.substring(0, 3) + "_" + SensorNames.ensureStandardName(primarySensorName),
                "delete from mm_coincidence c where exists ( select r.id from mm_observation r where c.matchup_id = r.id and r.time >= ?1 and r.time < ?2 and r.sensor = '" + Constants.SENSOR_NAME_DUMMY + "')");
        delete.setParameter(1, startDate);
        delete.setParameter(2, stopDate);
        delete.executeUpdate();
        delete = getPersistenceManager().createNativeQuery(
                // TODO - check this, because sensor name for reference observations is built according to the pattern referenceSensorName.substring(0, 3) + "_" + SensorNames.ensureStandardName(primarySensorName),
                "delete from mm_matchup m where exists ( select r from mm_observation r where m.refobs_id = r.id and r.time >= ?1 and r.time < ?2 and r.sensor = '" + Constants.SENSOR_NAME_DUMMY + "')");
        delete.setParameter(1, startDate);
        delete.setParameter(2, stopDate);
        delete.executeUpdate();
        delete = getPersistenceManager().createNativeQuery(
                // TODO - check this, because sensor name for reference observations is built according to the pattern referenceSensorName.substring(0, 3) + "_" + SensorNames.ensureStandardName(primarySensorName),
                "delete from mm_observation r where r.time >= ?1 and r.time < ?2 and r.sensor = '" + Constants.SENSOR_NAME_DUMMY + "'");
        delete.setParameter(1, startDate);
        delete.setParameter(2, stopDate);
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

    private void createMatchups(Logger logger, List<SamplingPoint> samples) {
        logInfo(logger, "Starting creating matchups...");
        createMatchups(samples, referenceSensorName, sensorName1, sensorName2, referenceSensorPattern,
                getPersistenceManager(), getStorage(), logger);
        logInfo(logger, "Finished creating matchups...");
    }

    private void removeOverlappingSamples(Logger logger, List<SamplingPoint> samples) {
        logInfo(logger, "Starting removing overlapping samples...");
        final OverlapRemover overlapRemover = createOverlapRemover();
        overlapRemover.removeSamples(samples);
        logInfo(logger, "Finished removing overlapping samples (" + samples.size() + " samples left)");
    }

    private void removeCloudySamples(Logger logger, List<SamplingPoint> samples, String sensorName, boolean isPrimary) {
        final CloudySubsceneRemover subsceneRemover = new CloudySubsceneRemover();
        final ColumnStorage columnStorage = getPersistenceManager().getColumnStorage();
        subsceneRemover.sensorName(sensorName)
                .primary(isPrimary)
                .subSceneWidth(subSceneWidth)
                .subSceneHeight(subSceneHeight)
                .cloudFlagsVariableName(cloudFlagsVariableName)
                .cloudFlagsMask(cloudFlagsMask)
                .cloudyPixelFraction(cloudyPixelFraction)
                .config(getConfig())
                .storage(getStorage())
                .columnStorage(columnStorage)
                .logger(logger)
                .removeSamples(samples);
    }

    private List<SamplingPoint> loadSamplePoints(Logger logger) throws IOException {
        logInfo(logger, "Starting loading samples...");
        final SamplePointImporter samplePointImporter = new SamplePointImporter(getConfig());
        samplePointImporter.setLogger(logger);
        final List<SamplingPoint> samples = samplePointImporter.load();
        logInfo(logger, "Finished loading samples: (" + samples.size() + " loaded).");
        return samples;
    }
}
