package org.esa.cci.sst.tools;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
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
    public void testPutAndGetStringValue_valueNotInConfig() {
        try {
            configuration.getStringValue("mms.toms.key");
            fail("ToolException expected");
        } catch (ToolException expected) {
            assertEquals("No value for: mms.toms.key", expected.getMessage());
        }
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
    public void testPutAndGetDateValue_withDefaultValue_unparseable() {
        configuration.put("date.key", "1978-01-01T00:00:00Z");

        try {
            configuration.getDateValue("invalid.date.key", "easter");
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
    public void testGetBooleanValue_noValueInConfig() {
        try {
            configuration.getBooleanValue("unconfigured_key");
            fail("ToolException expected");
        } catch (ToolException expected) {
            assertEquals("No value for: unconfigured_key", expected.getMessage());
        }
    }

    @Test
    public void testPutAndGetDoubleValue_withDefault() {
        configuration.put("a_key", "94.7");

        assertEquals(94.7, configuration.getDoubleValue("a_key", 107.3), 0.0);
        assertEquals(107.3, configuration.getDoubleValue("no_key", 107.3), 0.0);
    }

    @Test
    public void testPutAndGetDoubleValue_withoutDefault() {
        configuration.put("a_key", "94.7");

        assertEquals(94.7, configuration.getDoubleValue("a_key"), 0.0);
        try {
            configuration.getDoubleValue("no_key");
            fail("ToolException expected");
        } catch (ToolException expected) {
            assertEquals("No value for: no_key", expected.getMessage());
        }
    }

    @Test
    public void testPutAndGetDoubleValue_unparseableValue() {
        configuration.put("a_key", "alotofthings");

        try {
            configuration.getDoubleValue("a_key");
            fail("ToolException expected");
        } catch (ToolException expected) {
            assertEquals("Cannot parse double value: a_key: alotofthings", expected.getMessage());
        }
    }

    @Test
    public void testPutAndGetDoubleValue_withDefault_unparseableValue() {
        configuration.put("a_key", "stupid");

        try {
            configuration.getDoubleValue("a_key", 23.9);
            fail("ToolException expected");
        } catch (ToolException expected) {
            assertEquals("Cannot parse double value: a_key: stupid", expected.getMessage());
        }
    }

    @Test
    public void testPutAndGetIntValue_withDefault() {
        configuration.put("a_key", "94");

        assertEquals(94, configuration.getIntValue("a_key", 107));
        assertEquals(107, configuration.getIntValue("no_key", 107));
    }

    @Test
    public void testGetIntValue_noValueInConfig() {
        try {
            configuration.getIntValue("nasenmann.org");
            fail("ToolException expected");
        } catch (ToolException expected) {
            assertEquals("No value for: nasenmann.org", expected.getMessage());
        }
    }

    @Test
    public void testGetIntValue_unparseableValue() {
        configuration.put("int_key", "fourteen");

        try {
            configuration.getIntValue("int_key");
            fail("ToolException expected");
        } catch (ToolException expected) {
            assertEquals("Cannot parse integer value: int_key: fourteen", expected.getMessage());
        }
    }

    @Test
    public void testPutAndGetIntValue_withDefault_unparseableInteger() {
        configuration.put("a_key", "fourteen");

        try {
            configuration.getIntValue("a_key", 107);
            fail("ToolException expected");
        } catch (ToolException expected) {
            assertEquals("Cannot parse integer value: a_key: fourteen", expected.getMessage());
        }
    }

    @Test
    public void testPutAndGetBigIntegerValue_withDefault() {
        configuration.put("a_key", "2147483648");

        final BigInteger defaultValue = new BigInteger("107");
        assertEquals(new BigInteger("2147483648"), configuration.getBigIntegerValue("a_key", defaultValue));
        assertEquals(defaultValue, configuration.getBigIntegerValue("no_key", defaultValue));
    }

    @Test
    public void testPutAndGetBigIntegerValue_unpareseableValue() {
        configuration.put("a_key", "huge_number");

        final BigInteger defaultValue = new BigInteger("107");
        try {
            configuration.getBigIntegerValue("a_key", defaultValue);
            fail("ToolException expected");
        } catch (ToolException expected) {
            assertEquals("Cannot parse big integer value: a_key: huge_number", expected.getMessage());
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


        try {
            configuration.getStringValue("appended_key");
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    @Test
    public void testGetAsProperties() {
        configuration.put("key", "value");

        final Properties asProperties = configuration.getAsProperties();
        assertNotNull(asProperties);
        assertEquals("value", asProperties.getProperty("key"));
    }

    @Test
    public void testGetSensorPattern_withoutDefaultValue() throws Exception {
        try {
            configuration.getPattern("mysensor");
            fail();
        } catch (Exception expected) {
        }

        configuration.put("mysensor", "8000000000000000");
        try {
            configuration.getPattern("mysensor");
            fail();
        } catch (Exception expected) {
        }

        configuration.put("mms.pattern.mysensor", "8000000000000000");
        assertEquals(0x8000000000000000L, configuration.getPattern("mysensor"));
    }

    @Test
    public void testGetSensorPattern_withTooManyBits() throws Exception {
        configuration.put("mms.pattern.mysensor", "10000000000000000");
        try {
            configuration.getPattern("mysensor");
            fail();
        } catch (Exception expexted) {
        }
    }

    @Test
    public void testGetSensorPattern_withTooManyBitsSet() throws Exception {
        configuration.put("mms.pattern.mysensor", "8000000000000001");
        try {
            configuration.getPattern("mysensor");
            fail();
        } catch (Exception expexted) {
        }
    }

    @Test
    public void testGetSensorPattern_withDefaultValue() throws Exception {
        assertEquals(8L, configuration.getPattern("mysensor", 8L));

        configuration.put("mms.pattern.mysensor", "100");
        assertEquals(256, configuration.getPattern("mysensor", 8L));
    }
}
