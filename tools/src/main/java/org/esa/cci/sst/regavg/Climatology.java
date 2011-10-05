package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.GridDef;
import org.esa.cci.sst.util.UTC;
import org.esa.cci.sst.tool.ExitCode;
import org.esa.cci.sst.tool.ToolException;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

/**
 * OSTIA monthly SST climatology.
 *
 * @author Norman Fomferra
 */
public class Climatology {

    private final static GridDef GRID_DEF = GridDef.createGlobalGrid(0.05);
    private final Array[] analysedSsts;

    public static Climatology open(File dir, GridDef targetGridDef) throws ToolException {
        if (!dir.isDirectory()) {
            throw new ToolException("Not a directory or directory not found: " + dir, ExitCode.USAGE_ERROR);
        }
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".nc");
            }
        });
        if (files == null) {
            throw new ToolException("Climatology directory is empty: " + dir, ExitCode.USAGE_ERROR);
        }
        // todo - take this out (only for Norman's notebook)
        if (files.length == 1) {
            return createStupidOneFileClimatology(files);
        }
        if (files.length != 365) {
            throw new ToolException(String.format("Climatology directory is expected to contain 365 files, but found %d. Missing %s.",
                                                files.length, Arrays.toString(getMissingDays(files))),
                                    ExitCode.USAGE_ERROR);
        }
        try {
            Array[] arrays = new Array[365];
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
                try {
                    Array array = readAnalysedSst(netcdfFile);
                    if (!GRID_DEF.equals(targetGridDef)) {
                       array = resample(array, GRID_DEF, targetGridDef);
                    }
                    arrays[i] = array;
                } finally {
                    netcdfFile.close();
                }
            }
            return new Climatology(arrays);
        } catch (IOException e) {
            throw new ToolException("Failed to load climatology: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }
    }

    private static Array resample(Array array, GridDef sourceGridDef, GridDef targetGridDef) {
        int sourceWidth = sourceGridDef.getWidth();
        int sourceHeight = sourceGridDef.getHeight();
        int targetWidth = targetGridDef.getWidth();
        int targetHeight = targetGridDef.getHeight();

        Array resampledArray = Array.factory(DataType.FLOAT, new int[]{targetHeight, targetWidth});
        // todo
        return null;
    }

    private static Climatology createStupidOneFileClimatology(File[] files) throws ToolException {
        try {
            NetcdfFile netcdfFile = NetcdfFile.open(files[0].getPath());
            Array analysedSst = readAnalysedSst(netcdfFile);
            Array[] arrays = new Array[365];
            for (int i = 0; i < arrays.length; i++) {
                arrays[i] = analysedSst;
            }
            return new Climatology(arrays);
        } catch (IOException e) {
            throw new ToolException("Failed to load climatology: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }
    }

    private static Array readAnalysedSst(NetcdfFile netcdfFile) throws IOException {
        try {
            return netcdfFile.findTopVariable("analysed_sst").read(new int[]{0, 0, 0},
                                                                   new int[]{1, GRID_DEF.getHeight(), GRID_DEF.getWidth()});
        } catch (InvalidRangeException e) {
            throw new IllegalStateException(e);
        }
    }

    public Array getAnalysedSst(int day) {
        return analysedSsts[day];
    }

    private static String[] getMissingDays(File[] files) {
        Set<String> missing = new HashSet<String>();
        for (int i = 0; i < 365; i++) {
            missing.add(String.format("D%03d", i + 1));
        }
        for (File file : files) {
            missing.remove(file.getName().substring(0, 4));
        }
        String[] strings = missing.toArray(new String[missing.size()]);
        Arrays.sort(strings);
        return strings;
    }


    public Climatology(Array[] analysedSsts) {
        if (analysedSsts.length != 365) {
            throw new IllegalArgumentException("datasets.length != 365");
        }
        this.analysedSsts = analysedSsts;
    }

    /**
     * @param utc The UTC time
     * @return array of the form {day-of-year-1, day-of-year-2, millis-of-day}
     */
    public static int[] getDayOfYearRange(Date utc) {
        final Calendar calendar = UTC.createCalendar(utc);
        final int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        final long time = calendar.getTimeInMillis();
        final long dayMillis = 24 * 60 * 60 * 1000L;
        final long millisOfDay = time % dayMillis;
        final int dayOfYear1;
        final int dayOfYear2;
        // last day in a usual or leap year
        if (dayOfYear == 365 || dayOfYear == 366) {
            dayOfYear1 = 365;
            dayOfYear2 = 1;
        } else {
            dayOfYear1 = dayOfYear;
            dayOfYear2 = dayOfYear + 1;
        }
        return new int[]{dayOfYear1, dayOfYear2, (int) millisOfDay};
    }

    public static class Dataset {
        private final NetcdfFile file;
        private final int time;
        private final Map<String,Variable> variableMap;

        public Dataset(NetcdfFile file) throws IOException {
            this.file = file;
            List<Variable> variables = file.getVariables();
            variableMap = new HashMap<String, Variable>();
            for (Variable variable : variables) {
                String name = variable.getName();
                variableMap.put(name, variable);
            }
            Variable timeVar = variableMap.get("time");
            if (timeVar == null) {
                throw new IOException("Illegal climatology file: Missing variable 'time'.");
            }
            time = timeVar.readScalarInt();
        }

        public NetcdfFile getFile() {
            return file;
        }

        public int getTime() {
            return time;
        }
    }
}
