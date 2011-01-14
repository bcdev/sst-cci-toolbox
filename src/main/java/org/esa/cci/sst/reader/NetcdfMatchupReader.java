package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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
abstract public class NetcdfMatchupReader implements ObservationReader {

    static final String DEFAULT_POSTFIX = ".default";

    protected NetcdfFile netcdf;
    protected int length;
    protected DataFile dataFileEntry;
    protected int sstFillValue;

    private Map<String, Object> data = new HashMap<String, Object>();
    private int bufferStart = 0;
    private int bufferFill = 0;
    private int tileSize = 1024;  // TODO adjust default, read from property

    /**
     * Number of records of file. initialised in init() when opening the NetCDF file.
     * @return   number of records in NetCDF file
     */
    @Override
    public int length() {
        return length;
    }

    /**
     * Constant name of dimension to read the number of records from
     *
     * @return dimension name
     */
    abstract protected String getDimensionName();

    /**
     * Constant name of variable to read the sst value from
     *
     * @return variable name
     */
    abstract protected String getSstVariableName();

    /**
     * Constant list of variables to be cached from observation file needed to fill observations
     *
     * @return list of variable names
     */
    abstract protected String[] getVariableNames();

    /**
     * Opens NetCDF file, reads number of records and SST fill value, and
     * initialises map of variables. May be overridden to initialise additional
     * variables.
     *
     * @param observationFile file of observations in format corresponding to reader
     * @param dataFileEntry   data file entry to be referenced in each observation created by reader
     * @throws IOException  if file access fails
     */
    @Override
    public void init(File observationFile, DataFile dataFileEntry) throws IOException {

        this.dataFileEntry = dataFileEntry;

        // open match-up file
        netcdf = NetcdfFile.open(observationFile.getPath());
        // read number of records value
        length = netcdf.findDimension(getDimensionName()).getLength();
        // read SST fill value
        final Variable variable = netcdf.findVariable(getSstVariableName().replaceAll("\\.", "%2e"));
        sstFillValue = variable.findAttributeIgnoreCase("_fillvalue").getNumericValue().intValue();

        // initialise buffer with variable names
        for (String variableName : getVariableNames()) {
            data.put(variableName, null);
        }
    }

    /**
     * Closes NetCDF file
     * @throws IOException  if closing fails
     */
    @Override
    public void close() throws IOException {
        if (netcdf != null) {
            netcdf.close();
        }
    }

    /**
     * Ensures that identified record is in the data buffer, maybe reads tile to achieve this.
     *
     * @param recordNo  index of record to be in the data buffer
     * @return index of first record in buffer, to be used as offset of record
     *         number when accessing data buffer
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. length-1
     */
    protected int fetch(int recordNo) throws IOException, InvalidRangeException {
        if (recordNo < bufferStart || recordNo >= bufferStart + bufferFill) {
            Range range = new Range(recordNo, recordNo + tileSize - 1);
            int size = tileSize;
            for (String variableName : getVariableNames()) {
                final Variable variable = netcdf.findVariable(variableName.replaceAll("\\.", "%2e"));
                final int[] shape = variable.getShape();
                final int[] start = new int[shape.length];
                start[0] = recordNo;
                shape[0] = (shape[0] < recordNo + tileSize)
                        ? shape[0] - recordNo
                        : tileSize;
                final Object values = variable.read(start, shape);
                data.put(variableName, values);
            }
            bufferStart = recordNo;
            bufferFill = size;
        }
        return bufferStart;
    }

    /**
     * Reads record value contained in 2D char array
     * @param role  variable name
     * @param recordNo  record index in range 0 .. length-1
     * @return  record value as String
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. length-1
     */
    public String getString(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayChar.D2) variableData).getString(recordNo - offset);
    }

    /**
     * Reads record value contained in float array
     * @param role  variable name
     * @param recordNo  record index in range 0 .. length-1
     * @return record value as float
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. length-1
     */
    public float getFloat(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayFloat.D1) variableData).get(recordNo - offset);
    }

    /**
     * Reads record value contained in double array
     * @param role  variable name
     * @param recordNo  record index in range 0 .. length-1
     * @return record value as double
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. length-1
     */
    public double getDouble(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayDouble.D1) variableData).get(recordNo - offset);
    }

    /**
     * Reads record value contained in int array
     * @param role  variable name
     * @param recordNo  record index in range 0 .. length-1
     * @return record value as int
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. length-1
     */
    public int getInt(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayInt.D1) variableData).get(recordNo - offset);
    }

    /**
     * Reads record value contained in short array
     * @param role  variable name
     * @param recordNo  record index in range 0 .. length-1
     * @return record value as int
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. length-1
     */
    public int getShort(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayShort.D1) variableData).get(recordNo - offset);
    }

    /**
     * Reads single pixel value from record of sub-scenes contained in 3D short array
     * @param role  variable name
     * @param recordNo  record index in range 0 .. length-1
     * @param line  pixel line position in sub-scene
     * @param column  pixel column position in sub-scene
     * @return pixel value as int
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. length-1
     *           or line and column are out of range
     */
    public int getShort(String role, int recordNo, int line, int column) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayShort.D3) variableData).get(recordNo - offset, line, column);
    }

    /**
     * Reads single pixel value from record of sub-scenes contained in 3D int array
     * @param role  variable name
     * @param recordNo  record index in range 0 .. length-1
     * @param line  pixel line position in sub-scene
     * @param column  pixel column position in sub-scene
     * @return pixel value as int
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. length-1
     *           or line and column are out of range
     */
    public int getInt(String role, int recordNo, int line, int column) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayInt.D3) variableData).get(recordNo - offset, line, column);
    }

    /**
     * Reads single pixel value from record of sub-scenes contained in 3D double array
     * @param role  variable name
     * @param recordNo  record index in range 0 .. length-1
     * @param line  pixel line position in sub-scene
     * @param column  pixel column position in sub-scene
     * @return pixel value as double
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. length-1
     *           or line and column are out of range
     */
    public double getDouble(String role, int recordNo, int line, int column) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayDouble.D3) variableData).get(recordNo - offset, line, column);
    }

    /**
     * Reads single pixel value from record of sub-scenes contained in 2D double array
     * @param role  variable name
     * @param recordNo  record index in range 0 .. length-1
     * @param line  pixel line position in sub-scene
     * @return pixel value as double
     * @throws IOException  if file io fails
     * @throws InvalidRangeException  if record number is out of range 0 .. length-1
     *           or line and column are out of range
     */
    public double getDouble(String role, int recordNo, int line) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayDouble.D2) variableData).get(recordNo - offset, line);
    }
}
