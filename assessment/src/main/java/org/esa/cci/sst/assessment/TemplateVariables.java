package org.esa.cci.sst.assessment;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

class TemplateVariables {

    private static final String DEFAULT = ".default";

    private Properties properties;

    TemplateVariables() {
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
                extractProperty(resultMap, propertyName);
            }
        }

        return resultMap;
    }

    Map<String, String> getParagraphVariables() {
        final Set<String> propertyNames = properties.stringPropertyNames();
        final HashMap<String, String> resultMap = new HashMap<>();
        for (final String propertyName : propertyNames) {
            if (isParagraphProperty(propertyName)) {
                extractProperty(resultMap, propertyName);
            }
        }

        return resultMap;
    }

    Map<String, String> getFigureVariables() {
        final Set<String> propertyNames = properties.stringPropertyNames();
        final HashMap<String, String> resultMap = new HashMap<>();
        for (final String propertyName : propertyNames) {
            if (isFigureProperty(propertyName)) {
                extractProperty(resultMap, propertyName);
            }
        }

        return resultMap;
    }

    String getFiguresDirectory() {
        return properties.getProperty("figures.directory");
    }

    static boolean isDefaultProperty(String propertyName) {
        return propertyName.endsWith(DEFAULT);
    }

    static String getPropertyNameFromDefault(String defaultPropertyName) {
        final int defaultIndex = defaultPropertyName.indexOf(DEFAULT);
        if (defaultIndex > 0) {
            return defaultPropertyName.substring(0, defaultIndex);
        }
        return defaultPropertyName;
    }

    static boolean isParagraphProperty(String propertyName) {
        return propertyName.startsWith("paragraph.") || propertyName.startsWith("comment.");
    }

    static boolean isFigureProperty(String propertyName) {
        return propertyName.startsWith("figure.") && !propertyName.contains(".scale");
    }

    private void extractProperty(HashMap<String, String> resultMap, String propertyName) {
        if (isDefaultProperty(propertyName)) {
            final String originalProperty = getPropertyNameFromDefault(propertyName);
            if (resultMap.containsKey(originalProperty)) {
                return;
            }

            String propertyValue = properties.getProperty(originalProperty);
            if (propertyValue == null) {
                propertyValue = properties.getProperty(propertyName);
            }

            resultMap.put(originalProperty, propertyValue);
        } else {
            // @todo 2 tb/tb remove scale properties. There shouldn't be scaled word.* properties, but you never know 2016-06-07
            final String propertyValue = properties.getProperty(propertyName);

            resultMap.put(propertyName, propertyValue);
        }
    }
}
