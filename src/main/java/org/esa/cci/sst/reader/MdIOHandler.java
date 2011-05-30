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

package org.esa.cci.sst.reader;

import org.esa.beam.util.PixelLocator;
import org.esa.beam.util.QuadTreePixelLocator;
import org.esa.beam.util.SampleSource;
import org.esa.beam.util.VariableSampleSource;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.postgis.PGgeometry;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.awt.Point;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Reads records from a NetCDF input file and creates Observations. This abstract
 * reader buffers records in tiles, i.e. it reads several records in a single
 * read when one record of a certain range is accessed. The reader further
 * is configured in the concrete implementation class by the names of variables
 * to read. The buffer is organised as a map of variables, each having an array
 * of at least one dimension as value. The first dimension of the array specifies
 * the record.
 *
 * @author Martin Boettcher
 */
abstract class MdIOHandler extends NetcdfIOHandler {

    private final Map<String, Array> arrayMap = new HashMap<String, Array>();
    private final Map<String, Integer> indexMap = new HashMap<String, Integer>();

    private int numRecords;

    protected MdIOHandler(String sensorName) {
        super(sensorName);
    }

    @Override
    public void init(DataFile datafile) throws IOException {
        super.init(datafile);

        //noinspection LoopStatementThatDoesntLoop
        for (final Variable variable : getVariables()) {
            // the record dimension is the first dimension of all record variables
            final Dimension d = variable.getDimension(0);
            numRecords = d.getLength();
            break;
        }
    }

    @Override
    public void close() {
        numRecords = 0;
        indexMap.clear();
        arrayMap.clear();
        super.close();
    }

    @Override
    public final Array read(String role, ExtractDefinition extractDefinition) throws IOException {
        final Variable variable = getVariable(role);
        final Array targetArray = Array.factory(variable.getDataType(), extractDefinition.getShape());
        final int recordNo = extractDefinition.getRecordNo();
        if (variable.getRank() < 3) {
            final Array sourceArray = getData(variable, recordNo);
            for (int i = 0; i < sourceArray.getSize(); i++) {
                targetArray.setObject(i, sourceArray.getObject(i));
            }
            return targetArray;
        }
        final Variable lon = getVariable("lon");
        final Variable lat = getVariable("lat");
        final Array lonArray = getData(lon, recordNo);
        final Array latArray = getData(lat, recordNo);
        final SampleSource lonSource = new VariableSampleSource(lon, lonArray);
        final SampleSource latSource = new VariableSampleSource(lat, latArray);
        final PixelLocator pixelLocator = new QuadTreePixelLocator(lonSource, latSource);
        final Point p = new Point();
        final boolean success = pixelLocator.getPixelLocation(extractDefinition.getLon(),
                                                              extractDefinition.getLat(), p);
        final Number fillValue = getAttribute(variable, "_FillValue", Double.NEGATIVE_INFINITY);
        if (success) {
            final Array sourceArray = getData(variable, recordNo);
            extractSubscene(sourceArray, targetArray, p, fillValue);
        } else {
            final Logger logger = Logger.getLogger("org.esa.cci.sst");
            final String message = MessageFormat.format(
                    "Unable to find pixel at ({0}, {1}) for record {2} in file ''{3}''.",
                    extractDefinition.getLon(),
                    extractDefinition.getLat(),
                    recordNo,
                    getNetcdfFile().getLocation());
            logger.fine(message);
            for (int i = 0; i < targetArray.getSize(); i++) {
                targetArray.setObject(i, fillValue);
            }
        }
        return targetArray;
    }

