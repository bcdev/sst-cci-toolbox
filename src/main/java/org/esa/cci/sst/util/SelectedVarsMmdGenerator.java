/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.util;

import org.esa.cci.sst.Constants;
import org.esa.cci.sst.SensorName;
import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.Variable;
import org.esa.cci.sst.reader.ObservationIOHandler;
import org.postgis.PGgeometry;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFileWriteable;

import javax.persistence.Query;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.esa.cci.sst.SensorName.*;

/**
 * Implementation of MmdGenerator, which writes only specified variables. Holds an instance of
 * <code>DefaultMmdGenerator</code> as delegate.
 *
 * @author Thomas Storm
 */
public class SelectedVarsMmdGenerator implements MmdGeneratorTool.MmdGenerator {

    private final List<String> outputVariables;
    private DefaultMmdGenerator delegate;

    SelectedVarsMmdGenerator(final Properties properties, final List<String> outputVariables) throws IOException {
        this.outputVariables = outputVariables;
        delegate = new DefaultMmdGenerator(properties);
    }

    @Override
    public void createMmdStructure(final NetcdfFileWriteable file) throws Exception {
        delegate.addDimensions(file);
        addStandardVariables(file);

        for (SensorName sensorName : SensorName.values()) {
            if (!SENSOR_NAME_AATSR_MD.getSensor().equalsIgnoreCase(sensorName.getSensor()) &&
                !SENSOR_NAME_AAI.getSensor().equalsIgnoreCase(sensorName.getSensor()) &&
                !SENSOR_NAME_INSITU.getSensor().equalsIgnoreCase(sensorName.getSensor())) {
                delegate.addObservationTime(file, sensorName.getSensor());
                delegate.addLsMask(file, sensorName.getSensor());
                delegate.addNwpData(file, sensorName.getSensor());
                addVariables(file, sensorName.getSensor());
            }
        }
        delegate.addGlobalAttributes(file);
        file.setLargeFile(true);
        file.create();
    }

    @Override
    public void writeMatchups(final NetcdfFileWriteable file) throws Exception {
        // open database
        delegate.getPersistenceManager().transaction();
        try {
            final List<Matchup> resultList = delegate.getMatchups();
            int matchupCount = resultList.size();

            for (int matchupIndex = 0; matchupIndex < matchupCount; matchupIndex++) {
                Matchup matchup = resultList.get(matchupIndex);
                final int matchupId = matchup.getId();
                // todo - replace with logging
                System.out.println("Writing matchup '" + matchupId + "' (" + matchupIndex + "/" + matchupCount + ").");
                final ReferenceObservation referenceObservation = matchup.getRefObs();
                final List<Coincidence> coincidences = matchup.getCoincidences();
                final PGgeometry point = referenceObservation.getPoint();
                for (Coincidence coincidence : coincidences) {
                    writeObservation(file, coincidence.getObservation(), point, referenceObservation.getTime(), matchupIndex);
                }
                writeObservation(file, referenceObservation, point, referenceObservation.getTime(), matchupIndex);
                delegate.writeMatchupId(file, matchupId, matchupIndex);
                delegate.getPersistenceManager().detach(coincidences);
            }
        } finally {
            // close database
            delegate.getPersistenceManager().commit();
        }
    }

    @Override
    public void close() {
        delegate.close();
    }

    private void writeObservation(final NetcdfFileWriteable file, final RelatedObservation observation,
                                  final PGgeometry point, final Date refTime, final int matchupIndex) throws Exception {
        ObservationIOHandler ioHandler = delegate.getReader(observation);
        final Variable[] variables = ioHandler.getVariables();
        for (Variable variable : variables) {
            if (outputVariables.contains(variable.getName().replace(observation.getSensor() + ".", ""))) {
                ioHandler.write(observation, variable, file, matchupIndex, delegate.getDimensionSizes(variable.getName()),
                             point, refTime);
            }
        }
    }

    private void addStandardVariables(final NetcdfFileWriteable file) {
        delegate.addVariable(file, "mId", DataType.INT, Constants.DIMENSION_NAME_MATCHUP);
        delegate.addInsituDataHistories(file);
        delegate.addVariable(file, SENSOR_NAME_AAI.getSensor() + ".aai", DataType.SHORT,
                             Constants.DIMENSION_NAME_MATCHUP + " aai.ni");
        addVariables(file, SENSOR_NAME_AATSR_MD.getSensor());
    }

    @SuppressWarnings({"unchecked"})
    private void addVariables(final NetcdfFileWriteable file, final String sensor) {
        final Query query = delegate.createVariablesQuery(sensor);
        final List<Variable> resultList = query.getResultList();
        for (final Variable var : resultList) {
            String cleanVarName = var.getName();
            final int index = cleanVarName.indexOf(".");
            cleanVarName = cleanVarName.substring(index + 1);
            if (!outputVariables.contains(cleanVarName)) {
                return;
            }
            delegate.addVariable(file, var, delegate.createDimensionString(var, sensor));
        }
    }

}
