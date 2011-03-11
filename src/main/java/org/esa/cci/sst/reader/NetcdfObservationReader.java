package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.postgis.PGgeometry;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
abstract public class NetcdfObservationReader extends NetcdfObservationStructureReader {

    private int numRecords;
    private int sstFillValue;
    private Map<String, Array> data = new HashMap<String, Array>();
    private int bufferStart = 0;
    private int bufferFill = 0;
    private String recordDimensionName;
    private Map<String, Integer> offsetMap = new HashMap<String, Integer>();
    private Map<String, Integer> bufferMap = new HashMap<String, Integer>();

    protected NetcdfObservationReader(String sensorName, String recordDimensionName) {
        super(sensorName);
        this.recordDimensionName = recordDimensionName;
    }

    @Override
    public void init(DataFile dataFileEntry) throws IOException {
        super.init(dataFileEntry);
        // read number of records value
        final NetcdfFile ncFile = getNcFile();
        final Dimension dimension = ncFile.findDimension(recordDimensionName);
        if (dimension == null) {
            throw new IOException(MessageFormat.format("Can''t find dimension ''{0}'' in file {1}", recordDimensionName,
                                                       dataFileEntry.getPath()));
        }
        numRecords = dimension.getLength();
        // read SST fill value
        final ucar.nc2.Variable variable = ncFile.findVariable(getSstVariableName().replaceAll("\\.", "%2e"));
        // todo - only used to compute clearsky condition, generalise clearsky flag computation?!
        sstFillValue = variable.findAttributeIgnoreCase("_fillvalue").getNumericValue().intValue();
    }

    /**
     * Number of records of file. initialised in init() when opening the NetCDF file.
     *
     * @return number of records in NetCDF file
     */
    @Override
    public int getNumRecords() {
        return numRecords;
    }

