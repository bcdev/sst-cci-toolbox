package org.esa.cci.sst;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.fail;

public class TestUtil {

    public static File getTestDataDirectory() throws IOException {
        final InputStream resourceStream = TestUtil.class.getResourceAsStream("dataDirectory.properties");
        final Properties properties = new Properties();
        properties.load(resourceStream);
        final String dataDirectoryProperty = properties.getProperty("dataDirectory");
        if (dataDirectoryProperty == null) {
            fail("Property 'dataDirectory' is not set.");
        }
        final File dataDirectory = new File(dataDirectoryProperty);
        if (!dataDirectory.isDirectory()) {
            fail("Property 'dataDirectory' supplied does not exist: '" + dataDirectoryProperty + "'");
        }
        return dataDirectory;
    }
}
