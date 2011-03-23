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

package org.esa.cci.sst.data;

import org.esa.cci.sst.util.TimeUtil;

import javax.persistence.Entity;

/**
 * Data item that represents a single observation with a non-fixed position,
 * either with an SST value or a sub-scene or a related value for aerosol,
 * sea ice, etc. The object refers to a record in an MD file or a record in a related file.
 *
 * @author Thomas Storm
 */
@Entity
public class InsituObservation extends Observation {

    long timeRadius;

    public long getTimeRadius() {
        return timeRadius;
    }

    public void setTimeRadius(long timeRadius) {
        this.timeRadius = timeRadius;
    }

    public String toString() {
        return String.format("Observation(%d,%s,%s,%s,%d,%s,%d)", getId(), getName(), getSensor(),
                             TimeUtil.formatCcsdsUtcFormat(getTime()), getTimeRadius(), getDatafile(), getRecordNo());
    }
}
