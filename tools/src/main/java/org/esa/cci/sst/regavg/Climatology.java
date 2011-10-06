package org.esa.cci.sst.regavg;

import org.esa.cci.sst.tool.ExitCode;
import org.esa.cci.sst.tool.Tool;
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
import java.util.logging.Logger;

/**
 * OSTIA monthly SST climatology.
 *
 * @author Norman Fomferra
 */
public class Climatology {

    private static final GridDef OSTIA_GRID_DEF = GridDef.createGlobal(0.05);
    private static final GridDef GLOBAL_5D_GRID_DEF = GridDef.createGlobal(5.0);
    private static final GridDef GLOBAL_90D_GRID_DEF = GridDef.createGlobal(90.0);

    private static final Logger LOGGER = Tool.LOGGER;

    private final File[] files;
    private final GridDef gridDef;

    private final CachedGrid cachedGrid = new CachedGrid();

    private ArrayGrid waterCoverageGrid5;
    private ArrayGrid waterCoverageGrid90;

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
        if (files.length == 365) {
            File[] sortedFiles = new File[365];
            for (File file : files) {
                int day = Integer.parseInt(file.getName().substring(1, 4));
                sortedFiles[day - 1] = file;
            }
            return new Climatology(sortedFiles, gridDef);
        } else if (files.length == 1) {
            File[] sortedFiles = new File[365];
            for (int i = 0; i < 365; i++) {
                sortedFiles[i] = files[0];
            }
            return new Climatology(sortedFiles, gridDef);
        } else {
            throw new ToolException(String.format("Climatology directory is expected to contain 365 files, but found %d. Missing %s.",
                                                  files.length, Arrays.toString(getMissingDays(files))),
                                    ExitCode.USAGE_ERROR);
        }
    }

    public Grid getAnalysedSstGrid(int dayOfYear) throws IOException {
        synchronized (cachedGrid) {
            if (cachedGrid.dayOfYear != dayOfYear) {
                File file = files[dayOfYear - 1];
                long t0 = System.currentTimeMillis();
                LOGGER.info(String.format("Reading 'analysed_sst' from '%s'...", file.getPath()));
                NetcdfFile netcdfFile = NetcdfFile.open("file:" + file.getPath().replace('\\', '/'));
                try {
                    ArrayGrid sstGrid = NcUtils.readGrid(netcdfFile, "analysed_sst", 0, OSTIA_GRID_DEF);
                    // Flip Y, OSTIA Climatologies are stored upside-down!
                    LOGGER.fine(String.format("Reading 'analysed_sst' took %d ms", System.currentTimeMillis() - t0));
                    t0 = System.currentTimeMillis();
                    sstGrid.flipY();
                    if (!OSTIA_GRID_DEF.equals(gridDef)) {
                        sstGrid = scaleDown(sstGrid, gridDef);
                    }
                    LOGGER.fine(String.format("Transforming 'analysed_sst' took %d ms", System.currentTimeMillis() - t0));
                    cachedGrid.dayOfYear = dayOfYear;
                    cachedGrid.grid = sstGrid;

                    if (waterCoverageGrid5 == null) {
                        t0 = System.currentTimeMillis();
                        LOGGER.info(String.format("Reading 'mask' from '%s'...", file.getPath()));
                        ArrayGrid maskGrid = NcUtils.readGrid(netcdfFile, "mask", 0, OSTIA_GRID_DEF);
                        LOGGER.fine(String.format("Reading 'mask' took %d ms", System.currentTimeMillis() - t0));
                        t0 = System.currentTimeMillis();
                        maskGrid.flipY();
                        ArrayGrid unmaskedGrid = maskGrid.unmask(0x01);
                        waterCoverageGrid5 = scaleDown(unmaskedGrid, GLOBAL_5D_GRID_DEF);
                        waterCoverageGrid90 = scaleDown(waterCoverageGrid5, GLOBAL_90D_GRID_DEF);
                        LOGGER.finest(String.format("waterCoverageGrid5  = %s", waterCoverageGrid5.getArray()));
                        LOGGER.finest(String.format("waterCoverageGrid90 = %s", waterCoverageGrid90.getArray()));
                        LOGGER.fine(String.format("Transforming 'mask' took %d ms", System.currentTimeMillis() - t0));
                    }

                } finally {
                    netcdfFile.close();
                }
            }
            return cachedGrid.grid;
        }
    }

    public Grid getWaterCoverageGrid5() {
        return waterCoverageGrid5;
    }

    public Grid getWaterCoverageGrid90() {
        return waterCoverageGrid90;
    }

    private static ArrayGrid scaleDown(ArrayGrid grid, GridDef targetGridDef) {
        int sourceWidth = grid.getWidth();
        int sourceHeight = grid.getHeight();
        int targetWidth = targetGridDef.getWidth();
        int targetHeight = targetGridDef.getHeight();
        int scaleX = sourceWidth / targetWidth;
        int scaleY = sourceHeight / targetHeight;
        if (scaleX == 0 || scaleX * targetWidth != sourceWidth
                || scaleY == 0 || scaleY * targetHeight != sourceHeight) {
            throw new IllegalStateException(String.format("Climatology grid cannot be adapted scaled to %d x %d cells.", targetWidth, targetHeight));
        }
        LOGGER.fine(String.format("Scaling climatology grid from %dx%d down to %dx%d cells...",
                                  grid.getWidth(), grid.getHeight(),
                                  targetGridDef.getWidth(), targetGridDef.getHeight()));
        long t0 = System.currentTimeMillis();
        grid = grid.scaleDown(scaleX, scaleY);
        LOGGER.fine(String.format("Scaling took %d ms", System.currentTimeMillis() - t0));
        return grid;
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
