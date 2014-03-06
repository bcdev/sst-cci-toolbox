package org.esa.cci.sst.tools;

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.orm.Storage;
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
        // TODO - reference sensor pattern is different for in-situ points, make reference sensor name configurable rq-20140306
        referenceSensorPattern = config.getPattern(Constants.SENSOR_NAME_SOBOL, 0);
    }

    private void run() throws IOException {
        cleanupIfRequested();

        final Logger logger = getLogger();

        logInfo(logger, "Starting loading samples...");
        final SamplePointImporter samplePointImporter = new SamplePointImporter(getConfig());
        samplePointImporter.setLogger(logger);
        final List<SamplingPoint> samples = samplePointImporter.load();
        logInfo(logger, "Finished loading samples: (" + samples.size() + " loaded).");

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
                .logger(logger)
                .removeSamples(samples);

        if (sensorName2 != null) {
            // todo - find observations for secondary sensor
            // todo - remove observations for secondary sensor
            // subsceneRemover.sensorName(sensorName2).primary(false).removeSamples(samples);
        }

        logInfo(logger, "Starting removing overlapping samples...");
        final OverlapRemover overlapRemover = createOverlapRemover();
        overlapRemover.removeSamples(samples);
        logInfo(logger, "Finished removing overlapping samples (" + samples.size() + " samples left)");

        logInfo(logger, "Starting creating matchups...");
        createMatchups(samples, Constants.SENSOR_NAME_SOBOL, sensorName1, sensorName2, referenceSensorPattern,
                       getPersistenceManager(), getStorage(), logger);
        logInfo(logger, "Finished creating matchups...");
    }

    static void createMatchups(List<SamplingPoint> samples, String referenceSensorName, String primarySensorName,
                               String secondarySensorName, long referenceSensorPattern, PersistenceManager pm,
                               Storage storage, Logger logger) {
        final Stack<EntityTransaction> transactions = new Stack<>();
        try {
            // create reference observations
            logInfo(logger, "Starting creating reference observations...");
            transactions.push(pm.transaction());
            final List<ReferenceObservation> referenceObservations =
                    createReferenceObservations(samples,
                                                referenceSensorName + "." + primarySensorName,
                                                storage);
            pm.commit();
            logInfo(logger, "Finished creating reference observations");

            // persist reference observations, because we need the ID
            logInfo(logger, "Starting persisting reference observations...");
            transactions.push(pm.transaction());
            for (final ReferenceObservation r : referenceObservations) {
                pm.persist(r);
            }
            pm.commit();
            logInfo(logger, "Finished persisting reference observations");

            // define matchup pattern
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
            logInfo(logger, MessageFormat.format("Matchup pattern: {0}", Long.toHexString(matchupPattern)));

            // create matchups and coincidences
            logInfo(logger, "Starting creating matchups and coincidences...");

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

                final Coincidence coincidence = new Coincidence();
                coincidence.setMatchup(matchup);
                coincidence.setObservation(o1);
                // @todo 2 tb/** check for insitu - we may want to keep the *real* time delta tb 2014-02-12
                coincidence.setTimeDifference(0.0); // Math.abs(r.getTime().getTime() -
                coincidences.add(coincidence);

                if (secondarySensorName != null) {
                    final RelatedObservation o2 = storage.getRelatedObservation(p.getReference2());
                    final Coincidence secondCoincidence = new Coincidence();
                    secondCoincidence.setMatchup(matchup);
                    secondCoincidence.setObservation(o2);
                    final Date matchupTime = matchup.getRefObs().getTime();
                    final Date relatedTime = o2.getTime();
                    secondCoincidence.setTimeDifference(TimeUtil.getTimeDifferenceInSeconds(matchupTime, relatedTime));

                    coincidences.add(secondCoincidence);
                }
            }
            pm.commit();

            logInfo(logger, "Finished creating matchups and coincidences");

            // persist matchups and coincidences
            logInfo(logger, "Starting persisting matchups and coincidences...");

            transactions.push(pm.transaction());
            for (Matchup m : matchups) {
                pm.persist(m);
            }
            for (Coincidence c : coincidences) {
                pm.persist(c);
            }
            pm.commit();

            logInfo(logger, "Finished persisting matchups and coincidences...");
        } catch (Exception e) {
            while (!transactions.isEmpty()) {
                transactions.pop().rollback();
            }
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
        }
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
            // @todo 1 tb/** we may want to differentiate the insitu-souces tb 2014-03-06
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
        } else if (config.getBooleanValue(Configuration.KEY_MMS_SAMPLING_CLEANUP_INTERVAL)) {
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
