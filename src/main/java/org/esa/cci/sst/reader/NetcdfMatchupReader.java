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
 * TODO add API doc
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

    @Override
    public void close() throws IOException {
        if (netcdf != null) {
            netcdf.close();
        }
    }

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


    public String getString(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayChar.D2) variableData).getString(recordNo - offset);
    }

    public float getFloat(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayFloat.D1) variableData).get(recordNo - offset);
    }

    public double getDouble(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayDouble.D1) variableData).get(recordNo - offset);
    }

    public int getInt(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayInt.D1) variableData).get(recordNo - offset);
    }

    public int getShort(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayShort.D1) variableData).get(recordNo - offset);
    }

    public int getShort(String role, int recordNo, int line, int column) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayShort.D3) variableData).get(recordNo - offset, line, column);
    }

    public int getInt(String role, int recordNo, int line, int column) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayInt.D3) variableData).get(recordNo - offset, line, column);
    }

    public double getDouble(String role, int recordNo, int line, int column) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayDouble.D3) variableData).get(recordNo - offset, line, column);
    }

    public double getDouble(String role, int recordNo, int line) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        return ((ArrayDouble.D2) variableData).get(recordNo - offset, line);
    }
}