    @Deprecated
    @Override
    public void write(NetcdfFileWriteable targetFile, Observation observation, String sourceVarName,
                      String targetVarName, int matchupIndex, final PGgeometry refPoint, final Date refTime) throws
                                                                                                             IOException {
        final Variable targetVariable = targetFile.findVariable(NetcdfFile.escapeName(targetVarName));
        final int[] origin = new int[targetVariable.getRank()];
        origin[0] = matchupIndex;

        try {
            final Array variableData = getData(getVariable(sourceVarName), observation.getRecordNo());
            targetFile.write(NetcdfFile.escapeName(targetVarName), origin, variableData);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
    }

    @Override
    public final int getNumRecords() {
        return numRecords;
    }

    /**
     * Reads a record value of a variable of rank 2 and type char.
     *
     * @param role     The variable name.
     * @param recordNo The record index in range 0 .. numRecords-1.
     *
     * @return the record value as string.
     *
     * @throws IOException if the file IO operation fails.
     */
    public String getString(String role, int recordNo) throws IOException {
        final Variable variable = validateArguments(role, 2, DataType.CHAR, recordNo);
        final Array array = getData(variable, recordNo);
        return ((ArrayChar.D2) array).getString(0).trim();
    }

    /**
     * Reads a record value of a variable of rank 1 and type float.
     *
     * @param role     The variable name.
     * @param recordNo The record index in range 0 .. numRecords-1.
     *
     * @return the record value.
     *
     * @throws IOException if the file IO operation fails.
     */
    public float getFloat(String role, int recordNo) throws IOException {
        final Variable variable = validateArguments(role, 1, DataType.FLOAT, recordNo);
        final Array array = getData(variable, recordNo);
        return array.getFloat(0);
    }

    /**
     * Reads a record value of a variable of rank 1 and type double.
     *
     * @param role     The variable name.
     * @param recordNo The record index in range 0 .. numRecords-1.
     *
     * @return the record value.
     *
     * @throws IOException if the file IO operation fails.
     */
    public double getDouble(String role, int recordNo) throws IOException {
        final Variable variable = validateArguments(role, 1, DataType.DOUBLE, recordNo);
        final Array array = getData(variable, recordNo);
        return array.getDouble(0);
    }

    /**
     * Reads a record value of a variable of rank 1 and type byte.
     *
     * @param role     The variable name.
     * @param recordNo The record index in range 0 .. numRecords-1.
     *
     * @return the record value.
     *
     * @throws IOException if the file IO operation fails.
     */
    public byte getByte(String role, int recordNo) throws IOException {
        final Variable variable = validateArguments(role, 1, DataType.BYTE, recordNo);
        final Array array = getData(variable, recordNo);
        return array.getByte(0);
    }

    /**
     * Reads a record value of a variable of rank 1 and type int.
     *
     * @param role     The variable name.
     * @param recordNo The record index in range 0 .. numRecords-1.
     *
     * @return the record value.
     *
     * @throws IOException if the file IO operation fails.
     */
    public int getInt(String role, int recordNo) throws IOException {
        final Variable variable = validateArguments(role, 1, DataType.INT, recordNo);
        final Array array = getData(variable, recordNo);
        return array.getInt(0);
    }

    /**
     * Reads a record value of a variable of rank 1 and type short.
     *
     * @param role     The variable name.
     * @param recordNo The record index in range 0 .. numRecords-1.
     *
     * @return the record value.
     *
     * @throws IOException if the file IO operation fails.
     */
    public short getShort(String role, int recordNo) throws IOException {
        final Variable variable = validateArguments(role, 1, DataType.SHORT, recordNo);
        final Array array = getData(variable, recordNo);
        return array.getShort(0);
    }

    /**
     * Reads a record value of a variable of rank 1 and numerical type.
     *
     * @param role     The variable name.
     * @param recordNo The record index in range 0 .. numRecords-1.
     *
     * @return the record value.
     *
     * @throws IOException if the file IO operation fails.
     */
    public Number getNumber(String role, int recordNo) throws IOException {
        final Variable variable = validateArguments(role, 1, null, recordNo);
        final Array array = getData(variable, recordNo);
        return (Number) array.getObject(0);
    }

    /**
     * Reads a record value of a variable of rank 1 and numerical type.
     *
     * @param role     The variable name.
     * @param recordNo The record index in range 0 .. numRecords-1.
     *
     * @return the properly scaled record value.
     *
     * @throws IOException if the file IO operation fails.
     */
    public Number getNumberScaled(String role, int recordNo) throws IOException {
        final Variable variable = validateArguments(role, 1, null, recordNo);
        return getNumberScaled(variable, getNumber(role, recordNo));
    }

    /**
     * Reads single pixel value from record of sub-scenes contained in 3D short array.
     *
     * @param role     The variable name.
     * @param recordNo The record index in range 0 .. numRecords-1.
     * @param y        pixel line position in sub-scene.
     * @param x        pixel column position in sub-scene.
     *
     * @return the pixel value.
     *
     * @throws IOException if file IO fails.
     */
    public int getShort(String role, int recordNo, int y, int x) throws IOException {
        final Variable variable = validateArguments(role, 3, DataType.SHORT, recordNo);
        final Array array = getData(variable, recordNo);
        return ((ArrayShort.D3) array).get(0, y, x);
    }

    /**
     * Reads single pixel value from record of sub-scenes contained in 3D int array.
     *
     * @param role     The variable name.
     * @param recordNo The record index in range 0 .. numRecords-1.
     * @param y        pixel line position in sub-scene.
     * @param x        pixel column position in sub-scene.
     *
     * @return the pixel value.
     *
     * @throws IOException if file IO fails.
     */
    public int getInt(String role, int recordNo, int y, int x) throws IOException {
        final Variable variable = validateArguments(role, 3, DataType.INT, recordNo);
        final Array array = getData(variable, recordNo);
        return ((ArrayInt.D3) array).get(0, y, x);
    }

    /**
     * Reads single pixel value from record of sub-scenes contained in 3D double array.
     *
     * @param role     The variable name.
     * @param recordNo The record index in range 0 .. numRecords-1.
     * @param y        pixel line position in sub-scene.
     * @param x        pixel column position in sub-scene.
     *
     * @return the pixel value.
     *
     * @throws IOException if file IO fails.
     */
    public double getDouble(String role, int recordNo, int y, int x) throws IOException {
        final Variable variable = validateArguments(role, 3, DataType.DOUBLE, recordNo);
        final Array array = getData(variable, recordNo);
        return ((ArrayDouble.D3) array).get(0, y, x);
    }

    /**
     * Reads single pixel value from record of sub-scenes contained in 2D double array.
     *
     * @param role     The variable name.
     * @param recordNo The record index in range 0 .. numRecords-1.
     * @param y        pixel line position in sub-scene.
     *
     * @return the pixel value.
     *
     * @throws IOException if file IO fails.
     */
    public double getDouble(String role, int recordNo, int y) throws IOException {
        final Variable variable = validateArguments(role, 2, DataType.DOUBLE, recordNo);
        final Array array = getData(variable, recordNo);
        return ((ArrayDouble.D2) array).get(0, y);
    }

    private Variable validateArguments(String role, int expectedRank, DataType expectedDataType, int recordNo) {
        final Variable variable = getVariable(role);
        if (variable == null) {
            throw new IllegalArgumentException(
                    MessageFormat.format("Unknown variable ''{0}''.", role));
        }
        if (recordNo >= variable.getShape(0)) {
            throw new IllegalArgumentException("Illegal record number.");
        }
        final int actualRank = variable.getRank();
        if (actualRank != expectedRank) {
            throw new IllegalArgumentException(
                    MessageFormat.format("Expected variable of rank {0}. Rank of variable ''{1}'' is {2}",
                                         expectedRank, role, actualRank));
        }
        final DataType actualDataType = variable.getDataType();
        if (expectedDataType == null) {
            if (!actualDataType.isNumeric()) {
                throw new IllegalArgumentException(
                        MessageFormat.format("Expected numeric data type. Type of variable ''{0}'' is {1}",
                                             role, actualDataType));
            }
        } else {
            if (actualDataType != expectedDataType) {
                throw new IllegalArgumentException(
                        MessageFormat.format("Expected variable of type {0}. Type of variable ''{1}'' is {2}",
                                             expectedDataType, role, actualDataType));
            }
        }
        return variable;
    }

    private Array getData(Variable variable, int recordNo) throws IOException {
        final String role = variable.getName();
        final Array cachedArray = arrayMap.get(role);
        if (cachedArray != null) {
            final Integer cachedRecord = indexMap.get(role);
            if (cachedRecord != null && cachedRecord == recordNo) {
                return cachedArray;
            }
        }
        final int[] shape = variable.getShape();
        shape[0] = 1;
        final int[] start = new int[shape.length];
        start[0] = recordNo;
        try {
            final Array array = variable.read(start, shape);
            arrayMap.put(role, array);
            indexMap.put(role, recordNo);
            return array;
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
    }

    private static Number getNumberScaled(Variable variable, Number number) throws IOException {
        final double scaleFactor = getAttribute(variable, "scale_factor", 1.0).doubleValue();
        final double addOffset = getAttribute(variable, "add_offset", 0.0).doubleValue();

        return scaleFactor * number.doubleValue() + addOffset;
    }

    static void extractSubscene(Array source, Array target, Point p, Number fillValue) {
        final int[] sourceShape = source.getShape();
        final int[] targetShape = target.getShape();
        final int ssx = sourceShape[source.getRank() - 1];
        final int ssy = sourceShape[source.getRank() - 2];
        final int tsx = targetShape[target.getRank() - 1];
        final int tsy = targetShape[target.getRank() - 2];
        final int cx = tsx / 2;
        final int cy = tsy / 2;

        final Index si = source.getIndex();
        final Index ti = target.getIndex();
        for (int ty = 0; ty < tsy; ty++) {
            ti.setDim(target.getRank() - 2, ty);
            for (int tx = 0; tx < tsx; tx++) {
                ti.setDim(target.getRank() - 1, tx);
                final int sx = tx - (cx - p.x);
                final int sy = ty - (cy - p.y);
                if (sx >= 0 && sy >= 0 && sx < ssx && sy < ssy) {
                    si.setDim(source.getRank() - 1, sx);
                    si.setDim(source.getRank() - 2, sy);
                    target.setObject(ti, source.getObject(si));
                } else {
                    target.setObject(ti, fillValue);
                }
            }
        }
    }
}