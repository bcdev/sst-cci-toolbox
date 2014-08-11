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
        logInfo("Starting associating samples with observations...");
        ObservationFinder.Parameter parameter = createFromContextForPrimary(workflowContext);
        persistenceManager.transaction();
        observationFinder.findPrimarySensorObservations(samplingPoints, parameter);
        persistenceManager.commit();
        logInfo(MessageFormat.format("Finished associating samples with observations ({0} samples left)", samplingPoints.size()));

        final String sensorName2 = workflowContext.getSensorName2();
        if (StringUtils.isNotBlank(sensorName2)) {
            logInfo("Starting associating samples with secondary observations...");
            parameter = createFromContextForSecondary(workflowContext);
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
        final ObservationFinder.Parameter parameter = createWithTimeParameters(workflowContext);
        parameter.setSearchTimePast(workflowContext.getSearchTimePast());
        parameter.setSearchTimeFuture(workflowContext.getSearchTimeFuture());
        parameter.setSensorName(workflowContext.getSensorName1());
        return parameter;
    }

    // package access for testing only tb 2014-04-02
    static ObservationFinder.Parameter createFromContextForSecondary(WorkflowContext workflowContext) {
        final ObservationFinder.Parameter parameter = createWithTimeParameters(workflowContext);
        parameter.setSearchTimePast(workflowContext.getSearchTimePast2());
        parameter.setSearchTimeFuture(workflowContext.getSearchTimeFuture2  ());
        parameter.setSensorName(workflowContext.getSensorName2());
        return parameter;
    }

    private static ObservationFinder.Parameter createWithTimeParameters(WorkflowContext workflowContext) {
        final ObservationFinder.Parameter parameter = new ObservationFinder.Parameter();
        parameter.setStartTime(workflowContext.getStartTime());
        parameter.setStopTime(workflowContext.getStopTime());

        return parameter;
    }
}
