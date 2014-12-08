package org.esa.cci.sst.tools;

import org.apache.commons.lang.StringUtils;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.tools.matchup.MatchupIO;
import org.esa.cci.sst.tools.samplepoint.ObservationFinder;
import org.esa.cci.sst.util.ConfigUtil;
import org.esa.cci.sst.util.FileUtil;
import org.esa.cci.sst.util.Month;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AuxDataTool extends BasicTool {

    private static final String NAME = "auxdata-tool.sh";
    private static final String VERSION = "1.0";

    private String sensorName1;
    private String sensorName2;
    private String archiveRootPath;
    private String insituSensorName;
    private int aaiTimeDeltaSeconds;
    private int seaiceTimeDeltaSeconds;
    private String aaiSensorName;
    private String seaiceSensorName;

    protected AuxDataTool() {
        super(NAME, VERSION);
    }

    public static void main(String[] args) {
        final AuxDataTool auxDataTool = new AuxDataTool();

        try {
            if (!auxDataTool.setCommandLineArgs(args)) {
                return;
            }

            auxDataTool.initialize();
            auxDataTool.run();
        } catch (IOException e) {
            auxDataTool.getErrorHandler().terminate(new ToolException(e.getMessage(), e, ToolException.TOOL_IO_ERROR));
        }
    }

    @Override
    public void initialize() {
        super.initialize();

        final Configuration config = getConfig();

        final String[] sensorNames = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR).split(",", 2);
        sensorName1 = sensorNames[0];
        if (sensorNames.length > 1) {
            sensorName2 = sensorNames[1];
        }

        archiveRootPath = config.getStringValue(Configuration.KEY_MMS_ARCHIVE_ROOT);
        insituSensorName = config.getOptionalStringValue(Configuration.KEY_MMS_SAMPLING_INSITU_SENSOR);

        aaiSensorName = config.getStringValue("mms.matchup.43.sensor");
        seaiceSensorName = config.getStringValue("mms.matchup.44.sensor");

        aaiTimeDeltaSeconds = config.getIntValue("mms.timedelta.aai");
        seaiceTimeDeltaSeconds = config.getIntValue("mms.timedelta.seaice");
    }

    private void run() throws IOException {
        final List<Matchup> matchups = loadMatchups();

        final ObservationFinder observationFinder = new ObservationFinder(getPersistenceManager());
        for (final Matchup matchup : matchups) {
            final Date matchupTime = matchup.getRefObs().getTime();
            final long matchupMillis = matchupTime.getTime();

            final ObservationFinder.Parameter aaiParameter = createQueryParameter(matchupMillis, aaiTimeDeltaSeconds, aaiSensorName);
            final ObservationFinder.Parameter seaiceParameter = createQueryParameter(matchupMillis, seaiceTimeDeltaSeconds, seaiceSensorName);

            // @todo 1 tb/tb extend method to accept ONE geometry: observationFinder.findObservations();
            // query database using ObservationFinder
            // if results
            // -- create coincidence
            // -- add to matchup
        }


        // store matchups (find new filenamne pattern)
    }

    private List<Matchup> loadMatchups() throws IOException {
        final Month centerMonth = ConfigUtil.getCenterMonth(Configuration.KEY_MMS_SAMPLING_START_TIME,
                Configuration.KEY_MMS_SAMPLING_STOP_TIME,
                getConfig());
        final String[] sensorNamesArray = createSensorNamesArray();
        final String outputFilePath = ArchiveUtils.createCleanFilePath(archiveRootPath, sensorNamesArray, centerMonth.getYear(), centerMonth.getMonth());
        final File outFile = FileUtil.createNewFile(outputFilePath);

        return MatchupIO.read(new FileInputStream(outFile));
    }

    private String[] createSensorNamesArray() {
        final ArrayList<String> sensorNamesList = new ArrayList<>();
        sensorNamesList.add(sensorName1);
        if (sensorName2 != null) {
            sensorNamesList.add(sensorName2);
        }
        if (StringUtils.isNotBlank(insituSensorName)) {
            sensorNamesList.add("history");
        }

        return sensorNamesList.toArray(new String[sensorNamesList.size()]);
    }

    // package access for testing only tb 2014-12-08
    static ObservationFinder.Parameter createQueryParameter(long time, int delta, String sensorName) {
        final ObservationFinder.Parameter parameter = new ObservationFinder.Parameter();
        parameter.setStartTime(time);
        parameter.setStopTime(time);
        parameter.setSearchTimePast(delta);
        parameter.setSearchTimeFuture(delta);
        parameter.setSensorName(sensorName);
        return parameter;
    }
}
