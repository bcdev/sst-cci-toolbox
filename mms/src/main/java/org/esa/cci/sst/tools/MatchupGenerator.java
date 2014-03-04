package org.esa.cci.sst.tools;

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.data.SensorBuilder;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.samplepoint.CloudySubsceneRemover;
import org.esa.cci.sst.tools.samplepoint.OverlapRemover;
import org.esa.cci.sst.tools.samplepoint.SamplePointImporter;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;
import org.postgis.Point;

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

    private long startTime;
    private long stopTime;
    private String sensorName1;
    private String sensorName2;
    private int subSceneWidth;
    private int subSceneHeight;
    private String cloudFlagsVariableName;
    private int cloudFlagsMask;
    private double cloudyPixelFraction;
    private long referenceSensorPattern;

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
        }
    }

    @Override
    public void initialize() {
        super.initialize();

        final Configuration config = getConfig();
        startTime = config.getDateValue(Configuration.KEY_MMS_SAMPLING_START_TIME).getTime();
        stopTime = config.getDateValue(Configuration.KEY_MMS_SAMPLING_STOP_TIME).getTime();
        sensorName1 = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR);
        sensorName2 = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR_2);
        subSceneWidth = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SUBSCENE_WIDTH, 7);
        subSceneHeight = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SUBSCENE_HEIGHT, 7);
        cloudFlagsVariableName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_CLOUD_FLAGS_VARIABLE_NAME);
        cloudFlagsMask = config.getIntValue(Configuration.KEY_MMS_SAMPLING_CLOUD_FLAGS_MASK);
        cloudyPixelFraction = config.getDoubleValue(Configuration.KEY_MMS_SAMPLING_CLOUDY_PIXEL_FRACTION, 0.0);
        referenceSensorPattern = config.getPattern(Constants.SENSOR_NAME_SOBOL);
    }

    private void run() throws IOException {
        cleanupIfRequested();

        final SamplePointImporter samplePointImporter = new SamplePointImporter(getConfig());
        samplePointImporter.setLogger(getLogger());
        if (getLogger() != null && getLogger().isLoggable(Level.INFO)) {
            final String message = "Starting loading samples...";
            getLogger().info(message);
        }
        final List<SamplingPoint> samples = samplePointImporter.load();
        if (getLogger() != null && getLogger().isLoggable(Level.INFO)) {
            final String message = "Finished loading samples: (" + samples.size() + " loaded).";
            getLogger().info(message);
        }

        final CloudySubsceneRemover subsceneRemover = new CloudySubsceneRemover();
        subsceneRemover.sensorName(sensorName1)
                .primary(true)
                .subSceneWidth(subSceneWidth)
                .subSceneHeight(subSceneHeight)
                .cloudFlagsVariableName(cloudFlagsVariableName)
                .cloudFlagsMask(cloudFlagsMask)
                .cloudyPixelFraction(cloudyPixelFraction)
                .config(getConfig())
                .storage(getStorage())
                .logger(getLogger())
                .removeSamples(samples);

        if (sensorName2 != null) {
            // todo - find observations for secondary sensor
            // todo - remove observations for secondary sensor
            // subsceneRemover.sensorName(sensorName2).primary(false).removeSamples(samples);
        }

        final OverlapRemover overlapRemover = createOverlapRemover();
        if (getLogger() != null && getLogger().isLoggable(Level.INFO)) {
            final String message = "Starting removing overlapping samples...";
            getLogger().info(message);
        }
        overlapRemover.removeSamples(samples);
        if (getLogger() != null && getLogger().isLoggable(Level.INFO)) {
            final String message = "Finished removing overlapping samples (" + samples.size() + " samples left)";
            getLogger().info(message);
        }

        if (getLogger() != null && getLogger().isLoggable(Level.INFO)) {
            final String message = "Starting creating matchups...";
            getLogger().info(message);
        }
        createMatchups(samples, Constants.SENSOR_NAME_SOBOL, sensorName1, sensorName2, referenceSensorPattern,
                       getPersistenceManager(), getStorage(), getLogger());
        if (getLogger() != null && getLogger().isLoggable(Level.INFO)) {
            final String message = "Finished creating matchups...";
            getLogger().info(message);
        }
    }

    static void createMatchups(List<SamplingPoint> samples, String referenceSensorName, String primarySensorName,
                               String secondarySensorName, long referenceSensorPattern, PersistenceManager pm,
                               Storage storage, Logger logger) {
        final Stack<EntityTransaction> transactions = new Stack<>();
        try {
            // 1. create reference sensor, if not already present
            transactions.push(pm.transaction());
            if (storage.getSensor(referenceSensorName) == null) {
                if (logger != null && logger.isLoggable(Level.INFO)) {
                    final String message = MessageFormat.format("Starting persisting reference sensor: {0}",
                                                                referenceSensorName);
                    logger.info(message);
                }
                final Sensor sensor = new SensorBuilder()
                        .name(referenceSensorName)
                        .observationType(ReferenceObservation.class)
                        .pattern(referenceSensorPattern)
                        .build();
                pm.persist(sensor);
                if (logger != null && logger.isLoggable(Level.INFO)) {
                    final String message = MessageFormat.format("Finished persisting reference sensor: {0}",
                                                                referenceSensorName);
                    logger.info(message);
                }
            }
            pm.commit();

            // 2. create reference observations
            if (logger != null && logger.isLoggable(Level.INFO)) {
                final String message = "Starting creating reference observations...";
                logger.info(message);
            }
            transactions.push(pm.transaction());
            final List<ReferenceObservation> referenceObservations = createReferenceObservations(samples,
                                                                                                 referenceSensorName,
                                                                                                 storage);
            pm.commit();
            if (logger != null && logger.isLoggable(Level.INFO)) {
                final String message = "Finished creating reference observations";
                logger.info(message);
            }

            // 3. persist reference observations, because we need the ID
            if (logger != null && logger.isLoggable(Level.INFO)) {
                final String message = "Starting persisting reference observations...";
                logger.info(message);
            }
            transactions.push(pm.transaction());
            for (final ReferenceObservation r : referenceObservations) {
                pm.persist(r);
            }
            pm.commit();
            if (logger != null && logger.isLoggable(Level.INFO)) {
                final String message = "Finished persisting reference observations";
                logger.info(message);
            }

            // 4. define matchup pattern
            transactions.push(pm.transaction());
            final long matchupPattern;
            if (secondarySensorName != null) {
                matchupPattern = referenceSensorPattern |
                                 storage.getSensor(primarySensorName).getPattern() |
                                 storage.getSensor(secondarySensorName).getPattern();
            } else {
                matchupPattern = referenceSensorPattern | storage.getSensor(primarySensorName).getPattern();
            }
            pm.commit();
            if (logger != null && logger.isLoggable(Level.INFO)) {
                final String message = MessageFormat.format("Matchup pattern: {0}", Long.toHexString(matchupPattern));
                logger.info(message);
            }

            // 5. create matchups and coincidences
            if (logger != null && logger.isLoggable(Level.INFO)) {
                final String message = "Starting creating matchups and coincidences...";
                logger.info(message);
            }
            transactions.push(pm.transaction());
            final List<Matchup> matchups = new ArrayList<>(referenceObservations.size());
            final List<Coincidence> coincidences = new ArrayList<>(samples.size());
            for (int i = 0; i < samples.size(); i++) {
                final SamplingPoint p = samples.get(i);
                final ReferenceObservation r = referenceObservations.get(i);
                final Matchup matchup = new Matchup();
                matchup.setId(r.getId());
                matchup.setRefObs(r);
                // @todo 2 tb/** check pattern when using with insitu data - we may have to add a "| historyPattern" here   tb 2014-02-12
                matchup.setPattern(matchupPattern);
                matchups.add(matchup);
                final RelatedObservation o1 = storage.getRelatedObservation(p.getReference());
                final Coincidence c1 = new Coincidence();
                c1.setMatchup(matchup);
                c1.setObservation(o1);
                // @todo 2 tb/** check for insitu - we may want to keep the *real* time delta tb 2014-02-12
                c1.setTimeDifference(0.0);
                coincidences.add(c1);
                if (secondarySensorName != null) {
                    final RelatedObservation o2 = storage.getRelatedObservation(p.getReference2());
                    final Coincidence c2 = new Coincidence();
                    c2.setMatchup(matchup);
                    c2.setObservation(o2);
                    final Date matchupTime = matchup.getRefObs().getTime();
                    final Date relatedTime = o2.getTime();
                    c2.setTimeDifference(TimeUtil.getTimeDifferenceInSeconds(matchupTime, relatedTime));

                    coincidences.add(c2);
                }
            }
            pm.commit();
            if (logger != null && logger.isLoggable(Level.INFO)) {
                final String message = "Finished creating matchups and coincidences";
                logger.info(message);
            }

            // 6. persist matchups and coincidences
            if (logger != null && logger.isLoggable(Level.INFO)) {
                final String message = "Starting persisting matchups and coincidences...";
                logger.info(message);
            }
            transactions.push(pm.transaction());
            for (Matchup m : matchups) {
                pm.persist(m);
            }
            for (Coincidence c : coincidences) {
                pm.persist(c);
            }
            pm.commit();
            if (logger != null && logger.isLoggable(Level.INFO)) {
                final String message = "Finished persisting matchups and coincidences...";
                logger.info(message);
            }
        } catch (Exception e) {
            while (!transactions.isEmpty()) {
                transactions.pop().rollback();
            }
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
    }

    private static List<ReferenceObservation> createReferenceObservations(List<SamplingPoint> samples,
                                                                          String referenceSensorName,
                                                                          Storage storage) {
        final List<ReferenceObservation> referenceObservations = new ArrayList<>(samples.size());
        for (final SamplingPoint p : samples) {
            final ReferenceObservation r = new ReferenceObservation();
            r.setName(String.valueOf(p.getIndex()));
            r.setSensor(referenceSensorName);

            final PGgeometry location = new PGgeometry(new Point(p.getLon(), p.getLat()));
            r.setLocation(location);
            r.setPoint(location);

            // @todo 2 tb/** check for insitu - we may want to keep the *real* time delta tb 2014-02-12
            final Date time = new Date(p.getTime());
            r.setTime(time);
            r.setTimeRadius(0.0);

            // @todo 1 tb/** we need to keep the fileId of insitu-file, orbit-file and eventually second orbit-file tb 2014-02-12
            final Observation o = storage.getObservation(p.getReference());
            r.setDatafile(o.getDatafile());
            r.setRecordNo(0);
            r.setDataset(Constants.MATCHUP_INSITU_DATASET_DUMMY_BC);
            r.setReferenceFlag(Constants.MATCHUP_REFERENCE_FLAG_UNDEFINED);

            referenceObservations.add(r);
        }
        return referenceObservations;
    }

    private OverlapRemover createOverlapRemover() {
        return new OverlapRemover(subSceneWidth, subSceneHeight);
    }

    private void cleanupIfRequested() {
        final Configuration config = getConfig();
        if (config.getBooleanValue(Configuration.KEY_MMS_SAMPLING_CLEANUP)) {
            cleanup();
        } else if (config.getBooleanValue(Configuration.KEY_MMS_SAMPLING_CLEANUPINTERVAL)) {
            cleanupInterval();
        }
    }


    void cleanup() {
        getPersistenceManager().transaction();

        Query delete = getPersistenceManager().createQuery("delete from Coincidence c");
        delete.executeUpdate();
        delete = getPersistenceManager().createQuery("delete from Matchup m");
        delete.executeUpdate();
        delete = getPersistenceManager().createQuery(
                "delete from Observation o where o.sensor = '" + Constants.SENSOR_NAME_SOBOL + "'");
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

    void cleanupInterval() {
        getPersistenceManager().transaction();

        Query delete = getPersistenceManager().createNativeQuery(
                "delete from mm_coincidence c where exists ( select r.id from mm_observation r where c.matchup_id = r.id and r.time >= ?1 and r.time < ?2 and r.sensor = '" + Constants.SENSOR_NAME_SOBOL + "')");
        delete.setParameter(1, new Date(startTime));
        delete.setParameter(2, new Date(stopTime));
        delete.executeUpdate();
        delete = getPersistenceManager().createNativeQuery(
                "delete from mm_matchup m where exists ( select r from mm_observation r where m.refobs_id = r.id and r.time >= ?1 and r.time < ?2 and r.sensor = '" + Constants.SENSOR_NAME_SOBOL + "')");
        delete.setParameter(1, new Date(startTime));
        delete.setParameter(2, new Date(stopTime));
        delete.executeUpdate();
        delete = getPersistenceManager().createNativeQuery(
                "delete from mm_observation r where r.time >= ?1 and r.time < ?2 and r.sensor = '" + Constants.SENSOR_NAME_SOBOL + "'");
        delete.setParameter(1, new Date(startTime));
        delete.setParameter(2, new Date(stopTime));
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

}
