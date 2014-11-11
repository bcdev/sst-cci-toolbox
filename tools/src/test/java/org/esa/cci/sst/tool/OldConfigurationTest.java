package org.esa.cci.sst.tool;

import org.junit.Test;

import java.util.Properties;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

/**
 * {@author Bettina Scholze}
 * Date: 26.09.12 09:27
 */
public class OldConfigurationTest {

    @Test
    public void testGetString() throws Exception {
        Properties props = new Properties();
        String key = "myParam";
        props.setProperty(key, "C:\\Users\\bettina\\output\\regav\\2012-09-26     ");
        OldConfiguration oldConfiguration = new OldConfiguration(null, props);

        Parameter parameter = new Parameter(key, "TYPE", "/root/", "desc");
        assertEquals("C:\\Users\\bettina\\output\\regav\\2012-09-26", oldConfiguration.getString(parameter, false));
    }

    @Test
    public void testGetString_null() throws Exception {
        Properties props = new Properties();
        String key = "myParam";
        OldConfiguration oldConfiguration = new OldConfiguration(null, props);
        //execution with default
        Parameter parameter = new Parameter(key, "TYPE", "/root/", "desc");
        assertEquals("/root/", oldConfiguration.getString(parameter, false));
        //execution without default
        parameter = new Parameter(key, "TYPE", null, "desc");
        assertNull(oldConfiguration.getString(parameter, false));
    }


}
