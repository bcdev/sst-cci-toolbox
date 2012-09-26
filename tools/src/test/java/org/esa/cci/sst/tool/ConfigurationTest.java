package org.esa.cci.sst.tool;

import org.junit.Test;

import java.util.Properties;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

/**
 * {@author Bettina Scholze}
 * Date: 26.09.12 09:27
 */
public class ConfigurationTest {

    @Test
    public void testGetString() throws Exception {
        Properties props = new Properties();
        String key = "myParam";
        props.setProperty(key, "C:\\Users\\bettina\\output\\regav\\2012-09-26     ");
        Configuration configuration = new Configuration(null, props);

        Parameter parameter = new Parameter(key, "TYPE", "/root/", "desc");
        assertEquals("C:\\Users\\bettina\\output\\regav\\2012-09-26", configuration.getString(parameter, false));
    }

    @Test
    public void testGetString_null() throws Exception {
        Properties props = new Properties();
        String key = "myParam";
        Configuration configuration = new Configuration(null, props);
        //execution with default
        Parameter parameter = new Parameter(key, "TYPE", "/root/", "desc");
        assertEquals("/root/", configuration.getString(parameter, false));
        //execution without default
        parameter = new Parameter(key, "TYPE", null, "desc");
        assertNull(configuration.getString(parameter, false));
    }


}
