package org.esa.cci.sst.tools;

import org.esa.cci.sst.tools.samplepoint.*;
import org.esa.cci.sst.util.SamplingPoint;

import java.io.IOException;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.List;

public class SamplingPointGenerationTool extends BasicTool {

    private long startTime;
    private long stopTime;
    private int halfRevisitTime;
    private int sampleCount;
    private int sampleSkip;
    private String sensorName;

    public SamplingPointGenerationTool() {
        super("sampling-point-generator", "1.0");
    }

    public static void main(String[] args) {
        final SamplingPointGenerationTool tool = new SamplingPointGenerationTool();
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
        halfRevisitTime = config.getIntValue(Configuration.KEY_MMS_SAMPLING_HALF_REVISIT_TIME);
        sampleCount = config.getIntValue(Configuration.KEY_MMS_SAMPLING_COUNT);
        sampleSkip = config.getBigIntegerValue(Configuration.KEY_MMS_SAMPLING_SKIP, BigInteger.valueOf(0)).intValue();
        sensorName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR);
    }

    private void run() throws IOException {
        final SobolSamplePointGenerator generator = createSamplePointGenerator();
        getLogger().info(MessageFormat.format("Starting creating {0} samples...", sampleCount));
        final List<SamplingPoint> samples = generator.createSamples(sampleCount, sampleSkip, startTime, stopTime);
        getLogger().info(MessageFormat.format("Finished creating {0} samples", samples.size()));

        final LandPointRemover landPointRemover = createLandPointRemover();
        getLogger().info("Starting removing land samples...");
        landPointRemover.removeSamples(samples);
        getLogger().info(MessageFormat.format("Finished removing land samples ({0} samples left)", samples.size()));

        final ClearSkyPointRemover clearSkyPointRemover = createClearSkyPointRemover();
        getLogger().info("Starting removing prior clear-sky samples...");
        clearSkyPointRemover.removeSamples(samples);
        getLogger().info(
                MessageFormat.format("Finished removing prior clear-sky samples ({0} samples left)", samples.size()));

        final ObservationFinder observationFinder = new ObservationFinder(getPersistenceManager());
        getLogger().info("Starting associating samples with observations...");
        observationFinder.findPrimarySensorObservations(samples, sensorName, startTime, stopTime, halfRevisitTime);
        getLogger().info(
                MessageFormat.format("Finished associating samples with observations ({0} samples left)",
                        samples.size()));

        final Configuration config = getConfig();

        final WorkflowContext workflowContext = new WorkflowContext();
        workflowContext.setLogger(getLogger());
        workflowContext.setConfig(config);
        workflowContext.setStartTime(startTime);
        workflowContext.setStopTime(stopTime);

        final ExportSamplingPointsWorkflow exportSamplingPointsWorkflow = new ExportSamplingPointsWorkflow(workflowContext);
        exportSamplingPointsWorkflow.execute(samples);
    }

    private SobolSamplePointGenerator createSamplePointGenerator() {
        return new SobolSamplePointGenerator();
    }

    private LandPointRemover createLandPointRemover() {
        return new LandPointRemover();
    }

    private ClearSkyPointRemover createClearSkyPointRemover() {
        return new ClearSkyPointRemover();
    }
}
