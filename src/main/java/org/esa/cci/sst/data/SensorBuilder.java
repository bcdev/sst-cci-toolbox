package org.esa.cci.sst.data;


import com.bc.ceres.core.Assert;

/**
 * Public API for building immutable {@link Sensor} instances.
 *
 * @author Ralf Quast
 */
public final class SensorBuilder {

    private static final String PACKAGE_NAME = SensorBuilder.class.getPackage().getName();

    private String name;
    private long pattern;
    private String observationType;

    public SensorBuilder() {
        setName("untitled");
        setObservationType(Observation.class.getSimpleName());
    }

    public SensorBuilder setName(String name) {
        Assert.argument(name != null, "name == null");
        this.name = name;
        return this;
    }

    public SensorBuilder setPattern(long pattern) {
        this.pattern = pattern;
        return this;
    }

    public SensorBuilder setObservationType(Class<? extends Observation> observationType) {
        Assert.argument(observationType != null, "observationType == null");
        //noinspection ConstantConditions
        this.observationType = observationType.getSimpleName();
        return this;
    }

    public SensorBuilder setObservationType(String observationType) {
        Assert.argument(observationType != null, "observationType == null");
        final Class<? extends Observation> observationClass;
        try {
            //noinspection unchecked
            observationClass = (Class<? extends Observation>) Class.forName(PACKAGE_NAME + "." + observationType);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        return setObservationType(observationClass);
    }

    public Sensor build() {
        final Sensor sensor = new Sensor();
        sensor.setName(name);
        sensor.setPattern(pattern);
        sensor.setObservationType(observationType);

        return sensor;
    }
}
