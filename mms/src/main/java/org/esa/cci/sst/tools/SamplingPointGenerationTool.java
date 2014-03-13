package org.esa.cci.sst.tools;

import org.esa.cci.sst.tools.samplepoint.*;
import org.esa.cci.sst.util.SamplingPoint;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.List;

public class SamplingPointGenerationTool extends BasicTool {

    private WorkflowContext workflowContext;

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
        } finally {
            tool.getPersistenceManager().close();
        }
    }

    @Override
    public void initialize() {
        super.initialize();

        final Configuration config = getConfig();
        workflowContext = new WorkflowContext();
        workflowContext.setLogger(getLogger());
        workflowContext.setConfig(config);
        workflowContext.setPersistenceManager(getPersistenceManager());
        assignFromConfig(workflowContext, config);
    }

    private void run() throws IOException, ParseException {
        final Workflow generatePointsWorkflow = createPointGeneratorWorkflow(workflowContext);
        final List<SamplingPoint> samples = generatePointsWorkflow.execute();

        final Workflow findObservationsWorkflow = new FindObservationsWorkflow(workflowContext);
        findObservationsWorkflow.execute(samples);

        final Workflow exportSamplingPointsWorkflow = new ExportSamplingPointsWorkflow(workflowContext);
        exportSamplingPointsWorkflow.execute(samples);
    }

    // package access for testing only tb 2014-03-07
    static Workflow createPointGeneratorWorkflow(WorkflowContext workflowContext) {
        final String sampleGeneratorName = workflowContext.getSampleGeneratorName();
        if ("SOBOL".equalsIgnoreCase(sampleGeneratorName)) {
            return new GenerateSobolPointsWorkflow(workflowContext);
        } else if ("INSITU".equalsIgnoreCase(sampleGeneratorName)) {
            return new GenerateInsituPointsWorkflow(workflowContext);
        }

        throw new IllegalArgumentException("Invalid generatorname: " + sampleGeneratorName);
    }

    // package access for testing only tb 2014-03-06
    static void assignFromConfig(WorkflowContext workflowContext, Configuration config) {
        long startTime = config.getDateValue(Configuration.KEY_MMS_SAMPLING_START_TIME).getTime();
        workflowContext.setStartTime(startTime);

        long stopTime = config.getDateValue(Configuration.KEY_MMS_SAMPLING_STOP_TIME).getTime();
        workflowContext.setStopTime(stopTime);

        int halfRevisitTime = config.getIntValue(Configuration.KEY_MMS_SAMPLING_HALF_REVISIT_TIME);
        workflowContext.setHalfRevisitTime(halfRevisitTime);

        String sensorName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR);
        workflowContext.setSensorName(sensorName);

        int sampleSkip = config.getBigIntegerValue(Configuration.KEY_MMS_SAMPLING_SKIP, BigInteger.valueOf(0)).intValue();
        workflowContext.setSampleSkip(sampleSkip);

        int sampleCount = config.getIntValue(Configuration.KEY_MMS_SAMPLING_COUNT);
        workflowContext.setSampleCount(sampleCount);

        final String archiveRootPath = config.getStringValue(Configuration.KEY_ARCHIVE_ROOTDIR);
        workflowContext.setArchiveRootDir(new File(archiveRootPath));

        // TODO - insitu is not needed for Sobol sampling
        final String insituSensorName = config.getStringValue("mms.source.45.sensor", null);
        workflowContext.setInsituSensorName(insituSensorName);

        // TODO - insitu is not needed for Sobol sampling
        final long insituPattern = config.getPattern("history");
        workflowContext.setInsituSensorPattern(insituPattern);

        final String sampleGeneratorName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_GENERATOR);
        workflowContext.setSampleGeneratorName(sampleGeneratorName);
    }
}
