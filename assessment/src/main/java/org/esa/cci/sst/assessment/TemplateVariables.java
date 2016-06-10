package org.esa.cci.sst.assessment;

import org.esa.beam.util.StringUtils;

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

    Map<String, String> getFiguresVariables() {
        final Set<String> propertyNames = properties.stringPropertyNames();
        final HashMap<String, String> resultMap = new HashMap<>();
        for (final String propertyName : propertyNames) {
            if (isFiguresProperty(propertyName)) {
                extractProperty(resultMap, propertyName);
            }
        }

        return resultMap;
    }

    double getScale(String figureKey) {
        final String propertyName = getPropertyNameFromDefault(figureKey);
        final String scalePropertyName = createScaleName(propertyName);
        final String scaleValueString = properties.getProperty(scalePropertyName);
        if (StringUtils.isNullOrEmpty(scaleValueString)) {
            return 0.2;
        }

        return Double.parseDouble(scaleValueString.trim());
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

    static boolean isFiguresProperty(String propertyName) {
        return propertyName.startsWith("figures.") && !propertyName.contains(".scale");
    }

    static String createScaleName(String figureName) {
        return figureName + ".scale";
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
