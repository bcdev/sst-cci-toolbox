package org.esa.cci.sst.tools;

import org.apache.commons.lang.StringUtils;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.tools.matchup.MatchupIO;
import org.esa.cci.sst.util.ConfigUtil;
import org.esa.cci.sst.util.FileUtil;
import org.esa.cci.sst.util.Month;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AuxDataTool extends BasicTool {

    private static final String NAME = "auxdata-tool.sh";
    private static final String VERSION = "1.0";

    private String sensorName1;
    private String sensorName2;
    private String archiveRootPath;
    private String insituSensorName;

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
    }

    private void run() throws IOException {
        final List<Matchup> matchups = loadMatchups();

        // load seaice and aerosol sensor name/pattern

        // iterate over matchups
        for(final Matchup matchup: matchups) {
            // -- search db -> coincidence for seaice and aerosol for reference observation location and time
            // -- add as coincidence(s) to matchup
        }



        // store matchups
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
}
