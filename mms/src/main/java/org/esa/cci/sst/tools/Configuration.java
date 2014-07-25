package org.esa.cci.sst.tools;

import org.apache.commons.lang.StringUtils;
import org.esa.cci.sst.util.TimeUtil;

import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

public class Configuration {

    public static final String KEY_MMS_ARCHIVE_ROOT = "mms.archive.root";
    public static final String KEY_MMS_CONFIGURATION = "mms.configuration";
    public static final String KEY_MMS_IO_TMPDELETEONEXIT = "mms.io.tmpdeleteonexit";
    public static final String KEY_MMS_PATTERN_PREFIX = "mms.pattern.";
    public static final String KEY_MMS_DIRTY_MASK_PREFIX = "mms.dirty.";
    public static final String KEY_MMS_USECASE = "mms.usecase";

    public static final String KEY_MMS_INGESTION_CLEANUPINTERVAL = "mms.ingestion.cleanupinterval";
    public static final String KEY_MMS_INGESTION_START_TIME = "mms.source.startTime";
    public static final String KEY_MMS_INGESTION_STOP_TIME = "mms.source.stopTime";

    public static final String KEY_MMS_MATCHUP_START_TIME = "mms.matchup.startTime";
    public static final String KEY_MMS_MATCHUP_STOP_TIME = "mms.matchup.stopTime";
    public static final String KEY_MMS_MATCHUP_PRIMARY_SENSOR = "mms.matchup.primarysensor";

    public static final String KEY_MMS_MMD_DIMENSIONS = "mms.target.dimensions";
    public static final String KEY_MMS_MMD_TARGET_DIR = "mms.target.dir";
    public static final String KEY_MMS_MMD_TARGET_FILENAME = "mms.target.filename";

    public static final String KEY_MMS_SAMPLING_GENERATOR = "mms.sampling.generator";
    public static final String KEY_MMS_SAMPLING_SENSOR = "mms.sampling.sensor";
    public static final String KEY_MMS_SAMPLING_SENSOR_2 = "mms.sampling.sensor.2";
    public static final String KEY_MMS_SAMPLING_START_TIME = "mms.sampling.startTime";
    public static final String KEY_MMS_SAMPLING_STOP_TIME = "mms.sampling.stopTime";
    public static final String KEY_MMS_SAMPLING_COUNT = "mms.sampling.count";
    public static final String KEY_MMS_SAMPLING_SKIP = "mms.sampling.skip";
    public static final String KEY_MMS_SAMPLING_SUBSCENE_WIDTH = "mms.sampling.subscene.width";
    public static final String KEY_MMS_SAMPLING_SUBSCENE_HEIGHT = "mms.sampling.subscene.height";
    public static final String KEY_MMS_SAMPLING_SEARCH_TIME_FUTURE = "mms.sampling.searchtime.future";
    public static final String KEY_MMS_SAMPLING_SEARCH_TIME_PAST_2 = "mms.sampling.searchtime.past.2";
    public static final String KEY_MMS_SAMPLING_SEARCH_TIME_FUTURE_2 = "mms.sampling.searchtime.future.2";
    public static final String KEY_MMS_SAMPLING_SEARCH_TIME_PAST = "mms.sampling.searchtime.past";
    public static final String KEY_MMS_SAMPLING_CLEANUP = "mms.sampling.cleanup";
    public static final String KEY_MMS_SAMPLING_CLEANUP_INTERVAL = "mms.sampling.cleanupinterval";
    public static final String KEY_MMS_SAMPLING_DIRTY_PIXEL_FRACTION = "mms.sampling.dirtypixelfraction";
    public static final String KEY_MMS_SAMPLING_REFERENCE_SENSOR = "mms.sampling.referencesensor";
    public static final String KEY_MMS_SAMPLING_EXTRACTION_TIME = "mms.sampling.time.insituextraction";

