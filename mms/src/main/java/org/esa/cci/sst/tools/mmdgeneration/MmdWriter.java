package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.IoUtil;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;

import java.io.IOException;
import java.util.*;

class MmdWriter {

    private final NetcdfFileWriter fileWriter;

    static boolean canOpen(String filePath) throws IOException {
        return NetcdfFile.canOpen(filePath);
    }

    static MmdWriter open(String filePath) throws IOException {
        final NetcdfFileWriter netcdfFileWriter = NetcdfFileWriter.openExisting(filePath);

        return new MmdWriter(netcdfFileWriter);
    }

    MmdWriter(NetcdfFileWriter fileWriter) {
        this.fileWriter = fileWriter;
    }

    void initialize(int matchupCount, Map<String, Integer> dimensionConfig, List<Item> variableList) throws IOException {
        addDimensions(matchupCount, dimensionConfig);
        addGlobalAttributes(matchupCount);
        addVariables(variableList);

        fileWriter.create();
    }

    NetcdfFileWriter getFileWriter() {
        return fileWriter;  // @todo 2 tb/tb remove when code is migrated completely tb 2014-03-12
    }

    void close() throws IOException {
        fileWriter.flush();
        fileWriter.close();
    }

    private void addVariables(List<Item> variableList) {
        for (Item variable : variableList) {
            IoUtil.addVariable(fileWriter, variable);
        }
    }

    private void addGlobalAttributes(int matchupCount) {
        fileWriter.addGroupAttribute(null, new Attribute("title", "SST CCI multi-sensor match-up dataset (MMD) template"));
        fileWriter.addGroupAttribute(null, new Attribute("institution", "Brockmann Consult"));
        fileWriter.addGroupAttribute(null, new Attribute("contact", "Ralf Quast (ralf.quast@brockmann-consult.de)"));
        fileWriter.addGroupAttribute(null, new Attribute("creation_date", Calendar.getInstance().getTime().toString()));
        fileWriter.addGroupAttribute(null, new Attribute("total_number_of_matchups", matchupCount));
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
