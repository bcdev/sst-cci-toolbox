package org.esa.cci.sst.assessment;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

class TemplateVariables {

    private Properties properties;

    public TemplateVariables() {
        properties = new Properties();
    }

    void load(InputStream inputStream) throws IOException {
        properties.load(inputStream);
    }

    Map<String, String> getWordVariables() {
        final Set<String> propertyNames = properties.stringPropertyNames();
        final HashMap<String, String> resultMap = new HashMap<>();

        for (final String propertyName : propertyNames) {
            if (propertyName.startsWith("word.")) {
                final String propertyValue = properties.getProperty(propertyName);
                // @todo 1 tb/tb add handling of default values 2016-06-06

                resultMap.put(propertyValue, propertyValue);
            }
        }

        return resultMap;
    }
}
