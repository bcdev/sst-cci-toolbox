package org.esa.cci.sst.reader;

import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO add API doc
 *
 * @author Martin Boettcher
 */
abstract public class NetcdfMatchupReader {

    static final String DEFAULT_POSTFIX = ".default";

    private NetcdfFile matchupFile;
    private String schemaName;
    private String observationType;
    private Map<String,Object> data = new HashMap<String,Object>();
    private int bufferStart = 0;
    private int bufferFill = 0;
    private int tileSize = 16;  // TODO adjust default, read from property

    abstract public Date getDate(String role, int recordNo) throws IOException, InvalidRangeException;
    abstract public float getCoordinate(String role, int recordNo) throws IOException, InvalidRangeException;

    public NetcdfMatchupReader init(NetcdfFile matchupFile, String schemaName, String observationType) {

        this.matchupFile = matchupFile;
        this.schemaName = schemaName;
        this.observationType = observationType;
        final String prefix = String.format("%s.%s.", schemaName, observationType);

        for (Object k : System.getProperties().keySet()) {
            String key = (String) k;
            if (key.startsWith(prefix)) {
                if (key.endsWith(DEFAULT_POSTFIX)) {
                    data.put(key.substring(prefix.length(), key.length() - DEFAULT_POSTFIX.length()), System.getProperty(key));
                } else {
                    data.put(key.substring(prefix.length()), null);
                }
            }
        }

        return this;
    }

    private int fetch(int recordNo) throws IOException, InvalidRangeException {
        if (recordNo < bufferStart || recordNo >= bufferStart + bufferFill) {
            Range range = new Range(recordNo, recordNo + tileSize - 1);
            int size = tileSize;
            for (String role : data.keySet()) {
                Object values = data.get(role);
                if (values == null || (values instanceof Array)) {
                    final String variableName = String.format("%s.%s.%s", schemaName, observationType, role);
                    final Variable variable = matchupFile.findVariable(System.getProperty(variableName));
                    final int[] shape = variable.getShape();
                    final int[] start = new int[shape.length];
                    start[0] = recordNo;
                    shape[0] = (shape[0] < recordNo + tileSize)
                            ? shape[0] - recordNo
                            : tileSize;
                    values = ((Variable) variable).read(start, shape);
                    data.put(role, values);
                }
            }
            bufferStart = recordNo;
            bufferFill = size;
        }
        return bufferStart;
    }

/*
    public NetcdfMatchupReader read() throws IOException {
        for (String role : data.keySet()) {
            if (data.get(role) == null) {
                final String variableName = String.format("%s.%s.%s", schemaName, observationType, role);
                data.put(role, matchupFile.findVariable(System.getProperty(variableName)).read());
            }
        }
        return this;
    }
*/

    public String getString(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        if (variableData instanceof ArrayChar.D2) {
            return ((ArrayChar.D2) variableData).getString(recordNo - offset);
        } else {
            return (String) variableData;
        }
    }

    public float getFloat(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        if (variableData instanceof ArrayFloat.D1) {
            return ((ArrayFloat.D1) variableData).get(recordNo - offset);
        } else {
            return Float.parseFloat((String) variableData);
        }
    }

    public double getDouble(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        if (variableData instanceof ArrayDouble.D1) {
            return ((ArrayDouble.D1) variableData).get(recordNo - offset);
        } else {
            return Double.parseDouble((String) variableData);
        }
    }

    public int getInt(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        if (variableData instanceof ArrayInt.D1) {
            return ((ArrayInt.D1) variableData).get(recordNo - offset);
        } else {
            return Integer.parseInt((String) variableData);
        }
    }

    public int getShort(String role, int recordNo) throws IOException, InvalidRangeException {
        int offset = fetch(recordNo);
        Object variableData = data.get(role);
        if (variableData instanceof ArrayShort.D1) {
            return ((ArrayShort.D1) variableData).get(recordNo - offset);
        } else {
            return Integer.parseInt((String) variableData);
        }
    }
}
