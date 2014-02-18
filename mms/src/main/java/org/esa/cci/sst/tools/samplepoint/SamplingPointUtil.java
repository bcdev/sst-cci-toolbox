package org.esa.cci.sst.tools.samplepoint;


import java.text.DecimalFormat;

class SamplingPointUtil {

    private static final DecimalFormat monthFormat = new DecimalFormat("00");

    static String createPath(String archivRoot, String sensorName, int year, int month, char key) {
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
