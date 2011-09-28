package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.UTC;
import org.esa.cci.sst.tool.ExitCode;
import org.esa.cci.sst.tool.ToolException;
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

    private final File dir;
    private final Dataset[] datasets;

    public static Climatology open(File dir) throws ToolException {
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
            return createStupidOneFileClimatology(dir, files);
        }
        if (files.length != 365) {
            throw new ToolException(String.format("Climatology directory is expected to contain 365 files, but found %d. Missing %s.",
                                                files.length, Arrays.toString(getMissingDays(files))),
                                    ExitCode.USAGE_ERROR);
        }
        try {
            Dataset[] datasets = new Dataset[365];
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
                datasets[i] = new Dataset(netcdfFile);
            }
            return new Climatology(dir, datasets);
        } catch (IOException e) {
            throw new ToolException("Failed to load climatology: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }
    }

    private static Climatology createStupidOneFileClimatology(File dir, File[] files) throws ToolException {
        try {
            NetcdfFile netcdfFile = NetcdfFile.open(files[0].getPath());
            Dataset[] datasets = new Dataset[365];
            for (int i = 0; i < datasets.length; i++) {
                datasets[i] = new Dataset(netcdfFile);
            }
            return new Climatology(dir, datasets);
        } catch (IOException e) {
            throw new ToolException("Failed to load climatology: " + e.getMessage(), e, ExitCode.IO_ERROR);
        }
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


    private Climatology(File dir, Dataset[] datasets) {
        this.dir = dir;
        if (datasets.length != 365) {
            throw new IllegalArgumentException("datasets.length != 365");
        }
        this.datasets = datasets;
    }


    public Dataset[] getDatasets() {
        return datasets;
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
