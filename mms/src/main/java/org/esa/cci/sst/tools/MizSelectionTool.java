/*
 * Copyright (c) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools;

import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.util.NetCDFUtil;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.esa.cci.sst.tools.Constants.ATTRIBUTE_CREATION_DATE_NAME;
import static org.esa.cci.sst.tools.Constants.ATTRIBUTE_NUM_MATCHUPS_NAME;

/**
 * Selects matchup records located in the marginal ice zone (MIZ)
 *
 * @author Ralf Quast
 */
public class MizSelectionTool extends BasicTool {

    private String sourceMmdLocation;
    private String targetMmdLocation;
    private String referenceSensor;

    protected MizSelectionTool() {
        super("miz-selection-tool", "1.0");
    }

    public static void main(String[] args) {
        final MizSelectionTool tool = new MizSelectionTool();
        try {
            if (!tool.setCommandLineArgs(args)) {
                tool.printHelp();
                return;
            }
            tool.initialize();
            tool.run();
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Exception e) {
            tool.getErrorHandler().terminate(new ToolException(e.getMessage(), e, ToolException.UNKNOWN_ERROR));
        }
    }

    @Override
    public void initialize() {
        // no database functions needed, therefore don't call
        // super.initialize();

        final Configuration config = getConfig();

        sourceMmdLocation = config.getStringValue(Configuration.KEY_MMS_SELECTION_MMD_SOURCE);
        targetMmdLocation = config.getStringValue(Configuration.KEY_MMS_SELECTION_MMD_TARGET);
        final String[] sensors = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR).split(",", 2);
        referenceSensor = sensors[0];
    }

    private void run() throws IOException, InvalidRangeException {

        final NetcdfFile sourceMmd = NetcdfFile.open(sourceMmdLocation);
        final NetcdfFileWriter targetMmd = createTargetMmdWriter();

        try {
            final Array lonArray = NetCDFUtil.findVariable(sourceMmd, referenceSensor + ".longitude").read();
            final Array latArray = NetCDFUtil.findVariable(sourceMmd, referenceSensor + ".latitude").read();

            final int[] shape = lonArray.getShape();
            final int numMatches = shape[0];
            final int height = shape[1];
            final int width = shape[2];

            final CircularExtractMask extractMask = new CircularExtractMask(width, height, 50.0, 1.1);

            final List<Integer> matchIndexToKeep = new ArrayList<>();
            for (int matchup = 0; matchup < numMatches; matchup++) {
                // read ice-data for matches
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        final boolean insideCircle = extractMask.getValue(x, y);
                        // use mask to detect keep/remove matchups, remember indices to keep
                    }
                }

                // ---- remove this later ------
                // just a simple fake code to randomly narrow down the matchups - tb 2017-08-24
                final double random = Math.random();
                if (random > 0.7) {
                    matchIndexToKeep.add(matchup);
                }
                // ---- remove this later ------
            }

            createTargetMMD(sourceMmd, targetMmd, matchIndexToKeep);

            transferSelectedData(sourceMmd, targetMmd, matchIndexToKeep);

        /*
        TODO - implement what is defined below: include only those sub-scenes that satisfy both 1) and 2)

        So Ralf, our definition of the marginal ice zone is:

        1) No pixel within the central 101 pixel square exhibits a sea ice fraction greater than zero
        2) Any pixel within the 141 pixel square (but not within the central 101 pixel square) exhibits a sea ice fraction greater than 15%

        Regards,

        Kevin
         */

        } finally {
            sourceMmd.close();
            targetMmd.close();
        }
    }

    private void transferSelectedData(NetcdfFile sourceMmd, NetcdfFileWriter targetMmd, List<Integer> matchIndexToKeep) throws IOException, InvalidRangeException {
        final List<Variable> variables = sourceMmd.getVariables();
        for (final Variable variable : variables) {
            final int[] writeShape = variable.getShape();
            writeShape[0] = 1;

            final int[] origin = new int[writeShape.length];
            for (int i = 0; i < writeShape.length; i++) {
                origin[i] = 0;
            }

            final Variable targetVariable = targetMmd.findVariable(variable.getFullNameEscaped());
            int writeIndex = 0;
            for (Integer matchup : matchIndexToKeep) {
                origin[0] = matchup;
                final Array data = variable.read(origin, writeShape);

                origin[0] = writeIndex;
                targetMmd.write(targetVariable, origin, data);
                ++writeIndex;
            }
        }
    }

    private void createTargetMMD(NetcdfFile sourceMmd, NetcdfFileWriter targetMmd, List<Integer> matchIndexToKeep) throws IOException {
        final Group rootGroup = sourceMmd.getRootGroup();
        copyHeader(targetMmd, rootGroup, null, matchIndexToKeep.size());
        targetMmd.create();
    }

    private NetcdfFileWriter createTargetMmdWriter() throws IOException {
        final NetcdfFileWriter targetMmd = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, targetMmdLocation);
        targetMmd.setLargeFile(true);
        targetMmd.setFill(true);
        return targetMmd;
    }

    private void copyHeader(NetcdfFileWriter targetMmd, Group group, Group parent, int numMatchups) {
        final Group addedGroup = targetMmd.addGroup(parent, group.getShortName());

        for (Attribute attribute : group.getAttributes()) {
            if (attribute.getShortName().equals(ATTRIBUTE_NUM_MATCHUPS_NAME)) {
                attribute = new Attribute(ATTRIBUTE_NUM_MATCHUPS_NAME, numMatchups);
            }

            if (attribute.getShortName().equals(ATTRIBUTE_CREATION_DATE_NAME)) {
                attribute = new Attribute(ATTRIBUTE_CREATION_DATE_NAME, Calendar.getInstance().getTime().toString());
            }
            addedGroup.addAttribute(attribute);
        }

        for (Dimension dim : group.getDimensions()) {
            int length = dim.getLength();

            final String dimensionName = dim.getShortName();
            if (dimensionName.equals("matchup")) {
                length = numMatchups;
            }

            targetMmd.addDimension(addedGroup, dimensionName, length, true, dim.isUnlimited(), dim.isVariableLength());
        }

        for (Variable v : group.getVariables()) {
            final String shortName = v.getShortName();
            final Variable nv = targetMmd.addVariable(addedGroup, shortName, v.getDataType(), v.getDimensionsString());
            for (Attribute att : v.getAttributes()) {
                targetMmd.addVariableAttribute(nv, att);
            }
        }

        // recurse
        for (Group g : group.getGroups()) {
            copyHeader(targetMmd, g, addedGroup, numMatchups);
        }
    }
}
