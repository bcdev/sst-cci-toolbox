package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.IoUtil;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.esa.cci.sst.tools.Constants.ATTRIBUTE_CREATION_DATE_NAME;
import static org.esa.cci.sst.tools.Constants.ATTRIBUTE_NUM_MATCHUPS_NAME;

class MmdWriter implements Closeable {

    private final NetcdfFileWriter fileWriter;

    MmdWriter(NetcdfFileWriter fileWriter, int matchupCount, Map<String, Integer> dimensions, List<Item> variables) throws IOException {
        this.fileWriter = fileWriter;
        this.fileWriter.setLargeFile(true);
        this.fileWriter.setFill(true);

        addDimensions(matchupCount, dimensions);
        addGlobalAttributes(matchupCount);
        addVariables(variables);

        this.fileWriter.create();
    }

    @Override
    public void close() throws IOException {
        fileWriter.close();
    }

    List<Variable> getVariables() {
        final NetcdfFile netcdfFile = fileWriter.getNetcdfFile();
        return netcdfFile.getVariables();
    }

    void write(Variable variable, int[] origin, Array array) throws IOException, InvalidRangeException {
        fileWriter.write(variable, origin, array);
    }

    private void addVariables(List<Item> variableList) {
        for (Item variable : variableList) {
            IoUtil.addVariable(fileWriter, variable);
        }
    }

    private void addGlobalAttributes(int matchupCount) {
        fileWriter.addGroupAttribute(null, new Attribute("title", "SST CCI multi-sensor match-up dataset (MMD)"));
        fileWriter.addGroupAttribute(null, new Attribute("institution", "Brockmann Consult"));
        fileWriter.addGroupAttribute(null, new Attribute("contact", "Ralf Quast (ralf.quast@brockmann-consult.de)"));
        fileWriter.addGroupAttribute(null, new Attribute(ATTRIBUTE_CREATION_DATE_NAME, Calendar.getInstance().getTime().toString()));
        fileWriter.addGroupAttribute(null, new Attribute(ATTRIBUTE_NUM_MATCHUPS_NAME, matchupCount));
    }

    private void addDimensions(int matchupCount, Map<String, Integer> dimensionConfig) {
        fileWriter.addDimension(null, Constants.DIMENSION_NAME_MATCHUP, matchupCount);

        final Set<String> dimensionNames = dimensionConfig.keySet();
        for (final String dimensionName : dimensionNames) {
            final Integer dimensionSize = dimensionConfig.get(dimensionName);
            fileWriter.addDimension(null, dimensionName, dimensionSize);
        }
    }
}