    public static final String KEY_MMS_MAPPLOT_STATEGY = "mms.mapplot.strategy";
    public static final String KEY_MMS_MAPPLOT_TARGET_DIR = "mms.mapplot.target.dir";
    public static final String KEY_MMS_MAPPLOT_TARGET_FILENAME = "mms.mapplot.target.filename";
    public static final String KEY_MMS_MAPPLOT_TITLE = "mms.mapplot.title";
    public static final String KEY_MMS_MAPPLOT_SHOW = "mms.mapplot.show";
    public static final String KEY_MMS_MAPPLOT_STOP_TIME = "mms.mapplot.stoptime";
    public static final String KEY_MMS_MAPPLOT_START_TIME = "mms.mapplot.starttime";
    public static final String KEY_MMS_MAPPLOT_SENSOR = "mms.mapplot.sensor";

    public static final String KEY_MMS_NWP_CDO_HOME = "mms.nwp.cdo.home";
    public static final String KEY_MMS_NWP_MMD_SOURCE = "mms.nwp.mmd.source";
    public static final String KEY_MMS_NWP_NWP_SOURCE = "mms.nwp.nwp.source";
    public static final String KEY_MMS_NWP_FOR_SENSOR = "mms.nwp.forsensor";
    public static final String KEY_MMS_NWP_SENSOR = "mms.nwp.sensor";
    public static final String KEY_MMS_NWP_NWP_TARGET = "mms.nwp.nwp.target";

    public static final String KEY_MMS_GBCS_INTELVERSION = "mms.gbcs.intelversion";
    public static final String KEY_MMS_GBCS_VERSION = "mms.gbcs.version";
    public static final String KEY_MMS_GBCS_HOME = "mms.gbcs.home";
    public static final String KEY_MMS_GBCS_MMD_SOURCE = "mms.gbcs.mmd.source";
    public static final String KEY_MMS_GBCS_NWP_SOURCE = "mms.gbcs.nwp.source";
    public static final String KEY_MMS_GBCS_MMD_TARGET = "mms.gbcs.mmd.target";
    public static final String KEY_MMS_GBCS_SENSOR = "mms.gbcs.sensor";

    public static final String KEY_MMS_REINGESTION_LOCATED = "mms.reingestion.located";
    public static final String KEY_MMS_REINGESTION_OVERWRITE = "mms.reingestion.overwrite";
    public static final String KEY_MMS_REINGESTION_PATTERN = "mms.reingestion.pattern";
    public static final String KEY_MMS_REINGESTION_SENSOR = "mms.reingestion.sensor";
    public static final String KEY_MMS_REINGESTION_SOURCE = "mms.reingestion.source";

