package org.esa.cci.sst.tools;

import org.apache.commons.lang.StringUtils;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.tools.samplepoint.*;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
        workflowContext.setLogger(logger);
        workflowContext.setConfig(config);
        workflowContext.setPersistenceManager(getPersistenceManager());
        assignFromConfig(workflowContext, config);
    }

    private void run() throws IOException, ParseException {
        logger.info("Start generating sample points ...");
        final Workflow generatePointsWorkflow = createPointGeneratorWorkflow(workflowContext);
        final List<SamplingPoint> samples = generatePointsWorkflow.execute();
        logger.info("Generated sample points: " + samples.size());

        logger.info("Start intersecting matching orbits ...");
        final Workflow findObservationsWorkflow = new FindObservationsWorkflow(workflowContext);
        findObservationsWorkflow.execute(samples);
        logger.info("Intersected with matching orbits: " + samples.size());

        logger.info("Start exporting sample points ...");
        final Workflow exportSamplingPointsWorkflow = new ExportSamplingPointsWorkflow(workflowContext);
        exportSamplingPointsWorkflow.execute(samples);
        logger.info("Exporting sample points");
    }

    // package access for testing only tb 2014-03-07
    static Workflow createPointGeneratorWorkflow(WorkflowContext workflowContext) {
        final String sampleGeneratorName = workflowContext.getSampleGeneratorName();
        if ("sobol".equalsIgnoreCase(sampleGeneratorName)) {
            return new GenerateSobolPointsWorkflow(workflowContext);
        } else if ("insitu".equalsIgnoreCase(sampleGeneratorName)) {
            return new GenerateInsituPointsWorkflow(workflowContext);
        }

        throw new IllegalArgumentException("Invalid generator name: " + sampleGeneratorName);
    }

    // package access for testing only tb 2014-03-06
    static void assignFromConfig(WorkflowContext workflowContext, Configuration config) {
        long startTime = config.getDateValue(Configuration.KEY_MMS_SAMPLING_START_TIME).getTime();
        workflowContext.setStartTime(startTime);

        final Date endOfMonth = ensureEndOfMonth(config.getDateValue(Configuration.KEY_MMS_SAMPLING_STOP_TIME));
        workflowContext.setStopTime(endOfMonth.getTime());

        int searchTime = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SEARCH_TIME_PAST);
        workflowContext.setSearchTimePast(searchTime);

        searchTime = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SEARCH_TIME_FUTURE);
        workflowContext.setSearchTimeFuture(searchTime);

        String sensorName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR);
        workflowContext.setSensorNames(sensorName);

        int sampleSkip = config.getBigIntegerValue(Configuration.KEY_MMS_SAMPLING_SKIP, BigInteger.valueOf(0)).intValue();
        workflowContext.setSampleSkip(sampleSkip);

        int sampleCount = config.getIntValue(Configuration.KEY_MMS_SAMPLING_COUNT);
        workflowContext.setSampleCount(sampleCount);

        final boolean landWanted = config.getBooleanValue(Configuration.KEY_MMS_SAMPLING_LAND_WANTED, false);
        workflowContext.setLandWanted(landWanted);

        final boolean cloudsWanted = config.getBooleanValue(Configuration.KEY_MMS_SAMPLING_CLOUDS_WANTED, false);
        workflowContext.setCloudsWanted(cloudsWanted);

        final String archiveRootPath = config.getStringValue(Configuration.KEY_MMS_ARCHIVE_ROOT);
        workflowContext.setArchiveRootDir(new File(archiveRootPath));

        final String insituSourcePath = config.getStringValue(Configuration.KEY_MMS_SAMPLING_INSITU_SOURCE_DIR, null);
        workflowContext.setInsituInputPath(insituSourcePath);

        final String insituSensorName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_INSITU_SENSOR, null);
        workflowContext.setInsituSensorName(insituSensorName);

        final long insituPattern = config.getPattern(insituSensorName, 0);
        workflowContext.setInsituSensorPattern(insituPattern);

        final String sampleGeneratorName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_GENERATOR);
        workflowContext.setSampleGeneratorName(sampleGeneratorName);

        if (StringUtils.isNotBlank(workflowContext.getSensorName2())) {
            searchTime = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SEARCH_TIME_PAST_2);
            workflowContext.setSearchTimePast2(searchTime);

            searchTime = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SEARCH_TIME_FUTURE_2);
            workflowContext.setSearchTimeFuture2(searchTime);
        }
    }

    private static Date ensureEndOfMonth(Date stopDate) {
        final GregorianCalendar utcCalendar = TimeUtil.createUtcCalendar();
        utcCalendar.setTime(stopDate);
        utcCalendar.add(Calendar.DAY_OF_MONTH, -1);
        return TimeUtil.getEndOfMonth(utcCalendar.getTime());
    }
}
