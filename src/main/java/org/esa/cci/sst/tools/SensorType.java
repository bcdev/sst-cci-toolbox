/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools;

import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.RelatedObservation;

import java.text.MessageFormat;

/**
 * Enumeration of sensor types.
 *
 * @author Ralf Quast
 * @author Thomas Storm
 */
public enum SensorType {

    ATSR_MD(ReferenceObservation.class, 0x01, "atsr_md"),
    METOP(ReferenceObservation.class, 0x02, "metop"),
    SEVIRI(ReferenceObservation.class, 0x04, "seviri"),
    AVHRR(RelatedObservation.class, 0x08, "avhrr_m01", "avhrr_m02", "avhrr_m03", "avhrr_n10", "avhrr_n11", "avhrr_n12",
          "avhrr_n14", "avhrr_n15", "avhrr_n16", "avhrr_n17", "avhrr_n18", "avhrr_n19"),
    AMSRE(RelatedObservation.class, 0x10, "amsre"),
    TMI(RelatedObservation.class, 0x20, "tmi"),
    ATSR(RelatedObservation.class, 0x40, "atsr1", "atsr2", "aatsr"),
    AAI(Observation.class, 0x80, "aai"),
    SEAICE(RelatedObservation.class, 0x0100, "seaice"),
    HISTORY(InsituObservation.class, 0x0200, "history");

    private final Class<? extends Observation> observationClass;
    private final long pattern;
    private final String[] sensors;

    private SensorType(Class<? extends Observation> observationClass, long pattern, String... sensors) {
        this.observationClass = observationClass;
        this.pattern = pattern;
        this.sensors = sensors;
    }

    public Class<? extends Observation> getObservationClass() {
        return observationClass;
    }

    public long getPattern() {
        return pattern;
    }

    public String getSensor() {
        return sensors[0];
    }

    public String[] getSensors() {
        return sensors.clone();
    }


    public String nameLowerCase() {
        // todo - eliminate usages of this method (rq-20110322)
        return getSensor();
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public static boolean isSensorType(String name) {
        final SensorType[] values = SensorType.values();
        for (final SensorType sensorType : values) {
            if (sensorType.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSensor(String name) {
        for (final String sensor : getSensors()) {
            if (sensor.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public static SensorType valueOfIgnoreCase(String name) {
        return valueOf(name.toUpperCase());
    }

    public static SensorType getSensorType(String sensorName) {
        final SensorType[] values = SensorType.values();
        for (final SensorType sensorType : values) {
            for (final String sensor : sensorType.getSensors()) {
                if (sensor.equalsIgnoreCase(sensorName)) {
                    return sensorType;
                }
            }
        }
        throw new IllegalArgumentException(MessageFormat.format("Unknown sensor ''{0}''.", sensorName));
    }
}
