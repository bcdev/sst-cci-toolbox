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

import org.apache.openjpa.persistence.jdbc.Index;
import org.apache.openjpa.persistence.jdbc.Strategy;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * Data item that represents a single observation that is associated with
 * a location and time of observation.
 *
 * @author Martin Boettcher
 */
@Entity
public class RelatedObservation extends Observation implements Timeable {

    private Date time;
    private PGgeometry location;

    @Column(columnDefinition = "GEOGRAPHY(GEOMETRY,4326)")
    @Strategy("org.esa.cci.sst.orm.GeographyValueHandler")
    public PGgeometry getLocation() {
        return location;
    }

    @Index
    @Temporal(TemporalType.TIMESTAMP)
    @Override
    public Date getTime() {
        return time;
    }

    @Override
    public void setTime(Date time) {
        this.time = time;
    }

    public void setLocation(PGgeometry location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return String.format("RelatedObservation(%d,%s,%s,%s,%s,%s,%d)", getId(), getName(), getSensor(),
                             TimeUtil.formatCcsdsUtcFormat(getTime()), getLocation(), getDatafile(), getRecordNo());
    }
}

