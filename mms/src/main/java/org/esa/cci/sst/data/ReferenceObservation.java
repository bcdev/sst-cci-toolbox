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

import org.apache.openjpa.persistence.jdbc.Strategy;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Data item that represents a single observation that refers
 * to a record in an MD file.
 *
 * @author Martin Boettcher
 */
@Entity
public class ReferenceObservation extends RelatedObservation {

    private PGgeometry point;
    private byte dataset;
    private byte referenceFlag;

    @Column(columnDefinition = "GEOGRAPHY(POINT,4326)")
    @Strategy("org.esa.cci.sst.orm.PointValueHandler")
    public PGgeometry getPoint() {
        return point;
    }

    public void setPoint(PGgeometry point) {
        this.point = point;
    }

    public byte getDataset() {
        return dataset;
    }

    public void setDataset(byte dataset) {
        this.dataset = dataset;
    }


    public byte getReferenceFlag() {
        return referenceFlag;
    }

    public void setReferenceFlag(byte referenceFlag) {
        this.referenceFlag = referenceFlag;
    }

    @Override
    public String toString() {
        return String.format("ReferenceObservation(%d,%s,%s,%s,%s,%d,%s,%s,%f,%d,%d)", getId(), getSensor(),
                             TimeUtil.formatCcsdsUtcFormat(getTime()), getLocation(), getDatafile(), getRecordNo(),
                             getPoint(), getName(), getTimeRadius(), getDataset(), getReferenceFlag());
    }
}

