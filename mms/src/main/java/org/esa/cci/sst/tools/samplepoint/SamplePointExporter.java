package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.util.ConfigUtil;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SamplingPointIO;
import org.esa.cci.sst.util.TimeUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SamplePointExporter {

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
            logWarning("List of sampling points still contains points out of expected time range: " + samplingPoints.size());
        }

        final String usecaseRootPath = ConfigUtil.getUsecaseRootPath(config);
        final String sensorName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR);

        writeSamplingPoints(pointsMonthBefore, monthBefore, usecaseRootPath, sensorName, 'a');
        writeSamplingPoints(pointsCenterMonth, centerMonth, usecaseRootPath, sensorName, 'b');
        writeSamplingPoints(pointsMonthAfter, monthAfter, usecaseRootPath, sensorName, 'c');
    }

    private void writeSamplingPoints(List<SamplingPoint> points, TimeRange timeRange, String archiveRootPath,
                                     String sensorName, char key) throws IOException {
        final int year = TimeUtil.getYear(timeRange.getStartDate());
        final int month = TimeUtil.getMonth(timeRange.getStartDate());

        final String targetPath = SamplingPointUtil.createPath(archiveRootPath, sensorName, year, month, key);
        final File targetFile = new File(targetPath);
        final File targetDir = targetFile.getParentFile();
        if (!targetDir.isDirectory()) {
            if (!targetDir.mkdirs()) {
                throw new ToolException("Unable to create target directory: " + targetDir.getAbsolutePath(), -1);
            }
        }

        if (!targetFile.createNewFile()) {
            logWarning("Overwriting target file: " + targetFile.getAbsolutePath());
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
            if (extractRange.includes(new Date(point.getReferenceTime()))) {
                extracted.add(point);
                iterator.remove();
            }
        }
        return extracted;
    }

    private void logWarning(String msg) {
        if (logger != null && logger.isLoggable(Level.WARNING)) {
            logger.warning(msg);
        }
    }
}
