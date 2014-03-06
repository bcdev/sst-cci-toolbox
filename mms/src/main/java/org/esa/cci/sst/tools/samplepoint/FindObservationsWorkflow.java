package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.SamplingPoint;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

public class FindObservationsWorkflow extends Workflow {

    private final ObservationFinder observationFinder;

    public FindObservationsWorkflow(WorkflowContext workflowContext) {
        super(workflowContext);

        observationFinder = new ObservationFinder(workflowContext.getPersistenceManager());
    }

    @Override
    public void execute(List<SamplingPoint> samplingPoints) throws IOException {
        final String sensorName = workflowContext.getSensorName();
        final long startTime = workflowContext.getStartTime();
        final long stopTime = workflowContext.getStopTime();
        final int halfRevisitTime = workflowContext.getHalfRevisitTime();

        logInfo("Starting associating samples with observations...");

        observationFinder.findPrimarySensorObservations(samplingPoints, sensorName, startTime, stopTime, halfRevisitTime);

        logInfo(MessageFormat.format("Finished associating samples with observations ({0} samples left)", samplingPoints.size()));
    }

    @Override
    public List<SamplingPoint> execute() throws IOException {
        return null;    // nothing to do here
    }
}
