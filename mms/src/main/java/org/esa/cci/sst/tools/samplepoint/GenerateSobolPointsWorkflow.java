package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SamplingPointPlotter;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

public class GenerateSobolPointsWorkflow extends Workflow {

    private final SobolSamplePointGenerator generator;
    private final LandPointRemover landPointRemover;
    private final ClearSkyPointRemover clearSkyPointRemover;

    public GenerateSobolPointsWorkflow(WorkflowContext context) {
        super(context);

        generator = new SobolSamplePointGenerator();
        landPointRemover = new LandPointRemover();
        clearSkyPointRemover = new ClearSkyPointRemover();
    }

    @Override
    public void execute(List<SamplingPoint> samplingPoints) throws IOException {
        // nothing to do here
    }

    @Override
    public List<SamplingPoint> execute() throws IOException {
        final int sampleCount = workflowContext.getSampleCount();
        final int sampleSkip = workflowContext.getSampleSkip();
        final long startTime = workflowContext.getStartTime();
        final long stopTime = workflowContext.getStopTime();

        logInfo(MessageFormat.format("Starting creating {0} samples...", sampleCount));
        final List<SamplingPoint> samples = generator.createSamples(sampleCount, sampleSkip, startTime, stopTime);
        logInfo(MessageFormat.format("Finished creating {0} samples", samples.size()));

        logInfo("Starting removing land samples...");
        landPointRemover.removeSamples(samples);
        logInfo(MessageFormat.format("Finished removing land samples ({0} samples left)", samples.size()));

        logInfo("Starting removing prior clear-sky samples...");
        clearSkyPointRemover.removeSamples(samples);
        logInfo(MessageFormat.format("Finished removing prior clear-sky samples ({0} samples left)", samples.size()));

        return samples;
    }

    // for creating a movie
    public static void main(String[] args) throws IOException {
        final WorkflowContext context = new WorkflowContext();
        context.setSampleCount(100000);
        context.setSampleSkip(0);
        context.setStartTime(0L);
        context.setStopTime(2592000000L); // 30 days

        final GenerateSobolPointsWorkflow workflow = new GenerateSobolPointsWorkflow(context);
        final List<SamplingPoint> samples = workflow.execute();

        new SamplingPointPlotter()
                .samples(samples)
                .mapStrategyName("lonlat")
                .series(true)
                .plot();
    }
}
