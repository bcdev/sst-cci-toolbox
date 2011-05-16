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

import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Entity;

/**
 * Data item that represents a time series of in-situ observations.
 *
 * @author Thomas Storm
 */
@Entity
public class InsituObservation extends RelatedObservation {

    // important: double precision is used to preserve precision
    private double timeRadius;

    /**
     * Returns the time radius (seconds) of this observation.
     *
     * @return the time radius (seconds).
     */
    public double getTimeRadius() {
        return timeRadius;
    }

    /**
     * Sets the time radius (seconds) of this observation.
     *
     * @param timeRadius The time radius (seconds).
     */
    public void setTimeRadius(double timeRadius) {
        this.timeRadius = timeRadius;
    }

    @Override
    public String toString() {
        return String.format("InsituObservation(%d,%s,%s,%s,%f,%s,%d)", getId(), getCallsign(), getSensor(),
                             TimeUtil.formatCcsdsUtcFormat(getTime()), getTimeRadius(), getDatafile(), getRecordNo());
    }
}
