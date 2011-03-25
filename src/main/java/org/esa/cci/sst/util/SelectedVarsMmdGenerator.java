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
import org.esa.cci.sst.MmdGeneratorTool;
import org.esa.cci.sst.SensorType;
import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.reader.IOHandler;
import org.postgis.PGgeometry;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFileWriteable;

import javax.persistence.Query;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.esa.cci.sst.SensorType.*;

/**
 * Implementation of MmdGenerator, which writes only specified variables. Holds an instance of
 * <code>DefaultMmdGenerator</code> as delegate.
 *
 * @author Thomas Storm
 */
public class SelectedVarsMmdGenerator implements MmdGeneratorTool.MmdGenerator {

    private final List<String> outputVariables;
    private DefaultMmdGenerator delegate;

    public SelectedVarsMmdGenerator(final Properties properties, final List<String> outputVariables) throws IOException {
        this.outputVariables = outputVariables;
        delegate = new DefaultMmdGenerator(properties);
    }

    @Override
    public void createMmdStructure(final NetcdfFileWriteable file) throws Exception {
        delegate.addDimensions(file);
        addStandardVariables(file);

        // todo - iterate over sensors (rq-20110322)
        for (SensorType sensorType : SensorType.values()) {
            if (!ATSR_MD.nameLowerCase().equalsIgnoreCase(sensorType.nameLowerCase()) &&
                !AAI.nameLowerCase().equalsIgnoreCase(sensorType.nameLowerCase()) &&
                !HISTORY.nameLowerCase().equalsIgnoreCase(sensorType.nameLowerCase())) {
                delegate.addObservationTime(file, sensorType.nameLowerCase());
                delegate.addLsMask(file, sensorType.nameLowerCase());
                delegate.addNwpData(file, sensorType.nameLowerCase());
                addVariables(file, sensorType.nameLowerCase());
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
                    writeObservation(file, coincidence.getObservation(), point, referenceObservation.getTime(),
                                     matchupIndex);
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

    private void writeObservation(final NetcdfFileWriteable file, final Observation observation,
                                  final PGgeometry point, final Date refTime, final int matchupIndex) throws Exception {
        IOHandler ioHandler = delegate.getReader(observation);
        final VariableDescriptor[] variableDescriptors = ioHandler.getVariableDescriptors();
        for (VariableDescriptor variableDescriptor : variableDescriptors) {
            if (outputVariables.contains(variableDescriptor.getName().replace(observation.getSensor() + ".", ""))) {
                ioHandler.write(file, observation, variableDescriptor, matchupIndex,
                                delegate.getDimensionSizes(variableDescriptor.getName()),
                                point, refTime);
            }
        }
    }

    private void addStandardVariables(final NetcdfFileWriteable file) {
        delegate.addVariable(file, "matchup_id", DataType.INT, Constants.DIMENSION_NAME_MATCHUP);
        delegate.addInsituDataHistories(file);
        delegate.addVariable(file, AAI.nameLowerCase() + ".aai", DataType.SHORT,
                             Constants.DIMENSION_NAME_MATCHUP + " aai.ni");
        addVariables(file, ATSR_MD.nameLowerCase());
    }

    @SuppressWarnings({"unchecked"})
    private void addVariables(final NetcdfFileWriteable file, final String sensor) {
        final Query query = delegate.createVariablesQuery(sensor);
        final List<VariableDescriptor> resultList = query.getResultList();
        for (final VariableDescriptor var : resultList) {
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
