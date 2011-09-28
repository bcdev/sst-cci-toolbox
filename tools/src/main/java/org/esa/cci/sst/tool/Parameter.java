package org.esa.cci.sst.tool;

/**
 * Simple abstraction of a parameter.
 *
 * @author Norman Fomferra
 */
public class Parameter {
    private final String name;
    private final String type;
    private final String defaultValue;
    private final String description;

    public Parameter(String name, String type, String defaultValue, String description) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }
}
