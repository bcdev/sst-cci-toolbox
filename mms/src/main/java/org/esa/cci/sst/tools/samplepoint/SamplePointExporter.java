package org.esa.cci.sst.tools.samplepoint;


import org.esa.cci.sst.util.SamplingPoint;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SamplePointExporter {

    private static final DecimalFormat monthFormat = new DecimalFormat("00");

    public void export(List<SamplingPoint> samplingPoints, TimeRange samplingInterval) {
        final TimeRange monthBefore = samplingInterval.getMonthBefore();
        final TimeRange centerMonth = samplingInterval.getCenterMonth();
        final TimeRange monthAfter = samplingInterval.getMonthAfter();

        final List<SamplingPoint> pointMonthBefore = extractSamples(samplingPoints, monthBefore);
        final List<SamplingPoint> pointsCenterMonth = extractSamples(samplingPoints, centerMonth);
        final List<SamplingPoint> pointMonthAfter = extractSamples(samplingPoints, monthAfter);

        if (!samplingPoints.isEmpty()) {
            // @todo 1 tb/tb add logging entry that we still have remaining point - which should not be! tb 2014-02-17
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
}
