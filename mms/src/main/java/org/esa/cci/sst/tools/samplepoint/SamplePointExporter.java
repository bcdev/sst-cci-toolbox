package org.esa.cci.sst.tools.samplepoint;


import org.apache.commons.lang.StringUtils;
import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SamplingPointIO;
import org.esa.cci.sst.util.TimeUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class SamplePointExporter {

    private static final DecimalFormat monthFormat = new DecimalFormat("00");

    private final Configuration config;
    private Logger logger;

    public SamplePointExporter(Configuration config) {
        this.config = config;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void export(List<SamplingPoint> samplingPoints, TimeRange samplingInterval) throws IOException {
        final TimeRange monthBefore = samplingInterval.getMonthBefore();
        final TimeRange centerMonth = samplingInterval.getCenterMonth();
        final TimeRange monthAfter = samplingInterval.getMonthAfter();

        final List<SamplingPoint> pointsMonthBefore = extractSamples(samplingPoints, monthBefore);
        final List<SamplingPoint> pointsCenterMonth = extractSamples(samplingPoints, centerMonth);
        final List<SamplingPoint> pointsMonthAfter = extractSamples(samplingPoints, monthAfter);

        if (!samplingPoints.isEmpty()) {
            if (logger != null) {
                logger.warning("List of sampling points still contains points out of expected time range: " + samplingPoints.size());
            }
        }

        final String archiveRootPath = getArchiveRootPath(config);
        final String sensorName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR_1);

        writeSamplingPoints(pointsMonthBefore, monthBefore, archiveRootPath, sensorName, 'a');
        writeSamplingPoints(pointsCenterMonth, centerMonth, archiveRootPath, sensorName, 'b');
        writeSamplingPoints(pointsMonthAfter, monthAfter, archiveRootPath, sensorName, 'c');
    }

    private void writeSamplingPoints(List<SamplingPoint> points, TimeRange timeRange, String archiveRootPath, String sensorName, char key) throws IOException {
        final int year = TimeUtil.getYear(timeRange.getStartDate());
        final int month = TimeUtil.getMonth(timeRange.getStartDate());

        final String targetPath = createOutputPath(archiveRootPath, sensorName, year, month, key);
        final File targetFile = new File(targetPath);
        final File targetDir = targetFile.getParentFile();
        if (!targetDir.isDirectory()) {
            if (!targetDir.mkdirs()) {
                throw new ToolException("Unable to create target directory: " + targetDir.getAbsolutePath(), -1);
            }
        }

        if (!targetFile.createNewFile()) {
            throw new ToolException("Unable to create target file: " + targetFile.getAbsolutePath(), -1);
        }

        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            SamplingPointIO.write(points, outputStream);
        }
    }


    // package access for testing only tb 2014-02-14
    static List<SamplingPoint> extractSamples(List<SamplingPoint> samples, TimeRange extractRange) {
        final LinkedList<SamplingPoint> extracted = new LinkedList<>();

        final Iterator<SamplingPoint> iterator = samples.iterator();
        while (iterator.hasNext()) {
            final SamplingPoint point = iterator.next();
            if (extractRange.includes(new Date(point.getTime()))) {
                extracted.add(point);
                iterator.remove();
            }
        }
        return extracted;
    }

    // package access for testing only tb 2014-02-17
    static String createOutputPath(String archivRoot, String sensorName, int year, int month, char key) {
        final StringBuilder builder = new StringBuilder(256);
        builder.append(archivRoot);
        builder.append("/smp/");
        builder.append(sensorName);
        builder.append('/');
        builder.append(year);
        builder.append('/');
        builder.append(sensorName);
        builder.append("-smp-");
        builder.append(year);
        builder.append('-');
        builder.append(monthFormat.format(month));
        builder.append('-');
        builder.append(key);
        builder.append(".json");
        return builder.toString();
    }

    // package access for testing only tb 2014-02-17
    static String getArchiveRootPath(Configuration config) {
        final String archiveRootPath = config.getStringValue(Configuration.KEY_ARCHIVE_ROOTDIR);
        if (StringUtils.isEmpty(archiveRootPath)) {
            throw new ToolException("Missing archive root in config.", -1);
        }

        final String useCase = config.getStringValue(Configuration.KEY_ARCHIVE_USECASE);
        if (StringUtils.isEmpty(useCase)) {
            return archiveRootPath;
        }

        return archiveRootPath + File.separatorChar + useCase;
    }
}
