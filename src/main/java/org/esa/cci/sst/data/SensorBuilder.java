package org.esa.cci.sst.data;


import com.bc.ceres.core.Assert;

/**
 * Used for building {@link Sensor}s.
 *
 * @author Ralf Quast
 */
public final class SensorBuilder {

    private static final String PACKAGE_NAME = SensorBuilder.class.getPackage().getName();

    private String name;
    private long pattern;
    private String observationType;

    public SensorBuilder() {
        setName("internal");
        setObservationType(Observation.class.getSimpleName());
    }

    public String getName() {
        return name;
    }

    public SensorBuilder setName(String name) {
        Assert.argument(name != null, "name == null");
        this.name = name;
        return this;
    }

    public long getPattern() {
        return pattern;
    }

    public SensorBuilder setPattern(long pattern) {
        this.pattern = pattern;
        return this;
    }

    public String getObservationType() {
        return observationType;
    }

    @SuppressWarnings({"UnusedDeclaration", "UnusedAssignment", "unchecked"})
    public SensorBuilder setObservationType(String observationType) {
        Assert.argument(observationType != null, "observationType == null");
        final Class<? extends Observation> observationClass;
        try {
            observationClass = (Class<? extends Observation>) Class.forName(PACKAGE_NAME + "." + observationType);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        this.observationType = observationType;
        return this;
    }

    public Sensor build() {
        return new Sensor(this);
    }
}
