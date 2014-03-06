package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.SamplingPoint;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class ExportSamplingPointsWorkflow extends Workflow {

    private final TimeRange timeRange;
    private final SamplePointExporter samplePointExporter;

    public ExportSamplingPointsWorkflow(WorkflowContext context) {
        super(context);

        final Date startDate = new Date(context.getStartTime());
        final Date stopDate = new Date(context.getStopTime());
        timeRange = new TimeRange(startDate, stopDate);

        samplePointExporter = new SamplePointExporter(context.getConfig());
        samplePointExporter.setLogger(logger);
    }

    @Override
    public void execute(List<SamplingPoint> samplingPoints) throws IOException {
        logInfo("Starting writing samples...");

        samplePointExporter.export(samplingPoints, timeRange);

        logInfo("Finished writing samples...");
    }

    @Override
    public List<SamplingPoint> execute() throws IOException {
        return null;    // nothing to do here
    }
}
