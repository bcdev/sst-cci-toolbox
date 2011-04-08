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

package org.esa.cci.sst.reader;

import org.esa.cci.sst.Constants;
import org.esa.cci.sst.SensorType;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.util.IoUtil;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Allows reading of observations from the in-history situ data.
 *
 * @author Thomas Storm
 */
class InsituIOHandler extends NetcdfIOHandler {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public InsituIOHandler() {
        super(SensorType.HISTORY.getSensor());
    }

    @Override
    public int getNumRecords() {
        return 1;
    }

    @Override
    public InsituObservation readObservation(int recordNo) throws IOException {
        final InsituObservation observation = new InsituObservation();
        final DataFile dataFile = getDataFile();
        observation.setDatafile(dataFile);
        observation.setName(getNcFile().findGlobalAttribute("wmo_id").getStringValue());
        observation.setRecordNo(0);
        observation.setSensor(getSensorName());
        try {
            final Date startTime = parseDate("start_date");
            final Date endTime = parseDate("end_date");
            observation.setTime(IoUtil.centerTime(startTime, endTime));
            observation.setTimeRadius(IoUtil.timeRadius(startTime, endTime));
        } catch (ParseException e) {
            throw new IOException("Unable to set time.", e);
        }
        try {
            final double startLon = parseDouble("start_lon");
            final double startLat = parseDouble("start_lat");
            final double endLon = parseDouble("end_lon");
            final double endLat = parseDouble("end_lat");
            observation.setLocation(IoUtil.createLineGeometry(startLon, startLat, endLon, endLat));
        } catch (ParseException e) {
            throw new IOException("Unable to set location.", e);
        }
        return observation;
    }

    /**
     * Writes a subset of an in-situ history observation to an MMD target file. The method must be called per
     * variable and per match-up. The subset covers plus/minus 12 hours centered at the reference observation
     * time. The subset is limited by the size of the target array, which is {@link org.esa.cci.sst.Constants#INSITU_HISTORY_LENGTH}.
     *
     * @param targetFile   The target MMD file.
     * @param observation  The observation to write.
     * @param matchupIndex The target matchup index.
     * @param refPoint     Not used.
     * @param refTime      The reference time.
     *
     * @throws IOException when an IO error has occurred.
     */
    @Override
    public void write(NetcdfFileWriteable targetFile, Observation observation, String sourceVariableName,
                      String targetVariableName, int matchupIndex, PGgeometry refPoint, Date refTime) throws
                                                                                                      IOException {
        final NetcdfFile sourceFile = getNcFile();
        final Variable sourceVariable = sourceFile.findVariable(NetcdfFile.escapeName(sourceVariableName));
        final Variable targetVariable = targetFile.findVariable(NetcdfFile.escapeName(targetVariableName));

        final int[] sourceShape = sourceVariable.getShape();
        final int[] sourceOrigin = new int[sourceShape.length];
        final Variable timeVar = sourceFile.findVariable(NetcdfFile.escapeName(Constants.VARNAME_HISTORY_TIME));
        IoUtil.findTimeInterval(timeVar.read(), TimeUtil.toJulianDate(refTime), targetVariable.getShape(1),
                                sourceOrigin, sourceShape
        );

        final int[] targetOrigin = new int[sourceShape.length + 1];
        targetOrigin[0] = matchupIndex;
        final int[] targetShape = new int[sourceShape.length + 1];
        targetShape[0] = 1;
        System.arraycopy(sourceShape, 0, targetShape, 1, sourceShape.length);

        try {
            final Array array = sourceVariable.read(sourceOrigin, sourceShape);
            targetFile.write(NetcdfFile.escapeName(targetVariableName), targetOrigin, array.reshape(targetShape));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private Date parseDate(String attributeName) throws ParseException {
        return DATE_FORMAT.parse(getNcFile().findGlobalAttribute(attributeName).getStringValue());
    }

    private double parseDouble(String attributeName) throws ParseException {
        return Double.parseDouble(getNcFile().findGlobalAttribute(attributeName).getStringValue());
    }
}
