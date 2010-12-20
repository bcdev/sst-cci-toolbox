package org.esa.cci.sst;

import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * TODO add API doc
 *
 * @author Martin Boettcher
 */
public class NetcdfMatchupReader {

    static final String DEFAULT_POSTFIX = ".default";

    private NetcdfFile matchupFile;
    private String schemaName;
    private String observationType;
    private Map<String,Object> data = new HashMap<String,Object>();

    public NetcdfMatchupReader(NetcdfFile matchupFile, String schemaName, String observationType) {

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
    }

    public void read() throws IOException {
        for (String role : data.keySet()) {
            if (data.get(role) == null) {
                final String variableName = String.format("%s.%s.%s", schemaName, observationType, role);
                data.put(role, matchupFile.findVariable(System.getProperty(variableName)).read());
            }
        }
    }

    public String getString(String role, int recordNo) {
        Object variableData = data.get(role);
        if (variableData instanceof ArrayChar.D2) {
            return ((ArrayChar.D2) variableData).getString(recordNo);
        } else {
            return (String) variableData;
        }
    }

    public float getFloat(String role, int recordNo) {
        Object variableData = data.get(role);
        if (variableData instanceof ArrayFloat.D1) {
            return ((ArrayFloat.D1) variableData).get(recordNo);
        } else {
            return Float.parseFloat((String) variableData);
        }
    }

    public double getDouble(String role, int recordNo) {
        Object variableData = data.get(role);
        if (variableData instanceof ArrayDouble.D1) {
            return ((ArrayDouble.D1) variableData).get(recordNo);
        } else {
            return Double.parseDouble((String) variableData);
        }
    }

    public int getInt(String role, int recordNo) {
        Object variableData = data.get(role);
        if (variableData instanceof ArrayInt.D1) {
            return ((ArrayInt.D1) variableData).get(recordNo);
        } else {
            return Integer.parseInt((String) variableData);
        }
    }
}
