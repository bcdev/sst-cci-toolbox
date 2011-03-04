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

package org.esa.cci.sst;

/**
 * Enum containing sensor names.
 *
 * @author Thomas Storm
 */
public enum SensorName {

    SENSOR_NAME_AATSR_MD("aatsr-md"),
    SENSOR_NAME_AATSR("aatsr-md"),
    SENSOR_NAME_METOP("metop"),
    SENSOR_NAME_SEVIRI("seviri"),
    SENSOR_NAME_AVHRR("avhrr"),
    SENSOR_NAME_AMSRE("amsre"),
    SENSOR_NAME_TMI("tmi"),
    SENSOR_NAME_AAI("aai"),
    SENSOR_NAME_SEA_ICE("seaice"),
    SENSOR_NAME_INSITU("drifter");

    private final String sensor;

    private SensorName(String sensor) {
        this.sensor = sensor;
    }

    /**
     * Returns the sensor name.
     *
     * @return the sensor name.
     */
    public String getSensor() {
        return sensor;
    }
}
