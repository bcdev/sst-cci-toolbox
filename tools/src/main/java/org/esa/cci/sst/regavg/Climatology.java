package org.esa.cci.sst.regavg;

import org.esa.cci.sst.tool.ExitCode;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.util.ArrayGrid;
import org.esa.cci.sst.util.Grid;
import org.esa.cci.sst.util.GridDef;
import org.esa.cci.sst.util.NcUtils;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * OSTIA monthly SST climatology.
 *
 * @author Norman Fomferra
 */
public class Climatology {

    private final static GridDef SOURCE_GRID_DEF = GridDef.createGlobalGrid(0.05);

    private final File[] files;
    private final GridDef gridDef;

    private final CachedGrid cachedGrid = new CachedGrid();

    private static class CachedGrid {
        private int dayOfYear;
        private Grid grid;
    }

    public static Climatology create(File dir, GridDef gridDef) throws ToolException {
        if (!dir.isDirectory()) {
            throw new ToolException("Not a directory or directory not found: " + dir, ExitCode.USAGE_ERROR);
        }
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("D") && name.endsWith(".nc");
            }
        });
        if (files == null) {
            throw new ToolException("Climatology directory is empty: " + dir, ExitCode.USAGE_ERROR);
        }
        if (files.length != 365) {
            throw new ToolException(String.format("Climatology directory is expected to contain 365 files, but found %d. Missing %s.",
                                                  files.length, Arrays.toString(getMissingDays(files))),
                                    ExitCode.USAGE_ERROR);
        }
        File[] sortedFiles = new File[365];
        for (File file : files) {
            int day = Integer.parseInt(file.getName().substring(1, 4));
            sortedFiles[day - 1] = file;
        }
        return new Climatology(sortedFiles, gridDef);
    }

    private static ArrayGrid readAnalysedSstGrid(NetcdfFile netcdfFile) throws IOException {
        return NcUtils.readGrid(netcdfFile, "analysed_sst", 0, SOURCE_GRID_DEF);
    }

    /**
     * @param dayOfYear The day of the year starting from 1.
     * @return
     * @throws IOException
     */
    public Grid getAnalysedSstGrid(int dayOfYear) throws IOException {
        synchronized (cachedGrid) {
            if (cachedGrid.dayOfYear != dayOfYear) {
                File file = files[dayOfYear - 1];
                System.out.printf("Reading climatology from '%s'...\n", file.getPath());
                NetcdfFile netcdfFile = NetcdfFile.open("file:" + file.getPath().replace('\\', '/'));
                try {
                    ArrayGrid grid = NcUtils.readGrid(netcdfFile, "analysed_sst", 0, SOURCE_GRID_DEF);
                    if (!SOURCE_GRID_DEF.equals(gridDef)) {
                        System.out.printf("Resampling climatology grid from %d x %d --> %d x %d...\n",
                                          SOURCE_GRID_DEF.getWidth(), SOURCE_GRID_DEF.getHeight(),
                                          gridDef.getWidth(), gridDef.getHeight());
                        grid = grid.resample(gridDef);
                    }
                    cachedGrid.dayOfYear = dayOfYear;
                    cachedGrid.grid = grid;
                } finally {
                    netcdfFile.close();
                }
            }
            return cachedGrid.grid;
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


    private Climatology(File[] files, GridDef gridDef) {
        if (files.length != 365) {
            throw new IllegalArgumentException("files.length != 365");
        }
        this.files = files;
        this.gridDef = gridDef;
    }
}
