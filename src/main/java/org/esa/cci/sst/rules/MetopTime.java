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

package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.reader.ExtractDefinition;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * Sets metop.time.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
class MetopTime extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.DOUBLE;

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) throws RuleException {
        targetColumnBuilder.type(DATA_TYPE).unit(Constants.UNIT_TIME);
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array array = Array.factory(DATA_TYPE, new int[]{1});
        array.setDouble(0, readMetopTime());
        return array;
    }

    private double readMetopTime() throws RuleException {
        final Coincidence coincidence = getContext().getCoincidence();
        if (coincidence == null || !coincidence.getObservation().getSensor().equalsIgnoreCase("metop")) {
            return Double.NaN;
        }
        final ReferenceObservation observation = (ReferenceObservation) coincidence.getObservation();
        final Reader reader = getContext().getCoincidenceReader();
        return readObservationTime(getContext().getMatchup().getRefObs().getRecordNo(), reader, observation);
    }

    private double readObservationTime(int recordNo, Reader reader, ReferenceObservation observation) throws RuleException {
        final OneDimOneValue oneDimOneValue = new OneDimOneValue(recordNo);
        final double msrTime = read(reader, "msr_time", oneDimOneValue).getDouble(0);
        final Point point = observation.getPoint().getGeometry().getFirstPoint();
        final double lon = point.getX();
        final double lat = point.getY();
        final double dtime = read(reader, "dtime", new TwoDimsOneValue(recordNo, lon, lat)).getDouble(0);
        final double julianMsrTime = TimeUtil.julianDateToSecondsSinceEpoch(msrTime);
        return julianMsrTime + dtime;
    }

    private Array read(Reader reader, String variableName, ExtractDefinition extractDefinition) throws RuleException {
        Array value;
        try {
            value = reader.read(variableName, extractDefinition);
        } catch (IOException e) {
            throw new RuleException(MessageFormat.format("Unable to read from variable ''{0}''.", variableName), e);
        }
        return value;
    }

}
