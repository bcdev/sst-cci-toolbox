package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.util.ConfigUtil;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SamplingPointIO;
import org.esa.cci.sst.util.TimeUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;


public class SamplePointImporter {

    private final String usecaseRootPath;
    private final String sensorName;
    private final int year;
    private final int month;

    private Logger logger;

    public SamplePointImporter(Configuration config) {
        usecaseRootPath = ConfigUtil.getUsecaseRootPath(config);
        sensorName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR);

        final TimeRange timeRange = ConfigUtil.getTimeRange(Configuration.KEY_MMS_SAMPLING_START_TIME,
                Configuration.KEY_MMS_SAMPLING_STOP_TIME,
                config);
        final Date centerDate = TimeUtil.getCenterTime(timeRange.getStartDate(), timeRange.getStopDate());
        year = TimeUtil.getYear(centerDate);
        month = TimeUtil.getMonth(centerDate);
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
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

    private List<SamplingPoint> loadPointsFrom(File file) throws IOException {
        if (file == null) {
            return new ArrayList<>();
        }

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file), 16384)) {
            return SamplingPointIO.read(inputStream);
        }
    }

    private File getInputFile(char key) {
        final String path = SamplingPointUtil.createPath(usecaseRootPath, sensorName, year, month, key);
        final File file = new File(path);
        if (!file.isFile()) {
            logWarning("Missing input file: " + file.getAbsolutePath());
            return null;
        }
        return file;
    }

    private void logWarning(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }
}
