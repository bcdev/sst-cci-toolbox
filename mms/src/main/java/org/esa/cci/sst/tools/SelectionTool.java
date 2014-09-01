package org.esa.cci.sst.tools;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ralf Quast
 */
public class SelectionTool extends BasicTool {

    private String sourceMmdLocation;
    private String targetMmdLocation;

    protected SelectionTool() {
        super("selection-tool", "1.0");
    }

    public static void main(String[] args) {
        final SelectionTool tool = new SelectionTool();
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
    }

    private void run() throws IOException {
        final NetcdfFile sourceMmd = NetcdfDataset.open(sourceMmdLocation);
        try {
            final Dimension matchupDimension = findDimension(sourceMmd, "matchup");
            final int sourceMatchupCount = matchupDimension.getLength();

            final Variable[] zenithAngles = findZenithAngles(sourceMmd);
            final Variable[] brightnessTemperatures = findBrightnessTemperatures(sourceMmd);

            final ArrayList<Integer> sourceMatchupIndexes = new ArrayList<>(sourceMatchupCount);
            for (int i = 0; i < sourceMatchupCount; i++) {
                boolean accepted;

                final Array data1 = readRecord(i, zenithAngles[0]);
                final Array data2 = readRecord(i, zenithAngles[1]);
                accepted = acceptZenithAngles(data1, data2);
                if (accepted) {
                    for (final Variable brightnessTemperature : brightnessTemperatures) {
                        final Array data = readRecord(i, brightnessTemperature);
                        accepted = acceptBrightnessTemperatures(data);
                        if (!accepted) {
                            break;
                        }
                    }
                }
                if (accepted) {
                    sourceMatchupIndexes.add(i);
                }
            }

            final int targetMatchupCount = sourceMatchupIndexes.size();
            final NetcdfFileWriter targetMmd = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4_classic,
                                                                          targetMmdLocation);
            try {
                // copy MMD structure
                for (final Dimension d : sourceMmd.getDimensions()) {
                    if (d.getShortName().equals("matchup")) {
                        targetMmd.addDimension(null, d.getShortName(), targetMatchupCount);
                    } else {
                        targetMmd.addDimension(null, d.getShortName(), d.getLength());
                    }
                }
                final Map<Variable, Variable> mapping = new HashMap<>();
                for (final Variable s : sourceMmd.getVariables()) {
                    final Variable t = targetMmd.addVariable(null,
                                                             s.getShortName(),
                                                             s.getDataType(),
                                                             s.getDimensionsString());
                    for (final Attribute a : s.getAttributes()) {
                        t.addAttribute(a);
                    }
                    mapping.put(t, s);
                }
                for (final Attribute a : sourceMmd.getGlobalAttributes()) {
                    targetMmd.addGroupAttribute(null, a);
                }
                targetMmd.create();
                // copy matchup records
                for (int i = 0; i < targetMatchupCount; i++) {
                    for (final Variable t : mapping.keySet()) {
                        final Array sourceData = readRecord(sourceMatchupIndexes.get(i), mapping.get(t));
                        targetMmd.write(t, createOrigin(i, t.getRank()), sourceData);
                    }
                }
            } finally {
                try {
                    targetMmd.close();
                } catch (IOException ignored) {
                }
            }
        } catch (IOException | InvalidRangeException e) {
            final String message = MessageFormat.format("Failed to write MMD file: {0} ({1})", targetMmdLocation,
                                                        e.getMessage());
            throw new IOException(message, e);
        } finally {
            try {
                sourceMmd.close();
            } catch (IOException ignored) {
            }
        }
    }

    private boolean acceptBrightnessTemperatures(Array data) {
        return false;
    }

    private boolean acceptZenithAngles(Array sza1, Array sza2) {
        return false;
    }


    static Array readRecord(int i, Variable v) throws IOException, InvalidRangeException {
        return v.read(createOrigin(i, v.getRank()), createShape(v));
    }

    static int[] createOrigin(int i, int rank) {
        final int[] origin = new int[rank];
        origin[0] = i;
        return origin;
    }

    static int[] createShape(Variable v) {
        final int[] shape = v.getShape();
        shape[0] = 1;
        return shape;
    }

    static Variable[] findZenithAngles(NetcdfFile file) throws IOException {
        final ArrayList<Variable> variables = new ArrayList<>(2);
        final String part = "satellite_zenith_angle";
        for (final Variable v : file.getVariables()) {
            if (v.getShortName().contains(part) && !v.getShortName().contains("forward")) {
                variables.add(v);
            }
        }
        if (variables.size() == 0) {
            throw new IOException(MessageFormat.format("Could not find variable with name like ''{0}''.", part));
        }
        if (variables.size() == 1) {
            throw new IOException(MessageFormat.format("Expected two variables with name like ''{0}''.", part));
        }
        if (variables.size() > 2) {
            throw new IOException(MessageFormat.format("Too many variables with name like ''{0}''.", part));
        }
        return variables.toArray(new Variable[2]);
    }

    static Variable[] findBrightnessTemperatures(NetcdfFile file) throws IOException {
        final ArrayList<Variable> variables = new ArrayList<>(2);
        final String part = "brightness_temperature";
        for (final Variable v : file.getVariables()) {
            if (v.getShortName().contains(part) && !v.getShortName().contains("forward") && !v.getShortName().contains(
                    "ffm")) {
                variables.add(v);
            }
        }
        if (variables.size() == 0) {
            throw new IOException(MessageFormat.format("Could not find variable with name like ''{0}''.", part));
        }
        return variables.toArray(new Variable[variables.size()]);
    }

    static Dimension findDimension(NetcdfFile file, String name) throws IOException {
        final Dimension d = file.findDimension(name);
        if (d == null) {
            throw new IOException(MessageFormat.format("Could not find dimension ''{0}''.", name));
        }
        return d;
    }

    static Variable findVariable(NetcdfFile file, String name) throws IOException {
        final Variable v = file.findVariable(NetcdfFile.makeValidPathName(name));
        if (v == null) {
            throw new IOException(MessageFormat.format("Could not find variable ''{0}''.", name));
        }
        return v;
    }

}
