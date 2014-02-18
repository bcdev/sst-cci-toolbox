package org.esa.cci.sst.tools;

import org.esa.cci.sst.tools.samplepoint.*;
import org.esa.cci.sst.util.SamplingPoint;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class SamplePointGenerator extends BasicTool {

    private long startTime;
    private long stopTime;
    private int searchTimeDelta;
    private int sampleCount;
    private int sampleSkip;
    private String sensorName;

    public SamplePointGenerator() {
        super("sampling-point-generator", "1.0");
    }

    public static void main(String[] args) {
        final SamplePointGenerator tool = new SamplePointGenerator();
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
        }
    }

    @Override
    public void initialize() {
        super.initialize();

        final Configuration config = getConfig();
        startTime = config.getDateValue(Configuration.KEY_MMS_SAMPLING_START_TIME).getTime();
        stopTime = config.getDateValue(Configuration.KEY_MMS_SAMPLING_STOP_TIME).getTime();
        searchTimeDelta = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SEARCH_TIME_DELTA, 0);
        sampleCount = config.getIntValue(Configuration.KEY_MMS_SAMPLING_COUNT, 0);
        sampleSkip = config.getIntValue(Configuration.KEY_MMS_SAMPLING_SKIP, 0);
        sensorName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR);
    }

    private void run() throws IOException {
        final SobolSamplePointGenerator generator = createSamplePointGenerator();
        final List<SamplingPoint> samples = generator.createSamples(sampleCount, sampleSkip, startTime, stopTime);

        final LandPointRemover landPointRemover = createLandPointRemover();
        landPointRemover.removeSamples(samples);

        final ClearSkyPointRemover clearSkyPointRemover = createClearSkyPointRemover();
        clearSkyPointRemover.removeSamples(samples);

        final ObservationFinder observationFinder = new ObservationFinder(getPersistenceManager());
        observationFinder.findPrimarySensorObservations(samples, sensorName, startTime, stopTime, searchTimeDelta);

        final TimeRange timeRange = new TimeRange(new Date(startTime), new Date(stopTime));
        final SamplePointExporter samplePointExporter = new SamplePointExporter(getConfig());
        samplePointExporter.setLogger(getLogger());
        samplePointExporter.export(samples, timeRange);
    }

    private SobolSamplePointGenerator createSamplePointGenerator() {
        return new SobolSamplePointGenerator();
    }

    private LandPointRemover createLandPointRemover() {
        return new LandPointRemover();
    }

    private ClearSkyPointRemover createClearSkyPointRemover() {
        return new ClearSkyPointRemover();
    }
}