/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools.arcprocessing;

import org.esa.cci.sst.Queries;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.Constants;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * the strategy to create subscenes and ARC3 them:
 * - obtain the source file
 * - cut out subscenes using database information
 * - put these into a new mmd file
 * - copy all old values into that file
 * - take that file as input for arc 3
 * - reingest the new file
 * - delete the scripts
 *
 * @author Thomas Storm
 */
class SubsceneArc3CallBuilder extends Arc3CallBuilder {

    private final Properties configuration;
    private final PersistenceManager persistenceManager;

    SubsceneArc3CallBuilder(Properties configuration, PersistenceManager persistenceManager) {
        super(configuration);
        this.configuration = new Properties(configuration);
        this.persistenceManager = persistenceManager;
    }

    @Override
    public String createArc3Call() throws IOException {
        final String sourceFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE);
        final String targetFilename = createSubsceneMmdFilename();
        validateSourceFilename(sourceFilename);

        final NetcdfFile source = NetcdfFile.open(sourceFilename);
        final NetcdfFileWriteable target = NetcdfFileWriteable.createNew(targetFilename);
        final List<Variable> atsrSourceVars = getAtsrSourceVariables(source);
        validateSourceVariables(atsrSourceVars);

        addSubsceneDimensions(target, atsrSourceVars.get(0));
        addSubsceneVariables(target, atsrSourceVars);

        final Variable latitude = getAtsrSourceVar(atsrSourceVars, "latitude");
        final Variable longitude = getAtsrSourceVar(atsrSourceVars, "longitude");
        final Map<Integer, Point> matchupLocations = getMatchupLocations(source);
        for (Map.Entry<Integer, Point> entry : matchupLocations.entrySet()) {
            final Integer matchupId = entry.getKey();
            final int[] coords = findCentralNetcdfCoords(latitude, longitude, matchupId, entry.getValue());
            for (Variable atsrSourceVar : atsrSourceVars) {
                final Array sourceValues = readSubscene(matchupId, coords, atsrSourceVar, Constants.ATSR_SUBSCENE_WIDTH);
//                target.write(atsrSourceVar.getNameEscaped(), sourceValues);
            }
        }

