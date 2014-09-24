package org.esa.cci.sst.tools;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Selects dual-sensor matchup records that satisfy the following criteria:
 * <p/>
 * 1. The difference in view zenith angle between matching sub-scenes is less than 10°
 * 2. The standard deviation of brightness temperatures in sub-scenes is less than 2° K
 *
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
        final NetcdfFile sourceMmd = NetcdfFile.open(sourceMmdLocation);
        try {
            final Dimension matchupDimension = findDimension(sourceMmd, "matchup");
            final int sourceMatchupCount = matchupDimension.getLength();

            final Variable[] zenithAngles = findViewZenithAngles(sourceMmd);
            final Variable[] brightnessTemperatures = findBrightnessTemperatures(sourceMmd);
            final Variable[] nadirBrightnessTemperatures = findNadirBrightnessTemperatures(sourceMmd);
            final Variable[] forwardBrightnessTemperatures = findForwardBrightnessTemperatures(sourceMmd);

            final ArrayList<Integer> sourceMatchupIndexes = new ArrayList<>(sourceMatchupCount);
            for (int i = 0; i < sourceMatchupCount; i++) {
                boolean accepted;

                final double[] vza1 = readRecordEnhanced(i, zenithAngles[0]);
                final double[] vza2 = readRecordEnhanced(i, zenithAngles[1]);
                accepted = acceptZenithAngles(vza1, vza2);
                if (accepted) {
                    for (final Variable brightnessTemperature : brightnessTemperatures) {
                        final double[] data = readRecordEnhanced(i, brightnessTemperature);
                        accepted = acceptBrightnessTemperatures(data);
                        if (!accepted) {
                            break;
                        }
                    }
                }
                if (accepted) {
                    for (final Variable brightnessTemperature : nadirBrightnessTemperatures) {
                        final double[] data = readRecordEnhanced(i, brightnessTemperature);
                        accepted = acceptBrightnessTemperatures(data);
                        if (!accepted) {
                            break;
                        }
                    }
                    if (!accepted) {
                        for (final Variable brightnessTemperature : forwardBrightnessTemperatures) {
                            final double[] data = readRecordEnhanced(i, brightnessTemperature);
                            accepted = acceptBrightnessTemperatures(data);
                            if (!accepted) {
                                break;
                            }
                        }
                    }
                }
                if (accepted) {
                    sourceMatchupIndexes.add(i);
                }
            }

            final int targetMatchupCount = sourceMatchupIndexes.size();
            final NetcdfFileWriter targetMmd = createNew(targetMmdLocation);
            try {
                // copy MMD structure
                for (final Dimension d : sourceMmd.getDimensions()) {
                    if (d.getShortName().equals("matchup")) {
                        if (targetMatchupCount > 0) {
                            targetMmd.addDimension(null, d.getShortName(), targetMatchupCount);
                        } else {
                            targetMmd.addUnlimitedDimension(d.getShortName());
                        }
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
                        final Variable s = mapping.get(t);
                        final Array sourceData = readRecord(sourceMatchupIndexes.get(i), s);
                        targetMmd.write(t, createSingleRecordOrigin(i, t.getRank()), sourceData);
                    }
                }
                mapping.clear();
            } finally {
                try {
                    targetMmd.close();
                } catch (Throwable ignored) {
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

    static NetcdfFileWriter createNew(String path) throws IOException {
        final File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        return NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4_classic, file.getPath());
    }

    boolean acceptBrightnessTemperatures(double[] data) {
        return variance(data) < 4.0;
    }

    static double variance(double[] data) {
        int n = 0;
        double mean = 0.0;
        double m2 = 0.0;

        for (final double value : data) {
            if (!Double.isNaN(value)) {
                final double delta = value - mean;
                n++;
                mean = mean + delta / n;
                m2 = m2 + delta * (value - mean);
            }
        }
        if (n < 2) {
            return 0.0;
        }

        return m2 / (n - 1);
    }

    static boolean acceptZenithAngles(double[] vza1, double[] vza2) {
        final int i1 = vza1.length / 2;
        final int i2 = vza2.length / 2;

        return Math.abs(vza1[i1] - vza2[i2]) < 10.0;
    }


    static Array readRecord(int i, Variable v) throws IOException, InvalidRangeException {
        return v.read(createSingleRecordOrigin(i, v.getRank()), createSingleRecordShape(v));
    }

    private double[] readRecordEnhanced(int i, Variable v) throws IOException, InvalidRangeException {
        final Array data = readRecord(i, v);
        final Array enhancedData = Array.factory(DataType.DOUBLE, data.getShape());
        final double addOffset = getAttribute(v, "add_offset", 0.0);
        final double scaleFactor = getAttribute(v, "scale_factor", 0.0);
        final Number fillValue = getAttribute(v, "_FillValue");
        for (int k = 0; k < data.getSize(); ++k) {
            final double value = data.getDouble(k);
            if (fillValue == null || value != fillValue.doubleValue()) {
                enhancedData.setDouble(k, scaleFactor * value + addOffset);
            } else {
                enhancedData.setDouble(k, Double.NaN);
            }
        }
        return (double[]) enhancedData.getStorage();
    }

    private static Number getAttribute(Variable v, String name) {
        final Attribute attribute = v.findAttribute(name);
        if (attribute == null) {
            return null;
        }
        return attribute.getNumericValue();
    }

    private static double getAttribute(Variable v, String name, double defaultValue) {
        final Attribute attribute = v.findAttribute(name);
        if (attribute == null) {
            return defaultValue;
        }
        return attribute.getNumericValue().doubleValue();
    }

    static int[] createSingleRecordOrigin(int i, int rank) {
        final int[] origin = new int[rank];
        origin[0] = i;
        return origin;
    }

    static int[] createSingleRecordShape(Variable v) {
        final int[] shape = v.getShape();
        shape[0] = 1;
        return shape;
    }

    static Variable[] findBrightnessTemperatures(NetcdfFile file) throws IOException {
        final ArrayList<Variable> variables = new ArrayList<>(2);
        final String part = "brightness_temperature";
        for (final Variable v : file.getVariables()) {
            final String name = v.getShortName();
            if (name.contains(part) && !name.contains("nadir") && !name.contains("forward") && !name.contains("ffm")) {
                variables.add(v);
            }
        }
        if (variables.size() == 0) {
            throw new IOException(MessageFormat.format("Could not find variables with name like ''{0}''.", part));
        }
        return variables.toArray(new Variable[variables.size()]);
    }

    static Variable[] findNadirBrightnessTemperatures(NetcdfFile file) throws IOException {
        final ArrayList<Variable> variables = new ArrayList<>(2);
        final String part = "brightness_temperature";
        for (final Variable v : file.getVariables()) {
            final String name = v.getShortName();
            if (name.contains(part) && name.contains("nadir") && !name.contains("ffm")) {
                variables.add(v);
            }
        }
        return variables.toArray(new Variable[variables.size()]);
    }

    static Variable[] findForwardBrightnessTemperatures(NetcdfFile file) throws IOException {
        final ArrayList<Variable> variables = new ArrayList<>(2);
        final String part = "brightness_temperature";
        for (final Variable v : file.getVariables()) {
            final String name = v.getShortName();
            if (name.contains(part) && name.contains("forward") && !name.contains("ffm")) {
                variables.add(v);
            }
        }
        return variables.toArray(new Variable[variables.size()]);
    }

    static Variable[] findViewZenithAngles(NetcdfFile file) throws IOException {
        final ArrayList<Variable> variables = new ArrayList<>(2);
        final String part = "satellite_zenith_angle";
        for (final Variable v : file.getVariables()) {
            if (v.getShortName().contains(part) && !v.getShortName().contains("forward")) {
                variables.add(v);
            }
        }
        if (variables.size() == 0) {
            throw new IOException(MessageFormat.format("Could not find variables with name like ''{0}''.", part));
        }
        if (variables.size() == 1) {
            throw new IOException(MessageFormat.format("Expected two variables with name like ''{0}''.", part));
        }
        if (variables.size() > 2) {
            throw new IOException(MessageFormat.format("Too many variables with name like ''{0}''.", part));
        }
        return variables.toArray(new Variable[2]);
    }

    static Dimension findDimension(NetcdfFile file, String name) throws IOException {
        final Dimension d = file.findDimension(name);
        if (d == null) {
            throw new IOException(MessageFormat.format("Could not find dimension ''{0}''.", name));
        }
        return d;
    }

}
