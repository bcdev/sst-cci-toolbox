package org.esa.cci.sst.tools;

import org.esa.cci.sst.tools.overlap.RegionOverlapFilter;
import org.esa.cci.sst.tools.samplepoint.CloudySubsceneRemover;
import org.esa.cci.sst.tools.samplepoint.OverlapRemover;
import org.esa.cci.sst.util.SamplingPoint;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MatchupGenerator extends BasicTool {

    private static final String SOBOL_SENSOR_NAME = "sobol";

    private long startTime;
    private long stopTime;
    private String sensorName;
    private int subSceneWidth;
    private int subSceneHeight;
    private String cloudFlagsVariableName;
    private int cloudFlagsMask;
    private double cloudyPixelFraction;
    private boolean dualSensorMatching;

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
        sensorName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR);
        subSceneWidth = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SUBSCENE_WIDTH, 7);
        subSceneHeight = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SUBSCENE_HEIGHT, 7);
        cloudFlagsVariableName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_CLOUD_FLAGS_VARIABLE_NAME);
        cloudFlagsMask = config.getIntValue(Configuration.KEY_MMS_SAMPLING_CLOUD_FLAGS_MASK);
        cloudyPixelFraction = config.getDoubleValue(Configuration.KEY_MMS_SAMPLING_CLOUDY_PIXEL_FRACTION, 0.0);
        dualSensorMatching = config.getBooleanValue(Configuration.KEY_MMS_SAMPLING_DUAL_SENSOR_MATCHING);
    }

    private void run() {
        cleanupIfRequested();

        // todo - read input
        final List<SamplingPoint> samples = new ArrayList<>();

        final CloudySubsceneRemover subsceneRemover = new CloudySubsceneRemover();
        subsceneRemover.sensorName(sensorName)
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

        if (dualSensorMatching) {
            // todo - find observations for secondary sensor
            subsceneRemover.primary(false).removeSamples(samples);
        }

        final OverlapRemover overlapRemover = createOverlapRemover();
        overlapRemover.removeSamples(samples);
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
                "delete from Observation o where o.sensor = '" + SOBOL_SENSOR_NAME + "'");
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

    void cleanupInterval() {
        getPersistenceManager().transaction();

        Query delete = getPersistenceManager().createNativeQuery(
                "delete from mm_coincidence c where exists ( select r.id from mm_observation r where c.matchup_id = r.id and r.time >= ?1 and r.time < ?2 and r.sensor = '" + SOBOL_SENSOR_NAME + "')");
        delete.setParameter(1, new Date(startTime));
        delete.setParameter(2, new Date(stopTime));
        delete.executeUpdate();
        delete = getPersistenceManager().createNativeQuery(
                "delete from mm_matchup m where exists ( select r from mm_observation r where m.refobs_id = r.id and r.time >= ?1 and r.time < ?2 and r.sensor = '" + SOBOL_SENSOR_NAME + "')");
        delete.setParameter(1, new Date(startTime));
        delete.setParameter(2, new Date(stopTime));
        delete.executeUpdate();
        delete = getPersistenceManager().createNativeQuery(
                "delete from mm_observation r where r.time >= ?1 and r.time < ?2 and r.sensor = '" + SOBOL_SENSOR_NAME + "'");
        delete.setParameter(1, new Date(startTime));
        delete.setParameter(2, new Date(stopTime));
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

}
