package org.esa.cci.sst.tools.samplepoint;

import org.apache.commons.io.FileUtils;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Sensor;
import org.esa.cci.sst.data.SensorBuilder;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.reader.ReaderFactory;
import org.esa.cci.sst.util.SamplingPoint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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

    public List<SamplingPoint> generate() {
        final ArrayList<SamplingPoint> samplingPoints = new ArrayList<>();

        final Collection<File> insituFiles = FileUtils.listFiles(archiveDir, new String[]{"nc"}, true);
        for (Iterator<File> iterator = insituFiles.iterator(); iterator.hasNext(); ) {
            final File insituFile = iterator.next();
            final DataFile dataFile = new DataFile(insituFile.getName(), sensor);

            try {
                reader.init(dataFile, archiveDir);
                final List<SamplingPoint> pointsInFile = reader.readSamplingPoints();
                samplingPoints.addAll(pointsInFile);
            } catch (IOException e) {
                // @todo 2 tb/tb add logging of errors tb 2014-02-12
                System.out.println("e.getMessage() = " + e.getMessage());
            } finally {
                reader.close();
            }
        }

        return samplingPoints;
    }
}
