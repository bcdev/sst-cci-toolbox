package org.esa.cci.sst.tool;

/**
 * Simple abstraction of a parameter.
 *
 * @author Norman Fomferra
 */
public class Parameter {
    private final String name;
    private final String argName;
    private final String defaultValue;
    private final String description;

    public Parameter(String name, String type, String defaultValue, String description) {
        this.name = name;
        this.argName = type;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getArgName() {
        return argName;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }
}
