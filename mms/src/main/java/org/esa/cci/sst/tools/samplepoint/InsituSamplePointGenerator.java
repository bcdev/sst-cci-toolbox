package org.esa.cci.sst.tools.samplepoint;

import org.apache.commons.io.FileUtils;
import org.esa.beam.util.StringUtils;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.data.SensorBuilder;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.reader.ReaderFactory;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class InsituSamplePointGenerator {

    private final File archiveDir;
    private final Reader reader;
    private final Sensor sensor;

    public InsituSamplePointGenerator(File archiveDir) {
        this.archiveDir = archiveDir;
        reader = ReaderFactory.createReader("InsituReader", "");
        // @todo 1 tb/tb get from config tb 2014-02-12
        sensor = new SensorBuilder().name("history").pattern(4000000000000000L).build();
    }

    public List<SamplingPoint> generate(long startTime, long stopTime) throws ParseException {
        final ArrayList<SamplingPoint> samplingPoints = new ArrayList<>();
        final TimeRange timeRange = new TimeRange(new Date(startTime), new Date(stopTime));

        final LinkedList<File> filesInRange = new LinkedList<>();
        final Collection<File> insituFiles = FileUtils.listFiles(archiveDir, new String[]{"nc"}, true);
        for (final File file : insituFiles) {
            try {
                final TimeRange fileTimeRange = extractTimeRange(file.getName());
                // @todo 3 tb/tb move functionality to timeRange class tb 2014-02-13
                if (timeRange.isWithin(fileTimeRange.getStartDate()) || timeRange.isWithin(fileTimeRange.getStopDate())) {
                    filesInRange.add(file);
                }
            } catch (ParseException e) {
                // @todo 2 tb/tb add logging of errors tb 2014-02-13
                System.out.println("e.getMessage() = " + e.getMessage());
            }
        }

        for (File insituFile : filesInRange) {
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
                // @todo 2 tb/tb add logging of errors tb 2014-02-12
                System.out.println("e.getMessage() = " + e.getMessage());
            } finally {
                reader.close();
            }
        }
        return samplingPoints;
    }

    // package access for testing only tb 2014-02-13
    static TimeRange extractTimeRange(String fileName) throws ParseException {
        final String withoutExtension = org.esa.beam.util.io.FileUtils.getFilenameWithoutExtension(fileName);
        final String[] split = StringUtils.split(withoutExtension, new char[]{'_'}, true);
        final int endDateIndex = split.length - 1;
        final int startDateIndex = split.length - 2;

        final Date startDate = TimeUtil.parseInsituFileNameDateFormat(split[startDateIndex]);
        final Date endDate = TimeUtil.parseInsituFileNameDateFormat(split[endDateIndex]);
        final Date beginning = TimeUtil.getBeginningOfDay(startDate);
        final Date end = TimeUtil.getEndOfDay(endDate);
        return new TimeRange(beginning, end);
    }
}
