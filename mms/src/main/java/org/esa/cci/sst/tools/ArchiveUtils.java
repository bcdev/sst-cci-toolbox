package org.esa.cci.sst.tools;


import java.text.DecimalFormat;

public class ArchiveUtils {

    private static final DecimalFormat monthFormat = new DecimalFormat("00");

    public static String createCleanFilePath(String archiveRoot, String[] sensorNames, int year, int month) {
        final StringBuilder stringBuilder = new StringBuilder(256);
        stringBuilder.append(archiveRoot);
        stringBuilder.append("/clean/");

        final StringBuilder sensorTagBuilder = new StringBuilder(64);
        sensorTagBuilder.append(sensorNames[0]);
        for (int i = 1; i < sensorNames.length; i++) {
            sensorTagBuilder.append(',');
            sensorTagBuilder.append(sensorNames[i]);
        }
        final String sensorTag = sensorTagBuilder.toString();

        stringBuilder.append(sensorTag);
        stringBuilder.append('/');
        stringBuilder.append(year);
        stringBuilder.append('/');
        stringBuilder.append(sensorTag);
        stringBuilder.append("-clean-");
        stringBuilder.append(year);
        stringBuilder.append('-');
        stringBuilder.append(monthFormat.format(month));
        stringBuilder.append(".json");
        return stringBuilder.toString();
    }
}
