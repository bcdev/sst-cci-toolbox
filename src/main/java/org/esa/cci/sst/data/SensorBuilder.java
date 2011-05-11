/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
        name("unknown");
        observationType(Observation.class.getSimpleName());
    }

    public SensorBuilder name(String name) {
        Assert.argument(name != null, "name == null");
        this.name = name;
        return this;
    }

    public SensorBuilder pattern(long pattern) {
        this.pattern = pattern;
        return this;
    }

    public SensorBuilder observationType(Class<? extends Observation> observationType) {
        Assert.argument(observationType != null, "observationType == null");
        //noinspection ConstantConditions
        this.observationType = observationType.getSimpleName();
        return this;
    }

    public SensorBuilder observationType(String observationType) {
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
        return observationType(observationClass);
    }

    @SuppressWarnings({"deprecation"})
    public Sensor build() {
        final Sensor sensor = new Sensor();
        sensor.setName(name);
        sensor.setPattern(pattern);
        sensor.setObservationType(observationType);

        return sensor;
    }
}
