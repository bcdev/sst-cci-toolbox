package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.SamplingPoint;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Logger;

public class FindObservationsWorkflow {

    private final ObservationFinder observationFinder;
    private final WorkflowContext workflowContext;
    private final Logger logger;

    public FindObservationsWorkflow(WorkflowContext workflowContext) {
        this.workflowContext = workflowContext;
        this.logger = workflowContext.getLogger();

        observationFinder = new ObservationFinder(workflowContext.getPersistenceManager());
    }

    public void execute(List<SamplingPoint> samplingPoints) throws IOException {
        final String sensorName = workflowContext.getSensorName();
        final long startTime = workflowContext.getStartTime();
        final long stopTime = workflowContext.getStopTime();
        final int halfRevisitTime = workflowContext.getHalfRevisitTime();

        logInfo("Starting associating samples with observations...");

        observationFinder.findPrimarySensorObservations(samplingPoints, sensorName, startTime, stopTime, halfRevisitTime);

        logInfo(MessageFormat.format("Finished associating samples with observations ({0} samples left)", samplingPoints.size()));
    }

    private void logInfo(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }
}