    @Override
    public void write(Observation observation, org.esa.cci.sst.data.Variable variable, NetcdfFileWriteable file,
                      int matchupIndex, int[] dimensionSizes, final PGgeometry point) throws IOException {
        String sensorName = observation.getSensor();
        String originalVarName = variable.getName();
        String variableName = originalVarName.replace(sensorName + ".", "");
        final int[] origin = ReaderUtils.createOriginArray(matchupIndex, variable);
        try {
            Array variableData = getData(variableName, matchupIndex);
            file.write(NetcdfFile.escapeName(originalVarName), origin, variableData);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
    }

    /**
     * Reads record value contained in 2D char array
     *
     * @param role     variable name
     * @param recordNo record index in range 0 .. numRecords-1
     *
     * @return record value as String
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1 or if file io fails
     */
    public String getString(String role, int recordNo) throws IOException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayChar.D2) variableData).getString(recordNo - offset);
    }

    /**
     * Reads record value contained in float array
     *
     * @param role     variable name
     * @param recordNo record index in range 0 .. numRecords-1
     *
     * @return record value as float
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1 or if file io fails
     */
    public float getFloat(String role, int recordNo) throws IOException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayFloat.D1) variableData).get(recordNo - offset);
    }

    /**
     * Reads record value contained in double array
     *
     * @param role     variable name
     * @param recordNo record index in range 0 .. numRecords-1
     *
     * @return record value as double
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1 or if file io fails
     */
    public double getDouble(String role, int recordNo) throws IOException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayDouble.D1) variableData).get(recordNo - offset);
    }

    /**
     * Reads record value contained in int array
     *
     * @param role     variable name
     * @param recordNo record index in range 0 .. numRecords-1
     *
     * @return record value as int
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1 or if file io fails
     */
    public int getInt(String role, int recordNo) throws IOException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayInt.D1) variableData).get(recordNo - offset);
    }

    /**
     * Reads record value contained in short array
     *
     * @param role     variable name
     * @param recordNo record index in range 0 .. numRecords-1
     *
     * @return record value as int
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1 or if file io fails
     */
    public int getShort(String role, int recordNo) throws IOException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayShort.D1) variableData).get(recordNo - offset);
    }

    /**
     * Reads single pixel value from record of sub-scenes contained in 3D short array
     *
     * @param role     variable name
     * @param recordNo record index in range 0 .. numRecords-1
     * @param line     pixel line position in sub-scene
     * @param column   pixel column position in sub-scene
     *
     * @return pixel value as int
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1
     *                     or line and column are out of range or if file io fails
     */
    public int getShort(String role, int recordNo, int line, int column) throws IOException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayShort.D3) variableData).get(recordNo - offset, line, column);
    }

    /**
     * Reads single pixel value from record of sub-scenes contained in 3D int array
     *
     * @param role     variable name
     * @param recordNo record index in range 0 .. numRecords-1
     * @param line     pixel line position in sub-scene
     * @param column   pixel column position in sub-scene
     *
     * @return pixel value as int
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1
     *                     or line and column are out of range or if file io fails
     */
    public int getInt(String role, int recordNo, int line, int column) throws IOException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayInt.D3) variableData).get(recordNo - offset, line, column);
    }

    /**
     * Reads single pixel value from record of sub-scenes contained in 3D double array
     *
     * @param role     variable name
     * @param recordNo record index in range 0 .. numRecords-1
     * @param line     pixel line position in sub-scene
     * @param column   pixel column position in sub-scene
     *
     * @return pixel value as double
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1
     *                     or line and column are out of range or if file io fails
     */
    public double getDouble(String role, int recordNo, int line, int column) throws IOException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayDouble.D3) variableData).get(recordNo - offset, line, column);
    }

    /**
     * Reads single pixel value from record of sub-scenes contained in 2D double array
     *
     * @param role     variable name
     * @param recordNo record index in range 0 .. numRecords-1
     * @param line     pixel line position in sub-scene
     *
     * @return pixel value as double
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1
     *                     or if line and column are out of range or if file io fails
     */
    public double getDouble(String role, int recordNo, int line) throws IOException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayDouble.D2) variableData).get(recordNo - offset, line);
    }

    /**
     * Ensures that identified record is in the data buffer, maybe reads tile to achieve this.
     *
     * @param varName  The name of the variable to be read.
     * @param recordNo Index of record to be in the data buffer.
     *
     * @return Index of first record in buffer, to be used as offset of record
     *         number when accessing data buffer
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1 or if file io fails
     */
    int fetch(String varName, int recordNo) throws IOException {
        int bufferStart = 0;
        int bufferFill = 0;
        if (offsetMap.get(varName) != null) {
            bufferStart = offsetMap.get(varName);
            bufferFill = bufferMap.get(varName);
        }
        if (recordNo < bufferStart || recordNo >= bufferStart + bufferFill) {
            final Variable variable = getNcFile().findVariable(NetcdfFile.escapeName(varName));
            final int[] shape = variable.getShape();
            final int[] start = new int[shape.length];
            start[0] = recordNo;
            shape[0] = shape[0] - recordNo;
            ensureBounds(variable, shape);

            final Array values;
            try {
                values = variable.read(start, shape);
            } catch (InvalidRangeException e) {
                throw new IOException(e);
            }
            data.put(variable.getNameEscaped(), values);
            bufferStart = recordNo;
            bufferFill = shape[0];
            offsetMap.put(varName, bufferStart);
            bufferMap.put(varName, bufferFill);
        }
        return bufferStart;
    }

    private void ensureBounds(final Variable variable, final int[] shape) {
        for (int i = 0; i < shape.length; i++) {
            final int dimLength = variable.getDimension(i).getLength();
            if (shape[i] > dimLength) {
                shape[i] = dimLength;
            }
        }
    }

    /**
     * Ensures that identified record is in the data buffer, maybe reads tile to achieve this.
     *
     * @param recordNo Index of record to be in the data buffer.
     *
     * @return Index of first record in buffer, to be used as offset of record
     *         number when accessing data buffer
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1 or if file io fails
     */
    int fetch(int recordNo) throws IOException {
        if (recordNo < bufferStart || recordNo >= bufferStart + bufferFill) {
            int tileSize = 1024;  // TODO adjust default, read from property
            for (Variable variable : getNcFile().getVariables()) {
                final int[] shape = variable.getShape();
                final int[] start = new int[shape.length];
                start[0] = recordNo;
                shape[0] = (shape[0] < recordNo + tileSize)
                           ? shape[0] - recordNo
                           : tileSize;
                final Array values;
                try {
                    values = variable.read(start, shape);
                } catch (InvalidRangeException e) {
                    throw new IOException(e);
                }
                data.put(variable.getNameEscaped(), values);
            }
            bufferStart = recordNo;
            bufferFill = tileSize;
        }
        return bufferStart;
    }

    Array getData(String variableName, int recordNo) throws IOException {
        final Variable variable = getNcFile().findVariable(NetcdfFile.escapeName(variableName));
        if (recordNo >= variable.getShape()[0]) {
            return getFilledArray(variable);
        }
        fetch(variableName, recordNo);
        final int offset = offsetMap.get(variableName);
        final int index = recordNo - offset;
        final Array slice = data.get(NetcdfFile.escapeName(variableName)).slice(0, index);
        final int[] shape1 = slice.getShape();
        final int[] shape2 = new int[shape1.length + 1];
        shape2[0] = 1;
        System.arraycopy(shape1, 0, shape2, 1, shape1.length);
        return slice.reshape(shape2);
    }

    // todo - ts - verify
    private Array getFilledArray(final Variable variable) {
        final List<Dimension> dimensions = variable.getDimensions();
        final int dimCount = dimensions.size();
        final int[] shape = new int[dimCount];
        shape[0] = 1;
        for (int i = 1; i < dimCount; i++) {
            shape[i] = dimensions.get(i).getLength();
        }
        Number fillValue = null;
        final Attribute fillValueAttribute = variable.findAttribute("_FillValue");
        if (fillValueAttribute != null) {
            fillValue = fillValueAttribute.getNumericValue();
        }
        int size = 1;
        for (int i : shape) {
            size *= i;
        }

        final Array array = Array.factory(variable.getDataType(), shape);
        for (int i = 0; i < size; i++) {
            if (variable.getDataType().getPrimitiveClassType() == byte.class) {
                if (fillValue == null) {
                    array.setByte(i, (byte) -1);
                } else {
                    array.setByte(i, fillValue.byteValue());
                }
            } else if (variable.getDataType().getPrimitiveClassType() == short.class) {
                if (fillValue == null) {
                    array.setShort(i, (short) -1);
                } else {
                    array.setShort(i, fillValue.shortValue());
                }
            } else if (variable.getDataType().getPrimitiveClassType() == int.class) {
                if (fillValue == null) {
                    array.setInt(i, (short) -1);
                } else {
                    array.setInt(i, fillValue.intValue());
                }
            } else if (variable.getDataType().getPrimitiveClassType() == float.class) {
                if (fillValue == null) {
                    array.setFloat(i, (float) -1);
                } else {
                    array.setFloat(i, fillValue.floatValue());
                }
            } else if (variable.getDataType().getPrimitiveClassType() == double.class) {
                if (fillValue == null) {
                    array.setDouble(i, (double) -1);
                } else {
                    array.setDouble(i, fillValue.doubleValue());
                }
            } else if (variable.getDataType().getPrimitiveClassType() == long.class) {
                if (fillValue == null) {
                    array.setLong(i, (long) -1);
                } else {
                    array.setLong(i, fillValue.longValue());
                }
            }
        }
        return array;
    }

    protected int getSstFillValue() {
        return sstFillValue;
    }

    /**
     * Constant name of variable to read the sst value from
     *
     * @return variable name
     */
    abstract protected String getSstVariableName();

}