    // yet unclear properties
    public static final String KEY_ARC3_DESTDIR = "mms.arc3.destdir";
    public static final String KEY_NWP_ARC3_SENSOR = "mms.nwp_arc3.sensor";
    public static final String KEY_NWP_DESTDIR = "mms.nwp.destdir";

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
        final String property = properties.getProperty(key);
        if (StringUtils.isEmpty(property)) {
            throw new ToolException("No value for: " + key, ToolException.TOOL_CONFIGURATION_ERROR);
        }
        return property;
    }

    public String getStringValue(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public Date getDateValue(String key) {
        final String dateString = (String) properties.get(key);
        if (StringUtils.isEmpty(dateString)) {
            throw new ToolException("No date value for: " + key, ToolException.TOOL_CONFIGURATION_ERROR);
        }

        return parseDateString(dateString);
    }

    public Date getDateValue(String key, String defaultValue) {
        final String dateString = properties.getProperty(key, defaultValue);

        return parseDateString(dateString);
    }

    public boolean getBooleanValue(String key) {
        final String boolString = (String) properties.get(key);
        if (StringUtils.isEmpty(boolString)) {
            throw new ToolException("No value for: " + key, ToolException.TOOL_CONFIGURATION_ERROR);
        }
        return Boolean.parseBoolean(boolString);
    }

    public boolean getBooleanValue(String key, boolean defaultValue) {
        final String boolString = (String) properties.get(key);
        if (StringUtils.isEmpty(boolString)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(boolString);
    }

    public double getDoubleValue(String key) {
        final String doubleString = properties.getProperty(key);
        if (doubleString == null) {
            throw new ToolException("No value for: " + key, ToolException.TOOL_CONFIGURATION_ERROR);
        }
        try {
            return Double.parseDouble(doubleString);
        } catch (NumberFormatException e) {
            throw new ToolException("Cannot parse double value: " + key + ": " + doubleString, e,
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    public double getDoubleValue(String key, double defaultValue) {
        final String doubleString = properties.getProperty(key);
        if (doubleString == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(doubleString);
        } catch (NumberFormatException e) {
            throw new ToolException("Cannot parse double value: " + key + ": " + doubleString, e,
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    public int getIntValue(String key) {
        final String intString = properties.getProperty(key);
        if (intString == null) {
            throw new ToolException("No value for: " + key, ToolException.TOOL_CONFIGURATION_ERROR);
        }
        try {
            return Integer.parseInt(intString);
        } catch (NumberFormatException e) {
            throw new ToolException("Cannot parse integer value: " + key + ": " + intString, e,
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    public int getIntValue(String key, int defaultValue) {
        final String intString = properties.getProperty(key);
        if (intString == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(intString);
        } catch (NumberFormatException e) {
            throw new ToolException("Cannot parse integer value: " + key + ": " + intString, e,
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    public long getPatternValue(String key) {
        return parsePattern(key, getStringValue(key));
    }

    public void add(Properties toAdd) {
        for (final Map.Entry entry : toAdd.entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
        }
    }

    public void add(Properties toAdd, String keyPrefix) {
        for (final Map.Entry entry : toAdd.entrySet()) {
            final String key = (String) entry.getKey();
            if (key.startsWith(keyPrefix)) {
                properties.put(key, entry.getValue());
            }
        }
    }

    public Properties getAsProperties() {
        return properties;
    }

    public String getDirtyMaskExpression(String sensorName) {
        final String key = KEY_MMS_DIRTY_MASK_PREFIX + sensorName;

        return getStringValue(key, null);
    }

    public long getPattern(String sensorName) {
        final String key = KEY_MMS_PATTERN_PREFIX + sensorName;
        final String value = getStringValue(key);

        return parsePattern(key, value);
    }

    public long getPattern(String sensorName, long defaultValue) {
        final String key = KEY_MMS_PATTERN_PREFIX + sensorName;
        final String value;

        try {
            value = getStringValue(key);
        } catch (ToolException e) {
            return defaultValue;
        }

        return parsePattern(key, value);
    }

    public BigInteger getBigIntegerValue(String key, BigInteger defaultValue) {
        final String intString = properties.getProperty(key);
        if (intString == null) {
            return defaultValue;
        }
        try {
            return new BigInteger(intString);
        } catch (NumberFormatException e) {
            throw new ToolException("Cannot parse big integer value: " + key + ": " + intString, e,
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    public boolean containsValue(String key) {
        return properties.containsKey(key);
    }

    private long parsePattern(String key, String value) {
        final long pattern;
        try {
            final BigInteger bigInteger = new BigInteger(value, 16);
            if (bigInteger.bitLength() > 64) {
                throw new ToolException("Too many bits in pattern: " + key, ToolException.TOOL_CONFIGURATION_ERROR);
            }
            if (bigInteger.bitCount() > 1) {
                throw new ToolException("Too many bits set in pattern: " + key, ToolException.TOOL_CONFIGURATION_ERROR);
            }
            pattern = bigInteger.longValue();
        } catch (NumberFormatException e) {
            throw new ToolException("Cannot parse pattern: " + key, e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
        return pattern;
    }

    private Date parseDateString(String dateString) {
        try {
            return TimeUtil.parseCcsdsUtcFormat(dateString);
        } catch (ParseException e) {
            throw new ToolException("Cannot parse start or stop date.", e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }
}
