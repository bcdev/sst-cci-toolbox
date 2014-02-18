package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.ConfigUtil;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SamplingPointIO;
import org.esa.cci.sst.util.TimeUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;


public class SamplePointImporter {

    private final String archiveRootPath;
    private final String sensorName;
    private final int year;
    private final int month;

    public SamplePointImporter(Configuration config) {
        archiveRootPath = ConfigUtil.getArchiveRootPath(config);
        sensorName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR);

        final Date startDate = config.getDateValue(Configuration.KEY_MMS_SAMPLING_START_TIME);
        final Date stopDate = config.getDateValue(Configuration.KEY_MMS_SAMPLING_STOP_TIME);
        final Date centerDate = TimeUtil.centerTime(startDate, stopDate);
        year = TimeUtil.getYear(centerDate);
        month = TimeUtil.getMonth(centerDate);

    }

    public List<SamplingPoint> load() throws IOException {
        final File fileA = getInputFile('a');
        final File fileB = getInputFile('b');
        final File fileC = getInputFile('c');

        final List<SamplingPoint> listA = loadPointsFrom(fileA);
        final List<SamplingPoint> listB = loadPointsFrom(fileB);
        final List<SamplingPoint> listC = loadPointsFrom(fileC);

        listA.addAll(listB);
        listA.addAll(listC);
        return listA;
    }

    private List<SamplingPoint> loadPointsFrom(File fileA) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(fileA)) {
            return SamplingPointIO.read(inputStream);
        }
    }

    private File getInputFile(char key) {
        final String pathA = SamplingPointUtil.createPath(archiveRootPath, sensorName, year, month, key);
        final File fileA = new File(pathA);
        if (!fileA.isFile()) {
            throw new ToolException("Missing input file: sensor.5-smp-2007-01-a.json", -1);
        }
        return fileA;
    }
}
