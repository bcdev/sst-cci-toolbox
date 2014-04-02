package org.esa.cci.sst.tools.samplepoint;

import org.apache.commons.lang.StringUtils;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.util.SamplingPoint;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

public class FindObservationsWorkflow extends Workflow {

    private final ObservationFinder observationFinder;
    private final PersistenceManager persistenceManager;

    public FindObservationsWorkflow(WorkflowContext workflowContext) {
        super(workflowContext);

        persistenceManager = workflowContext.getPersistenceManager();
        observationFinder = new ObservationFinder(persistenceManager);
    }

    @Override
    public void execute(List<SamplingPoint> samplingPoints) throws IOException {
        final String sensorName = workflowContext.getSensorName();
        final long startTime = workflowContext.getStartTime();
        final long stopTime = workflowContext.getStopTime();
        final int searchTimePast = workflowContext.getSearchTimePast();
        final int searchTimeFuture = workflowContext.getSearchTimeFuture();

        logInfo("Starting associating samples with observations...");
        // @todo 2 tb/tb encapsulate
        final ObservationFinder.Parameter parameter = new ObservationFinder.Parameter();
        parameter.setSensorName(sensorName);
        parameter.setStartTime(startTime);
        parameter.setStopTime(stopTime);
        parameter.setSearchTimePast(searchTimePast);
        parameter.setSearchTimeFuture(searchTimeFuture);
        persistenceManager.transaction();
        observationFinder.findPrimarySensorObservations(samplingPoints, parameter);
        persistenceManager.commit();
        logInfo(MessageFormat.format("Finished associating samples with observations ({0} samples left)", samplingPoints.size()));

        final String sensorName2 = workflowContext.getSensorName2();
        if (StringUtils.isNotBlank(sensorName2)) {
            logInfo("Starting associating samples with secondary observations...");
            parameter.setSensorName(sensorName2);
            persistenceManager.transaction();
            observationFinder.findSecondarySensorObservations(samplingPoints, parameter);
            persistenceManager.commit();
            logInfo(MessageFormat.format("Finished associating samples with secondary observations ({0} samples left)", samplingPoints.size()));
        }
    }

    @Override
    public List<SamplingPoint> execute() throws IOException {
        return null;    // nothing to do here
    }

    // package access for testing only tb 2014-04-02
     static ObservationFinder.Parameter createFromContextForPrimary(WorkflowContext workflowContext) {
        return new ObservationFinder.Parameter();
    }
}
