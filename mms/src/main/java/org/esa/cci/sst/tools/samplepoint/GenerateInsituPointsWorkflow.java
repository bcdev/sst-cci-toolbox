package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.data.SensorBuilder;
import org.esa.cci.sst.orm.ColumnStorage;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.orm.Storage;
import org.esa.cci.sst.util.SamplingPoint;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public class GenerateInsituPointsWorkflow extends Workflow{

    private final InsituSamplePointGenerator generator;

    public GenerateInsituPointsWorkflow(WorkflowContext context) {
        super(context);

        final File archiveRootDir = context.getArchiveRootDir();
        final String insituSensorName = context.getInsituSensorName();
        final long insituSensorPattern = context.getInsituSensorPattern();
        final Sensor sensor = createSensor(insituSensorName, insituSensorPattern);
        final PersistenceManager persistenceManager = context.getPersistenceManager();
        final Storage toolStorage = persistenceManager.getStorage();
        final ColumnStorage columnStorage = persistenceManager.getColumnStorage();

        generator = new InsituSamplePointGenerator(archiveRootDir, sensor, toolStorage, columnStorage);
        generator.setLogger(workflowContext.getLogger());
    }

    @Override
    public void execute(List<SamplingPoint> samplingPoints) throws IOException{
        // nothing to do here
    }

    @Override
    public List<SamplingPoint> execute() throws IOException, ParseException {
        final long startTime = workflowContext.getStartTime();
        final long stopTime = workflowContext.getStopTime();

        return generator.generate(startTime, stopTime);
    }

    static Sensor createSensor(String name, long pattern) {
        return new SensorBuilder().name(name).pattern(pattern).observationType(InsituObservation.class).build();
    }
}
