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

package org.esa.cci.sst.tools.arcprocessing;

import org.esa.cci.sst.Queries;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.Constants;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
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

/**
 * Responsible for cutting subscenes from ATSR scenes which are within an MMD file.
 *
 * @author Thomas Storm
 */
public class Arc3SubsceneCutter extends BasicTool {

    public static void main(String[] args) throws IOException {
        final Arc3SubsceneCutter arc3SubsceneCutter = new Arc3SubsceneCutter();
        arc3SubsceneCutter.setCommandLineArgs(args);
        arc3SubsceneCutter.initialize();
        arc3SubsceneCutter.cutSubscene();
    }

    Arc3SubsceneCutter() {
        super("mms-subscene-%s-submit.sh", "0.1");
    }

    private void cutSubscene() throws IOException {
        final String sourceFilename = getConfiguration().getProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE);
        final NetcdfFile source = NetcdfFile.open(sourceFilename);
        final List<Variable> atsrSourceVars = getAtsrSourceVariables(source);
        validateSourceVariables(atsrSourceVars);
        final String targetFilename = SubsceneArc3CallBuilder.createSubsceneMmdFilename(sourceFilename);

        final NetcdfFileWriteable target = NetcdfFileWriteable.createNew(targetFilename);
        addSubsceneDimensions(target, atsrSourceVars.get(0).getDimensions());
        addVariables(source, target);
        addNonSubsceneDimensions(source, target);

        writeSubscene(source, target, atsrSourceVars);
        copyNonSubsceneValues(source, target, getVarNames(atsrSourceVars));
    }

    void addNonSubsceneDimensions(NetcdfFile source, NetcdfFileWriteable target) {
        for (Dimension dimension : source.getDimensions()) {
            if (!isSubsceneDimension(target, dimension)) {
                target.addDimension(dimension.getName(), dimension.getLength());
            }
        }
    }

    void writeSubscene(NetcdfFile source, NetcdfFileWriteable target, List<Variable> atsrSourceVars) throws IOException {
        ensureWriteMode(target);
        final Variable latitude = getAtsrSourceVar(atsrSourceVars, "latitude");
        final Variable longitude = getAtsrSourceVar(atsrSourceVars, "longitude");
        final Map<Integer, Point> matchupLocations = getMatchupLocations(source);
        try {
            for (Map.Entry<Integer, Point> entry : matchupLocations.entrySet()) {
                final Integer matchupId = entry.getKey();
                final int[] coords = findCentralNetcdfCoords(latitude, longitude, matchupId, entry.getValue());
                for (Variable atsrSourceVar : atsrSourceVars) {
                    final Array sourceValues = readSubscene(matchupId, coords, atsrSourceVar, getSubsceneWidth());
                    target.write(atsrSourceVar.getNameEscaped(), sourceValues);
                }
            }
        } catch (InvalidRangeException e) {
            throw new IOException("Could not write subscene.", e);
        }
    }

    void copyNonSubsceneValues(NetcdfFile source, NetcdfFileWriteable target, List<String> subsceneNames) throws IOException {
        ensureWriteMode(target);
        for (Variable variable : source.getVariables()) {
            final String variableName = variable.getName();
            final boolean isSubsceneVariable = subsceneNames.contains(variableName);
            if (!isSubsceneVariable) {
                write(target, variable, variableName);
            }
        }
    }

    Array readSubscene(int matchupId, int[] coords, Variable atsrSourceVar, int width) throws IOException {
        final Section section = createSection(matchupId, coords, width);
        final Array values;
        try {
            values = atsrSourceVar.read(section.origin, section.shape);
        } catch (InvalidRangeException e) {
            throw new IOException(
                    MessageFormat.format("Unable to read from variable ''{0}''.", atsrSourceVar.getName()), e);
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
            if (currentDelta < delta) {
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
            final ReferenceObservation observation = Queries.getReferenceObservationForMatchup(getPersistenceManager(), currentMatchupId);
            map.put(currentMatchupId, observation.getPoint().getGeometry().getFirstPoint());
        }
        return map;
    }

    void addVariables(NetcdfFile source, NetcdfFileWriteable target) {
        for (Variable sourceVar : source.getVariables()) {
            final String varName = sourceVar.getName();
            final DataType dataType = sourceVar.getDataType();
            final String dimensionsString = sourceVar.getDimensionsString();
            final Variable variable = target.addVariable(varName, dataType, dimensionsString);
            addAttributes(sourceVar, variable);
        }
    }

    void addSubsceneDimensions(NetcdfFileWriteable target, List<Dimension> dimensions) {
        for (Dimension dimension : dimensions) {
            int length = getSubsceneWidth();
            if (dimension.getName().equalsIgnoreCase("record")) {
                length = dimension.getLength();
            }
            target.addDimension(dimension.getName(), length);
        }
    }

    Variable getMatchupVariable(NetcdfFile source) {
        String matchupNameEscaped = NetcdfFile.escapeName(Constants.COLUMN_NAME_MATCHUP_ID);
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
                                         Constants.COLUMN_NAME_MATCHUP_ID,
                                         Constants.VARIABLE_NAME_MATCHUP_ID_ALTERNATIVE));
        }
        return matchupVariable;
    }

    int getSubsceneWidth() {
        return Constants.ATSR_SUBSCENE_WIDTH;
    }

    boolean isSubsceneDimension(NetcdfFileWriteable target, Dimension dimension) {
        // there's no other possible method to inquire if netcdf file has dimensions
        try {
            target.getRootGroup().getDimensions();
        } catch (NullPointerException e) {
            return true;
        }
        return target.getRootGroup().findDimension(dimension.getName()) != null;
    }

    static void ensureWriteMode(NetcdfFileWriteable target) throws IOException {
        if (target.isDefineMode()) {
            target.create();
        }
    }

    Section createSection(int matchupId, int[] coords, int width) {
        final int[] origin = {
                matchupId,
                coords[0] - (width / 2),
                coords[1] - (width / 2)
        };
        int[] offsets = new int[]{0, 0, 0};
        for (int i = 0; i < origin.length; i++) {
            if (origin[i] < 0) {
                offsets[i] = 0 - origin[i];
                origin[i] = 0;
            }
        }
        final int[] shape = {1, width, width};
        for (int i = 0; i < shape.length; i++) {
            shape[i] -= offsets[i];
        }
        return new Section(origin, shape);
    }

    private void addAttributes(Variable sourceVar, Variable variable) {
        for (Attribute attribute : sourceVar.getAttributes()) {
            variable.addAttribute(attribute);
        }
    }

    private void write(NetcdfFileWriteable target, Variable variable, String variableName) throws IOException {
        try {
            target.write(NetcdfFile.escapeName(variableName), variable.read());
        } catch (InvalidRangeException e) {
            throw new IOException(MessageFormat.format("Unable to read from variable ''{0}''.", variableName), e);
        }
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

    private List<String> getVarNames(List<Variable> atsrSourceVars) {
        List<String> subsceneNames = new ArrayList<String>();
        for (Variable atsrSourceVar : atsrSourceVars) {
            subsceneNames.add(atsrSourceVar.getName());
        }
        return subsceneNames;
    }

    private static void validateSourceVariables(List<Variable> sourceVars) {
        if (sourceVars == null || sourceVars.isEmpty()) {
            throw new IllegalStateException("No variables of type 'atsr_orb' within source file.");
        }
    }

    static class Section {

        int[] origin;
        int[] shape;

        private Section(int[] origin, int[] shape) {
            this.origin = origin;
            this.shape = shape;
        }
    }

}
