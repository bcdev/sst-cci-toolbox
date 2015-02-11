package org.esa.cci.sst.util;

import org.esa.cci.sst.TestHelper;
import org.esa.cci.sst.common.InsituDatasetId;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class SamplingPointIOMain {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Must supply number of sampling points and output directory");
            System.exit(1);
        }

        final int numSamplingPoints = Integer.parseInt(args[0]);

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        TestHelper.traceMemory();
        System.out.println("Creating " + numSamplingPoints + " sampling points");

        final ArrayList<SamplingPoint> samplingPointList = new ArrayList<>(numSamplingPoints);
        for (int i = 0; i < numSamplingPoints; i++) {
            final double angleDelta = numSamplingPoints / (double) i;
            final SamplingPoint samplingPoint = new SamplingPoint();
            samplingPoint.setDatasetName("ds_name_" + i);
            samplingPoint.setIndex(11 + i);
            samplingPoint.setInsituDatasetId(InsituDatasetId.drifter);
            samplingPoint.setInsituReference(12 + i);
            samplingPoint.setLat(13 + angleDelta);
            samplingPoint.setLon(14 + angleDelta);
            samplingPoint.setReference(15 + i);
            samplingPoint.setReference2(16 + i);
            samplingPoint.setReferenceTime(17 + i);
            samplingPoint.setReference2Time(18 + i);
            samplingPoint.setReferenceLat(19 - angleDelta);
            samplingPoint.setReferenceLon(20 - angleDelta);
            samplingPoint.setTime(22 + i);
            samplingPoint.setX(23 + i);
            samplingPoint.setY(24 + i);

            samplingPointList.add(samplingPoint);
        }

        stopWatch.stop();
        System.out.println("Done " + ((double) stopWatch.getElapsedMillis()) / 1000.0 + " sec");
        TestHelper.traceMemory();

        final File file = new File(args[1], "sampling_" + new Date().getTime() + ".json");
        file.createNewFile();
        final FileOutputStream outputStream = new FileOutputStream(file);

        System.out.println("Start writing to disk");
        stopWatch.start();

        SamplingPointIO.write(samplingPointList, outputStream);

        outputStream.close();

        stopWatch.stop();
        System.out.println("Done " + ((double) stopWatch.getElapsedMillis()) / 1000.0 + " sec");
        TestHelper.traceMemory();

        System.out.println("file size: " + file.length() * TestHelper.to_MB + " MB");
    }
}
