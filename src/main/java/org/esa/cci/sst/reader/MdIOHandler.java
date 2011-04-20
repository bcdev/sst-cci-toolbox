package org.esa.cci.sst.reader;

import com.bc.ceres.core.Assert;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.postgis.PGgeometry;
import ucar.ma2.Array;
import ucar.ma2.ArrayByte;
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
import ucar.nc2.VariableIF;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
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
public abstract class MdIOHandler extends NetcdfIOHandler {

    private final String recordDimensionName;
    private final Map<String, Array> data = new HashMap<String, Array>();
    private final Map<String, Integer> offsetMap = new HashMap<String, Integer>();
    private final Map<String, Integer> bufferMap = new HashMap<String, Integer>();

    private int numRecords;
    private int sstFillValue;
    private int bufferStart = 0;
    private int bufferFill = 0;

    protected MdIOHandler(String sensorName, String recordDimensionName) {
        super(sensorName);
        this.recordDimensionName = recordDimensionName;
    }

    @Override
    public void init(DataFile dataFile) throws IOException {
        super.init(dataFile);
        // read number of records value
        final NetcdfFile ncFile = getNcFile();
        final Dimension dimension = ncFile.findDimension(recordDimensionName);
        if (dimension == null) {
            throw new IOException(MessageFormat.format("Can''t find dimension ''{0}'' in file {1}", recordDimensionName,
                                                       dataFile.getPath()));
        }
        numRecords = dimension.getLength();
        // read SST fill value
        final Variable variable = ncFile.findVariable(NetcdfFile.escapeName(getSstVariableName()));
        if (variable != null) {
            final Attribute attribute = variable.findAttribute("_FillValue");
            if (attribute != null) {
                sstFillValue = attribute.getNumericValue().intValue();
            }
        }
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
    public void close() {
        bufferMap.clear();
        offsetMap.clear();
        data.clear();
        super.close();
    }

    @Override
    public void write(NetcdfFileWriteable targetFile, Observation observation, String sourceVarName,
                      String targetVarName, int matchupIndex, final PGgeometry refPoint, final Date refTime) throws
                                                                                                             IOException {
        final Variable targetVariable = targetFile.findVariable(NetcdfFile.escapeName(targetVarName));
        final int[] origin = new int[targetVariable.getRank()];
        origin[0] = matchupIndex;

        try {
            final Array variableData = getData(sourceVarName, observation.getRecordNo());
            targetFile.write(NetcdfFile.escapeName(targetVarName), origin, variableData);
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
        final int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayChar.D2) variableData).getString(recordNo - offset).trim();
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
        final int offset = fetch(recordNo);
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
        final int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayDouble.D1) variableData).get(recordNo - offset);
    }

    /**
     * Reads record value contained in byte array
     *
     * @param role     variable name
     * @param recordNo record index in range 0 .. numRecords-1
     *
     * @return record value as byte
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1 or if file io fails
     */
    public byte getByte(String role, int recordNo) throws IOException {
        final int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayByte.D1) variableData).get(recordNo - offset);
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
        final int offset = fetch(recordNo);
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
    public short getShort(String role, int recordNo) throws IOException {
        final int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayShort.D1) variableData).get(recordNo - offset);
    }

    /**
     * Returns a record value contained in a numeric array.
     *
     * @param role     The variable name.
     * @param recordNo The record number in range 0 .. numRecords-1.
     *
     * @return the record value as {@code Number}.
     *
     * @throws IOException if the record number is out of range 0 .. numRecords-1 or if file io fails.
     */
    public Number getNumber(String role, int recordNo) throws IOException {
        final int offset = fetch(recordNo);
        final Array variableData = data.get(role);
        return (Number) variableData.getObject(recordNo - offset);
    }

    /**
     * Returns a record value contained in a numeric array, which is properly scaled
     * according to a variable' s attributes.
     *
     * @param role     The variable name.
     * @param recordNo The record number in range 0 .. numRecords-1.
     *
     * @return the record value as {@code Number}.
     *
     * @throws IOException if the record number is out of range 0 .. numRecords-1 or if file io fails.
     */
    public Number getNumberScaled(String role, int recordNo) throws IOException {
        return getNumberScaled(getNcFile(), role, getNumber(role, recordNo));
    }

    private static Number getNumberScaled(NetcdfFile ncFile, String variableName, Number number) throws IOException {
        final Variable variable = ncFile.findVariable(NetcdfFile.escapeName(variableName));
        return getNumberScaled(variable, number);
    }

    private static Number getNumberScaled(VariableIF variable, Number number) throws IOException {
        Assert.notNull(variable);
        Assert.notNull(number);
        final double factor = getAttribute(variable, "scale_factor", 1.0).doubleValue();
        final double offset = getAttribute(variable, "add_offset", 0.0).doubleValue();

        return factor * number.doubleValue() + offset;
    }

    private static Number getAttribute(VariableIF variable, String attributeName, Number defaultValue) {
        final Attribute attribute = variable.findAttribute(attributeName);
        if (attribute == null) {
            return defaultValue;
        }
        return attribute.getNumericValue();
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
        final int offset = fetch(recordNo);
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
        final int offset = fetch(recordNo);
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
        final int offset = fetch(recordNo);
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
        final int offset = fetch(recordNo);
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
            data.put(varName, values);
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
            final int tileSize = 1024;  // TODO adjust default, read from property
            final List<? extends Variable> variableList = getNcFile().getVariables();
            for (final Variable variable : variableList) {
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
                // we do not use the escaped variable name here because we want to be able to
                // call, e.g., data.get("atsr.longitude")
                data.put(variable.getName(), values);
            }
            bufferStart = recordNo;
            bufferFill = tileSize;
        }
        return bufferStart;
    }

    Array getData(String variableName, int recordNo) throws IOException {
        final Variable variable = getNcFile().findVariable(NetcdfFile.escapeName(variableName));
        if (recordNo >= variable.getShape()[0]) {
            throw new IllegalArgumentException("recordNo >= variable.getShape()[0]");
        }
        // todo - why not use fetch(recordNo)? (rq-20110323)
        fetch(variableName, recordNo);
        final int offset = offsetMap.get(variableName);
        final int index = recordNo - offset;
        final Array slice = data.get(variableName).slice(0, index);
        final int[] shape1 = slice.getShape();
        final int[] shape2 = new int[shape1.length + 1];
        shape2[0] = 1;
        System.arraycopy(shape1, 0, shape2, 1, shape1.length);
        return slice.reshape(shape2);
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