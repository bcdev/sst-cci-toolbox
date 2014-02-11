package org.esa.cci.sst.tools;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Properties;

import static org.junit.Assert.*;

public class ConfigurationTest {

    private Configuration configuration;

    @Before
    public void setUp() {
        configuration = new Configuration();
    }

    @Test
    public void testPutAndGetStringValue() {
        configuration.put("mms.toms.key", "the value");
        assertEquals("the value", configuration.getStringValue("mms.toms.key"));
    }

    @Test
    public void testPutAndGetStringValue_withDefaultValue() {
        assertEquals("the default", configuration.getStringValue("missing.toms.key", "the default"));
    }

    @Test
    public void testPutAndGetDateValue() {
        configuration.put("date.key", "1978-01-01T00:00:00Z");

        final Date dateValue = configuration.getDateValue("date.key");
        assertNotNull(dateValue);
        assertEquals(252460800000L, dateValue.getTime());
    }

    @Test
    public void testPutAndGetDateValue_withDefaultValue() {
        configuration.put("date.key", "1978-01-01T00:00:00Z");

        final Date dateValue = configuration.getDateValue("invalid.date.key", "2001-01-01T00:00:00Z");
        assertNotNull(dateValue);
        assertEquals(978307200000L, dateValue.getTime());
    }

    @Test
    public void testPutAndGetDateValue_unparseableDate() {
        configuration.put("date.key", "Christmas");

        try {
            configuration.getDateValue("date.key");
            fail("ToolException expected");
        } catch (ToolException expected) {
            assertEquals("Cannot parse start or stop date.", expected.getMessage());
        }
    }

    @Test
    public void testPutAndGetBooleanValue() {
        configuration.put("true_bool_key", "true");
        configuration.put("false_bool_key", "false");

        assertTrue(configuration.getBooleanValue("true_bool_key"));
        assertFalse(configuration.getBooleanValue("false_bool_key"));
    }

    @Test
    public void testPutAndGetIntValue_withDefault() {
        configuration.put("a_key", "94");

        assertEquals(94, configuration.getIntValue("a_key", 107));
        assertEquals(107, configuration.getIntValue("no_key", 107));
    }

    @Test
    public void testPutAndGetIntValue_unparseableInteger() {
        configuration.put("a_key", "fourteen");

        try {
            configuration.getIntValue("a_key", 107);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    @Test
    public void testLoad() throws IOException {
        final String configFileContent = "key.1 = value.1\n" +
                "date.key = 1979-01-01T00:00:00Z\n" +
                "key.2 = value.2";

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(configFileContent.getBytes());
        final InputStreamReader reader = new InputStreamReader(inputStream);

        configuration.load(reader);

        assertEquals("value.1", configuration.getStringValue("key.1"));
        assertEquals("value.2", configuration.getStringValue("key.2"));
        final Date dateValue = configuration.getDateValue("date.key");
        assertEquals(283996800000L, dateValue.getTime());
    }

    @Test
    public void testLoadAppendsNewProperties() throws IOException {
        final String configFileContent = "key.1 = value.1\n";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(configFileContent.getBytes());
        final InputStreamReader reader = new InputStreamReader(inputStream);

        configuration.put("old_key", "old_value");

        configuration.load(reader);
        assertEquals("value.1", configuration.getStringValue("key.1"));
        assertEquals("old_value", configuration.getStringValue("old_key"));
    }

    @Test
    public void testAddProperties() {
        final Properties toAppend = new Properties();
        toAppend.put("appended_key", "appended_value");

        configuration.put("initial_key", "initial_value");

        configuration.add(toAppend);

        assertEquals("initial_value", configuration.getStringValue("initial_key"));
        assertEquals("appended_value", configuration.getStringValue("appended_key"));
    }

    @Test
    public void testAddProperties_withMandatoryPrefix() {
        final Properties toAppend = new Properties();
        toAppend.put("appended_key", "appended_value");
        toAppend.put("mms.key", "mms.value");

        configuration.put("initial_key", "initial_value");

        configuration.add(toAppend, "mms.");

        assertEquals("initial_value", configuration.getStringValue("initial_key"));
        assertEquals("mms.value", configuration.getStringValue("mms.key"));
        assertNull(configuration.getStringValue("appended_key"));
    }

    @Test
    public void testGetAsProperties() {
        configuration.put("key", "value");

        final Properties asProperties = configuration.getAsProperties();
        assertNotNull(asProperties);
        assertEquals("value", asProperties.getProperty("key"));
    }
}