        final StringBuilder builder = new StringBuilder();
        return builder.toString();
    }

    @Override
    String createReingestionCall() {
        return null;
    }

    @Override
    String createCleanupCall() {
        return null;
    }

    Array readSubscene(int matchupId, int[] coords, Variable atsrSourceVar, int width) throws IOException {
        final int[] origin = {
                matchupId,
                coords[0] - (width / 2),
                coords[1] - (width / 2)
        };
        final int[] shape = {1, width, width};
        final Array values;
        try {
            values = atsrSourceVar.read(origin, shape);
        } catch (InvalidRangeException e) {
            throw new IOException("Unable to read from variable '" + atsrSourceVar.getName() + "'.", e);
        }
        return values;
    }

    int[] findCentralNetcdfCoords(Variable latitude, Variable longitude, int matchupId, Point point) throws IOException {
        final int[] latOrigin = {matchupId, 0, 0};
        final int[] latShape = {1, latitude.getDimension(1).getLength(), latitude.getDimension(2).getLength()};
        final int[] lonOrigin = {matchupId, 0, 0};
        final int[] lonShape = {1, longitude.getDimension(1).getLength(), longitude.getDimension(2).getLength()};
        final Array matchupLatitude;
        final Array matchupLongitude;
        try {
            matchupLatitude = latitude.read(latOrigin, latShape);
            matchupLongitude = longitude.read(lonOrigin, lonShape);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
        final IndexIterator latitudeIterator = matchupLatitude.getIndexIterator();
        final IndexIterator longitudeIterator = matchupLongitude.getIndexIterator();
        double delta = Double.MAX_VALUE;
        int[] centralPoint = new int[]{-1, -1};
        while (latitudeIterator.hasNext() && longitudeIterator.hasNext()) {
            final double currentLon = longitudeIterator.getDoubleNext();
            final double currentLat = latitudeIterator.getDoubleNext();
            final double currentDelta = Math.abs(currentLon - point.getX()) + Math.abs(currentLat - point.getY());
            if(currentDelta < delta) {
                delta = currentDelta;
                centralPoint[0] = longitudeIterator.getCurrentCounter()[1];
                centralPoint[1] = longitudeIterator.getCurrentCounter()[2];
            }
        }
        return centralPoint;
    }

    Variable getAtsrSourceVar(List<Variable> atsrSourceVars, String suffix) {
        for (Variable atsrSourceVar : atsrSourceVars) {
            if (atsrSourceVar.getName().endsWith(suffix)) {
                return atsrSourceVar;
            }
        }
        throw new IllegalStateException(MessageFormat.format("No variable found with suffix ''{0}''.", suffix));
    }

    Map<Integer, Point> getMatchupLocations(NetcdfFile source) throws IOException {
        Variable matchupVariable = getMatchupVariable(source);
        final Array matchupVariables = matchupVariable.read();
        final IndexIterator indexIterator = matchupVariables.getIndexIterator();
        final HashMap<Integer, Point> map = new HashMap<Integer, Point>();
        while (indexIterator.hasNext()) {
            final Integer currentMatchupId = (Integer) indexIterator.next();
            final ReferenceObservation observation = Queries.getReferenceObservationForMatchup(persistenceManager,
                                                                                               currentMatchupId);
            map.put(currentMatchupId, observation.getPoint().getGeometry().getFirstPoint());
        }
        return map;
    }

    void addSubsceneVariables(NetcdfFileWriteable target, List<Variable> atsrSourceVars) {
        for (Variable atsrSourceVar : atsrSourceVars) {
            target.addVariable(atsrSourceVar.getName(), atsrSourceVar.getDataType(),
                               atsrSourceVar.getDimensionsString());
        }
    }

    void addSubsceneDimensions(NetcdfFileWriteable target, Variable atsrSourceVar) {
        for (Dimension dimension : atsrSourceVar.getDimensions()) {
            int length = Constants.ATSR_SUBSCENE_WIDTH;
            if (dimension.getName().equalsIgnoreCase("record")) {
                length = dimension.getLength();
            }
            target.addDimension(dimension.getName(), length);
        }
    }

    Variable getMatchupVariable(NetcdfFile source) {
        String matchupNameEscaped = NetcdfFile.escapeName(Constants.VARIABLE_NAME_MATCHUP_ID);
        Variable matchupVariable = source.findVariable(matchupNameEscaped);
        if (matchupVariable == null) {
            String matchupNameAlternativeEscaped = NetcdfFile.escapeName(
                    Constants.VARIABLE_NAME_MATCHUP_ID_ALTERNATIVE);
            matchupVariable = source.findVariable(matchupNameAlternativeEscaped);
        }
        if (matchupVariable == null) {
            throw new IllegalStateException(
                    MessageFormat.format("File ''{0}'' does neither contain the variable ''{1}'' nor ''{2}''.",
                                         source.getLocation(),
                                         Constants.VARIABLE_NAME_MATCHUP_ID,
                                         Constants.VARIABLE_NAME_MATCHUP_ID_ALTERNATIVE));
        }
        return matchupVariable;
    }

    String createSubsceneMmdFilename() {
        final String sourceFilename = configuration.getProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE);
        final int extensionStart = sourceFilename.lastIndexOf('.');
        final StringBuilder builder = new StringBuilder(sourceFilename);
        return builder.insert(extensionStart, "_subscenes").toString();
    }

    private List<Variable> getAtsrSourceVariables(NetcdfFile source) {
        final List<Variable> atsrVars = new ArrayList<Variable>();
        for (Variable variable : source.getVariables()) {
            if (variable.getName().startsWith("atsr_orb")) {
                atsrVars.add(variable);
            }
        }
        return atsrVars;
    }

    private static void validateSourceVariables(List<Variable> sourceVars) {
        if (sourceVars == null || sourceVars.isEmpty()) {
            throw new IllegalStateException("No variables of type 'atsr_orb' within source file.");
        }
    }
}
