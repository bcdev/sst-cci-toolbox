package org.esa.cci.sst.util;

import org.esa.cci.sst.Constants;
import org.esa.cci.sst.data.Variable;
import org.esa.cci.sst.orm.PersistenceManager;
import ucar.ma2.DataType;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFileWriteable;

import javax.persistence.Query;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class MmdFormatGenerator {

    private final PersistenceManager persistenceManager;

    public static void main(String[] args) throws IOException {
        final MmdFormatGenerator generator = new MmdFormatGenerator();
        generator.generateMmdFormat();
    }

    public MmdFormatGenerator() {
        persistenceManager = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME);
    }

    private void generateMmdFormat() throws IOException {
        final NetcdfFileWriteable file = NetcdfFileWriteable.createNew("mmd.nc");
        file.addUnlimitedDimension("match_up");

        file.addDimension("aatsr.ni", 101);
        file.addDimension("aatsr.nj", 101);
        file.addDimension("metop.ni", 21);
        file.addDimension("metop.nj", 21);
        file.addDimension("metop.len_id", 11);
        file.addDimension("metop.len_filename", 65);
        file.addDimension("seviri.ni", 5);
        file.addDimension("seviri.nj", 5);
        file.addDimension("seviri.len_id", 11);
        file.addDimension("seviri.len_filename", 65);

        addVariables(file, Constants.SENSOR_NAME_AATSR);
        addVariables(file, Constants.SENSOR_NAME_METOP);
        addVariables(file, Constants.SENSOR_NAME_SEVIRI);

        file.create();
        file.close();
        NCdumpW.printHeader("mmd.nc", new PrintWriter(System.out));
    }

    private void addVariables(NetcdfFileWriteable file, String sensorName) {
        final Query query = createVariablesQuery(sensorName);
        @SuppressWarnings({"unchecked"})
        final List<Variable> resultList = query.getResultList();
        for (Variable variable : resultList) {
            if (variable.getName().endsWith("observation_time")) {
                file.addVariable(file.getRootGroup(), variable.getName(), DataType.valueOf(variable.getType()),
                                 String.format("match_up %s.ni", sensorName));
            } else {
                final String[] dimensionNames = variable.getDimensions().split(" ");
                final String[] dimensionRoles = variable.getDimensionRoles().split(" ");
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < dimensionRoles.length; i++) {
                    final String dimensionName = dimensionNames[i];
                    final String dimensionRole = dimensionRoles[i];
                    if (i != 0) {
                        sb.append(" ");
                    }
                    if (!"match_up".equals(dimensionRole)) {
                        sb.append(sensorName);
                        sb.append(".");
                    }
                    if (!"length".equals(dimensionRole)) {
                        sb.append(dimensionRole);
                    } else {
                        sb.append(dimensionName);
                    }
                }
                try {
                    file.addVariable(file.getRootGroup(), variable.getName(), DataType.valueOf(variable.getType()),
                                     sb.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Query createMatchupCountQuery() {
        return persistenceManager.createQuery("select count(m) from Matchup m");
    }

    private Query createVariablesQuery(String sensorName) {
        return persistenceManager.createQuery(
                String.format("select v from Variable v where v.name like '%s.%%'", sensorName));
    }
}
