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

package org.esa.cci.sst.common;

import org.esa.cci.sst.data.ReferenceObservation;
import org.postgis.PGgeometry;
import org.postgis.Point;

import java.util.Date;

/**
 * Utility class for creating immutable extract definitions.
 *
 * @author Ralf Quast
 */
public class ExtractDefinitionBuilder {

    private double lon;
    private double lat;
    private int recordNo;
    private int[] shape;

    private Date date;
    private Number fillValue;

    private int halfExtractDuration;

    public ExtractDefinitionBuilder() {
        shape = new int[]{1};
    }

    public ExtractDefinitionBuilder referenceObservation(ReferenceObservation refObs) {
        final PGgeometry refObsPoint = refObs.getPoint();
        if (refObsPoint != null) {
            final Point point = refObsPoint.getGeometry().getFirstPoint();
            lon = point.getX();
            lat = point.getY();
        } else {
            lon = Double.NaN;
            lat = Double.NaN;
        }
        date = refObs.getTime();
        return this;
    }

    public ExtractDefinitionBuilder recordNo(int recordNo) {
        this.recordNo = recordNo;
        return this;
    }

    public ExtractDefinitionBuilder shape(int[] shape) {
        this.shape = shape.clone();
        return this;
    }

    public ExtractDefinitionBuilder fillValue(Number fillValue) {
        this.fillValue = fillValue;
        return this;
    }

    public ExtractDefinitionBuilder lat(double lat) {
        this.lat = lat;
        return this;
    }

    public ExtractDefinitionBuilder lon(double lon) {
        this.lon = lon;
        return this;
    }

    public ExtractDefinitionBuilder halfExtractDuration(int halfExtractDuration) {
        this.halfExtractDuration = halfExtractDuration;
        return this;
    }

    public ExtractDefinition build() {
        final double lat = this.lat;
        final double lon = this.lon;
        final int recordNo = this.recordNo;
        final Date date = this.date;
        final Number fillValue = this.fillValue;

        final int[] shape = this.shape;
        // the first dimension in an MMD is the record dimension. Since an extract is for a single record, the shape for the first dimension is always 1.
        shape[0] = 1;

        return new ExtractDefinition() {

            @Override
            public final double getLat() {
                return lat;
            }

            @Override
            public final double getLon() {
                return lon;
            }

            @Override
            public final int getRecordNo() {
                return recordNo;
            }

            @Override
            public int[] getShape() {
                return shape;
            }

            @Override
            public final Date getDate() {
                return date;
            }

            @Override
            public int getHalfExtractDuration() {
                return halfExtractDuration;
            }

            @Override
            public Number getFillValue() {
                return fillValue;
            }
        };
    }
}
