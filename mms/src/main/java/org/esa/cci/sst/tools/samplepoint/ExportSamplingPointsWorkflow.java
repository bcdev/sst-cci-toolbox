package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.SamplingPoint;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class ExportSamplingPointsWorkflow {

    private final Logger logger;
    private final TimeRange timeRange;
    private final SamplePointExporter samplePointExporter;

    public ExportSamplingPointsWorkflow(WorkflowContext context) {
        logger = context.getLogger();

        final Date startDate = new Date(context.getStartTime());
        final Date stopDate = new Date(context.getStopTime());
        timeRange = new TimeRange(startDate, stopDate);

        samplePointExporter = new SamplePointExporter(context.getConfig());
        samplePointExporter.setLogger(logger);
    }

    public void execute(List<SamplingPoint> samplingPoints) throws IOException {
        logInfo("Starting writing samples...");

        samplePointExporter.export(samplingPoints, timeRange);

        logInfo("Finished writing samples...");

    }

    private void logInfo(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }
}
