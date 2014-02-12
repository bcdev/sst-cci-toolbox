package org.esa.cci.sst.tools;

import org.esa.cci.sst.util.TimeUtil;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

public class Configuration {

    public static final String KEY_CONFIGURATION = "mms.configuration";

    public static final String KEY_ARC3_DESTDIR = "mms.arc3.destdir";

    public static final String KEY_ARCHIVE_ROOTDIR = "mms.archive.rootdir";

    public static final String KEY_NWP_ARC3_SENSOR = "mms.nwp_arc3.sensor";
    public static final String KEY_NWP_DESTDIR = "mms.nwp.destdir";

    public static final String KEY_MATCHUP_START_TIME = "mms.matchup.startTime";
    public static final String KEY_MATCHUP_STOP_TIME = "mms.matchup.stopTime";

    public static final String KEY_MMS_REINGESTION_FILENAME = "mms.reingestion.filename";

    public static final String KEY_SOURCE_START_TIME = "mms.source.startTime";
    public static final String KEY_SOURCE_STOP_TIME = "mms.source.stopTime";

    public static final String KEY_MMS_SAMPLING_SENSOR = "mms.sampling.sensor";
    public static final String KEY_MMS_SAMPLING_START_TIME = "mms.sampling.startTime";
    public static final String KEY_MMS_SAMPLING_STOP_TIME = "mms.sampling.stopTime";
    public static final String KEY_MMS_SAMPLING_SHOW_MAPS = "mms.sampling.showmaps";
    public static final String KEY_MMS_SAMPLING_COUNT = "mms.sampling.count";
    public static final String KEY_MMS_SAMPLING_SKIP = "mms.sampling.skip";
    public static final String KEY_MMS_SAMPLING_SUBSCENE_WIDTH = "mms.sampling.subscene.width";
    public static final String KEY_MMS_SAMPLING_SUBSCENE_HEIGHT = "mms.sampling.subscene.height";

    private final Properties properties;

    public Configuration() {
        properties = new Properties();
    }

    public void put(String key, String value) {
        properties.put(key, value);
    }

    public void load(Reader reader) throws IOException {
        properties.load(reader);
    }

    public String getStringValue(String key) {
        return properties.getProperty(key);
    }

    public String getStringValue(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public Date getDateValue(String key) {
        final String dateString = (String) properties.get(key);
        try {
            return TimeUtil.parseCcsdsUtcFormat(dateString);
        } catch (ParseException e) {
            throw new ToolException("Cannot parse start or stop date.", e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    public Date getDateValue(String key, String defaultValue) {
        final String dateString = properties.getProperty(key, defaultValue);

        try {
            return TimeUtil.parseCcsdsUtcFormat(dateString);
        } catch (ParseException e) {
            throw new ToolException("Cannot parse start or stop date.", e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    public boolean getBooleanValue(String key) {
        final String boolString = (String) properties.get(key);
        return Boolean.parseBoolean(boolString);
    }

    public int getIntValue(String key, int defaultValue) {
        final String intString = properties.getProperty(key);
        if (intString == null){
            return defaultValue;
        }
        try {
            return Integer.parseInt(intString);
        } catch (NumberFormatException e) {
            throw new ToolException("Cannot parse integer value: " + key, e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    public void add(Properties toAdd) {
        for (final Map.Entry entry : toAdd.entrySet()) {
            this.properties.put(entry.getKey(), entry.getValue());
        }
    }

    public void add(Properties toAdd, String keyPrefix) {
        for (final Map.Entry entry : toAdd.entrySet()) {
            final String key = (String) entry.getKey();
            if (key.startsWith(keyPrefix)) {
                this.properties.put(key, entry.getValue());
            }
        }
    }

    public Properties getAsProperties() {
        return properties;
    }
}
