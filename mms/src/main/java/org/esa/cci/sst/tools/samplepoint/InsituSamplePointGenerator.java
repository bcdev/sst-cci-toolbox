package org.esa.cci.sst.tools.samplepoint;

import org.apache.commons.io.FileUtils;
import org.esa.beam.util.StringUtils;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.reader.ReaderFactory;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

public class InsituSamplePointGenerator {

    private final File archiveDir;
    private final Reader reader;
    private final Sensor sensor;

    private Logger logger;

    public InsituSamplePointGenerator(File archiveDir, Sensor sensor) {
        this.archiveDir = archiveDir;
        reader = ReaderFactory.createReader("InsituReader", "");
        this.sensor = sensor;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public List<SamplingPoint> generate(long startTime, long stopTime) throws ParseException {
        final ArrayList<SamplingPoint> samplingPoints = new ArrayList<>();
        final TimeRange timeRange = new TimeRange(new Date(startTime), new Date(stopTime));

        final LinkedList<File> filesInRange = findFilesInTimeRange(timeRange);
        for (File insituFile : filesInRange) {
            extractPointsInTimeRange(samplingPoints, timeRange, insituFile);
        }
        return samplingPoints;
    }

    private void extractPointsInTimeRange(ArrayList<SamplingPoint> samplingPoints, TimeRange timeRange, File insituFile) {
        final DataFile dataFile = new DataFile(insituFile.getName(), sensor);

        try {
            reader.init(dataFile, archiveDir);
            final List<SamplingPoint> pointsInFile = reader.readSamplingPoints();
            for (final SamplingPoint samplingPoint : pointsInFile) {
                if (timeRange.isWithin(new Date(samplingPoint.getTime()))) {
                    samplingPoints.add(samplingPoint);
                }
            }
        } catch (IOException e) {
            logError(e.getMessage());
        } finally {
            reader.close();
        }
    }

    private LinkedList<File> findFilesInTimeRange(TimeRange timeRange) {
        final LinkedList<File> filesInRange = new LinkedList<>();
        final Collection<File> insituFiles = FileUtils.listFiles(archiveDir, new String[]{"nc"}, true);
        for (final File file : insituFiles) {
            try {
                final TimeRange fileTimeRange = extractTimeRange(file.getName());
                if (timeRange.hasIntersectWith(fileTimeRange)) {
                    filesInRange.add(file);
                }
            } catch (ParseException e) {
                logError(e.getMessage());
            }
        }
        return filesInRange;
    }

    private void logError(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }

    // package access for testing only tb 2014-02-13
    static TimeRange extractTimeRange(String fileName) throws ParseException {
        final String withoutExtension = org.esa.beam.util.io.FileUtils.getFilenameWithoutExtension(fileName);
        final String[] split = StringUtils.split(withoutExtension, new char[]{'_'}, true);
        if (split.length < 2) {
            throw new ParseException("File name not matching pattern", 0);
        }
        final int endDateIndex = split.length - 1;
        final int startDateIndex = split.length - 2;

        final Date startDate = TimeUtil.parseInsituFileNameDateFormat(split[startDateIndex]);
        final Date endDate = TimeUtil.parseInsituFileNameDateFormat(split[endDateIndex]);
        final Date beginning = TimeUtil.getBeginningOfDay(startDate);
        final Date end = TimeUtil.getEndOfDay(endDate);
        return new TimeRange(beginning, end);
    }
}
