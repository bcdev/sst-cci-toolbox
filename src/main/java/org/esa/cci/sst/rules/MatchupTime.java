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

import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.reader.ExtractDefinition;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * Sets the matchup's time.
 *
 * @author Thomas Storm
 */
@SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
class MatchupTime extends AbstractImplicitRule {

    private static final DataType DATA_TYPE = DataType.DOUBLE;
    private static final int[] SHAPE = new int[]{1};

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array array = Array.factory(DATA_TYPE, SHAPE);
        array.setDouble(0, readMatchupTime());
        return array;
    }

    private double readMatchupTime() throws RuleException {
        final Context context = getContext();
        final Reader reader = context.getReferenceObservationReader();
        final Matchup matchup = context.getMatchup();
        final String sensor = matchup.getRefObs().getSensor();
        final int recordNo = matchup.getRefObs().getRecordNo();
        final OneDimOneValue oneDimOneValue = new OneDimOneValue(recordNo);
        if ("atsr_md".equalsIgnoreCase(sensor)) {
            return read(reader, "atsr.time.julian", oneDimOneValue).getDouble(0);
        } else if ("metop".equalsIgnoreCase(sensor) || "seviri".equalsIgnoreCase(sensor)) {
            return readObservationTime(recordNo, reader, matchup.getRefObs());
        }
        return Double.NaN;
    }

    private double readObservationTime(int recordNo, Reader reader, ReferenceObservation observation) throws
                                                                                                      RuleException {
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
