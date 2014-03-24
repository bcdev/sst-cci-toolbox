package org.esa.cci.sst.tools.samplepoint;

import org.apache.commons.io.FileUtils;
import org.esa.beam.util.StringUtils;
import org.esa.cci.sst.common.InsituDatasetId;
import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.orm.ColumnStorage;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.orm.Storage;
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
    private final String insituRelativePath;
    private final Reader reader;
    private final Sensor sensor;
    private final Storage storage;
    private final ColumnStorage columnStorage;

    private Logger logger;

    public InsituSamplePointGenerator(File archiveDir, Sensor sensor, PersistenceManager persistenceManager,
                                      String insituRelativePath) {
        this.archiveDir = archiveDir;
        this.insituRelativePath = insituRelativePath;
        this.reader = ReaderFactory.createReader("InsituReader", "history");
        this.sensor = sensor;
        this.storage = persistenceManager.getStorage();
        this.columnStorage = persistenceManager.getColumnStorage();
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public List<SamplingPoint> generate(long startTime, long stopTime) throws ParseException {
        final ArrayList<SamplingPoint> samplingPoints = new ArrayList<>();
        final TimeRange timeRange = new TimeRange(new Date(startTime), new Date(stopTime));

        ensureSensorStoredInDb();

        final LinkedList<File> filesInRange = findFilesInTimeRange(timeRange);
        for (File insituFile : filesInRange) {
            try {
                initializeReader(insituFile);
                extractPointsInTimeRange(samplingPoints, timeRange);

                if (samplingPoints.size() > 0) {
                    final int id = persist(insituFile);
                    final byte datasetId = extractInsituDatasetId(insituFile.getName());
                    final String datasetName = reader.getDatasetName();

                    setFileProperties(samplingPoints, id, datasetId, datasetName);

                    final Item[] readerColumns = reader.getColumns();
                    persistColumnNames(readerColumns);

                } else {
                    logWarning("File does not contain any data in time range: " + insituFile.getAbsolutePath());
                }

            } catch (IOException e) {
                logWarning(e.getMessage());
            } finally {
                reader.close();
            }
        }
        return samplingPoints;
    }

    private void ensureSensorStoredInDb() {
        final Sensor sensorFromDb = storage.getSensorWithTransaction(sensor.getName());
        if (sensorFromDb == null) {
            storage.storeWithTransaction(sensor);
        }
    }

    private void persistColumnNames(Item[] readerColumns) {
        final List<String> columnNames = columnStorage.getAllColumnNamesWithTransaction();
        for (final Item column : readerColumns) {
            if (!columnNames.contains(column.getName())) {
                columnStorage.storeWithTransaction((Column) column);
            }
        }
    }

    // package access for testing only tb 2014-03-20
    static void setFileProperties(List<SamplingPoint> samplingPoints, int id, byte datasetId, String datasetName) {
        for (SamplingPoint samplingPoint : samplingPoints) {
            samplingPoint.setInsituReference(id);
            samplingPoint.setInsituDatasetId(InsituDatasetId.create(datasetId));
            samplingPoint.setDatasetName(datasetName);
        }
    }

    private void initializeReader(File insituFile) throws IOException {
        String insituFilePath = insituFile.getPath();
        if (insituFilePath.startsWith(archiveDir.getAbsolutePath() + File.separator)) {
            insituFilePath = insituFilePath.substring(archiveDir.getAbsolutePath().length() + 1);
        }
        final DataFile dataFile = new DataFile(insituFilePath, sensor);
        reader.init(dataFile, archiveDir);
    }

    private void extractPointsInTimeRange(ArrayList<SamplingPoint> samplingPoints, TimeRange timeRange) throws IOException {
        final List<SamplingPoint> pointsInFile = reader.readSamplingPoints();
        for (final SamplingPoint samplingPoint : pointsInFile) {
            if (timeRange.includes(new Date(samplingPoint.getTime()))) {
                samplingPoints.add(samplingPoint);
            }
        }
    }

    private LinkedList<File> findFilesInTimeRange(TimeRange timeRange) {
        final File insituDirectory = new File(archiveDir, insituRelativePath);

        logInfo("Searching for insitu files ...: " + insituDirectory.getPath());
        final LinkedList<File> filesInRange = new LinkedList<>();
        final Collection<File> insituFiles = FileUtils.listFiles(insituDirectory, new String[]{"nc"}, true);
        logInfo("Found insitu files: " + insituFiles.size());

        logInfo("Extracting files in time-range... :" + timeRange.getStartDate() + " - " + timeRange.getStopDate());
        // @todo 1 rq/tb-20140217 - in the config file there are entries 'mms.source.44.filenamePattern' that could be used for filtering file names instead of 'nc'
        for (final File file : insituFiles) {
            try {
                final TimeRange fileTimeRange = extractTimeRange(file.getName());
                if (timeRange.intersectsWith(fileTimeRange)) {
                    filesInRange.add(file);
                }
            } catch (ParseException e) {
                logWarning(e.getMessage());
            }
        }
        logInfo("Found insitu files in time range: " + filesInRange.size());
        return filesInRange;
    }

    private void logWarning(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }

    private void logInfo(String message) {
        if (logger != null) {
            logger.info(message);
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

    // package access for testing only tb 2014-03-05
    static DataFile createDataFile(File insituFile, Sensor sensor) {
        final DataFile dataFile = new DataFile();

        dataFile.setPath(insituFile.getPath());
        dataFile.setSensor(sensor);
        return dataFile;
    }

    // package access for testing only tb 2014-03-19
    static byte extractInsituDatasetId(String fileName) {
        final int startIndex = fileName.indexOf("_") + 1;
        final int endIndex = fileName.indexOf("_", startIndex + 1);
        final String idString = fileName.substring(startIndex, endIndex);
        try {
            return Byte.parseByte(idString);
        } catch (NumberFormatException e) {
            return 8;
        }
    }

    private int persist(File insituFile) {
        final DataFile storageDatafile = storage.getDatafileWithTransaction(insituFile.getPath());

        if (storageDatafile == null) {
            final DataFile dataFile = createDataFile(insituFile, sensor);
            return storage.storeWithTransaction(dataFile);
        } else {
            return storageDatafile.getId();
        }
    }
}
