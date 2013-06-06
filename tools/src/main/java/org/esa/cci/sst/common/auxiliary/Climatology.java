/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.common.auxiliary;

import org.esa.cci.sst.common.cellgrid.Downscaling;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.Mask;
import org.esa.cci.sst.common.cellgrid.YFlip;
import org.esa.cci.sst.tool.ExitCode;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.util.NcUtils;
import ucar.nc2.NetcdfFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OSTIA monthly SST climatology.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public class Climatology {

    private static final GridDef SOURCE_GRID_DEF = GridDef.createGlobal(0.05);
    private static final GridDef TARGET_5D_GRID_DEF = GridDef.createGlobal(5.0);
    private static final GridDef TARGET_90D_GRID_DEF = GridDef.createGlobal(90.0);

    private static final Logger LOGGER = Logger.getLogger("org.esa.cci.sst");

    private final File[] dailyClimatologyFiles;
    private final GridDef targetGridDef;

    private Grid sstGrid;
    private int dayOfYear;

    private Grid seaCoverageGrid; // 0.1° or 0.05° same as input files
    private Grid seaCoverageCell5Grid;
    private Grid seaCoverageCell90Grid;

    private Climatology(File[] dailyClimatologyFiles, GridDef targetGridDef) {
        this.dailyClimatologyFiles = dailyClimatologyFiles;
        this.targetGridDef = targetGridDef;
    }

    public static Climatology create(File dir, GridDef targetGridDef) throws ToolException {
        if (!dir.isDirectory()) {
            throw new ToolException("Not a directory or directory not found: " + dir, ExitCode.USAGE_ERROR);
        }
        final File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(".*\\d\\d\\d.*\\.nc");
            }
        });
        if (files == null) {
            throw new ToolException(String.format("Climatology directory is empty: %s", dir), ExitCode.USAGE_ERROR);
        }
        if (files.length == 365 || files.length == 366) {
            final File[] dailyClimatologyFiles = new File[files.length];
            final Pattern pattern = Pattern.compile("\\d\\d\\d");
            for (final File file : files) {
                final Matcher matcher = pattern.matcher(file.getName());
                if (matcher.find()) {
                    final int day = Integer.parseInt(file.getName().substring(matcher.start(), matcher.end()));
                    dailyClimatologyFiles[day - 1] = file;
                } else {
                    throw new ToolException("An internal error occurred.", ExitCode.INTERNAL_ERROR);
                }
            }
            return new Climatology(dailyClimatologyFiles, targetGridDef);
        } else if (files.length == 1) {
            final File[] dailyClimatologyFiles = new File[]{files[0]};
            return new Climatology(dailyClimatologyFiles, targetGridDef);
        } else {
            final String[] missingDays = getMissingDays(files);
            throw new ToolException(
                    String.format(
                            "Climatology directory is expected to contain 365 or 366 files, but found %d. Missing %s.",
                            files.length, Arrays.toString(missingDays)), ExitCode.USAGE_ERROR);
        }
    }

    public Grid getSst(int dayOfYear) throws IOException {
        synchronized (this) {
            if (this.dayOfYear != dayOfYear) {
                readGrids(dayOfYear);
            }
            return sstGrid;
        }
    }

    public Grid getSeaCoverage() {
        return seaCoverageGrid;
    }

    public Grid getSeaCoverageGrid5() {
        return seaCoverageCell5Grid;
    }

    public Grid getSeaCoverageGrid90() {
        return seaCoverageCell90Grid;
    }

    private void readGrids(int dayOfYear) throws IOException {
        if (dayOfYear < 1) {
            throw new IllegalArgumentException("dayOfYear < 1");
        } else if (dayOfYear > 366) {
            throw new IllegalArgumentException("dayOfYear > 366");
        }
        if (dayOfYear > dailyClimatologyFiles.length) {
            dayOfYear = dailyClimatologyFiles.length;
        }
        final File file = dailyClimatologyFiles[dayOfYear - 1];
        long t0 = System.currentTimeMillis();
        LOGGER.info(
                String.format("Processing input climatology file '%s' for day of year %d", file.getPath(), dayOfYear));
        final NetcdfFile netcdfFile = NetcdfFile.open("file:" + file.getPath().replace('\\', '/'));
        try {
            readGrids(netcdfFile, dayOfYear);
        } finally {
            netcdfFile.close();
        }
        LOGGER.fine(String.format("Processing input climatology file took %d ms", System.currentTimeMillis() - t0));
    }

    private void readGrids(NetcdfFile netcdfFile, int dayOfYear) throws IOException {
        readAnalysedSstGrid(netcdfFile, dayOfYear);
        if (seaCoverageGrid == null) {
            readSeaCoverageGrids(netcdfFile);
            // TODO - remove
            writeMaskImage();
        }
    }

    private void readAnalysedSstGrid(NetcdfFile netcdfFile, int dayOfYear) throws IOException {
        long t0 = System.currentTimeMillis();
        LOGGER.fine("Reading 'analysed_sst'...");
        // TODO - use different SST name for new climatology files
        Grid sstGrid = NcUtils.readGrid(netcdfFile, "analysed_sst", SOURCE_GRID_DEF, 0);
        LOGGER.fine(String.format("Reading 'analysed_sst' took %d ms", System.currentTimeMillis() - t0));
        t0 = System.currentTimeMillis();
        if (!SOURCE_GRID_DEF.equals(targetGridDef)) {
            sstGrid = Downscaling.create(sstGrid, targetGridDef);
        }
        LOGGER.fine(String.format("Transforming 'analysed_sst' took %d ms", System.currentTimeMillis() - t0));
        this.sstGrid = YFlip.create(sstGrid);
        this.dayOfYear = dayOfYear;
    }

    private void readSeaCoverageGrids(NetcdfFile netcdfFile) throws IOException {
        long t0 = System.currentTimeMillis();
        LOGGER.fine("Reading 'mask'...");
        // TODO - create sea coverage grid from SST for new climatology files
        final Grid maskGrid = NcUtils.readGrid(netcdfFile, "mask", SOURCE_GRID_DEF, 0);
        LOGGER.fine(String.format("Reading 'mask' took %d ms", System.currentTimeMillis() - t0));
        t0 = System.currentTimeMillis();
        seaCoverageGrid = YFlip.create(Mask.create(maskGrid, 0x01));
        if (!SOURCE_GRID_DEF.equals(targetGridDef)) {
            seaCoverageGrid = Downscaling.create(seaCoverageGrid, targetGridDef);
        }
        seaCoverageCell5Grid = Downscaling.create(seaCoverageGrid, TARGET_5D_GRID_DEF);
        seaCoverageCell90Grid = Downscaling.create(seaCoverageCell5Grid, TARGET_90D_GRID_DEF);
        LOGGER.fine(String.format("Transforming 'mask' took %d ms", System.currentTimeMillis() - t0));
    }

    private static String[] getMissingDays(File[] files) {
        final Set<String> missing = new HashSet<String>();
        for (int i = 0; i < 365; i++) {
            missing.add(String.format("D%03d", i + 1));
        }
        for (File file : files) {
            missing.remove(file.getName().substring(0, 4));
        }
        final String[] strings = missing.toArray(new String[missing.size()]);
        Arrays.sort(strings);
        return strings;
    }

    private void writeMaskImage() throws IOException {
        final IndexColorModel colorModel = new IndexColorModel(8, 2, new byte[]{0, (byte) 255},
                                                               new byte[]{0, (byte) 255}, new byte[]{0, (byte) 255});
        final int w = seaCoverageGrid.getGridDef().getWidth();
        final int h = seaCoverageGrid.getGridDef().getHeight();
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
        final byte[] src = new byte[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                src[y * w + x] = (byte) seaCoverageGrid.getSampleInt(x, y);
            }
        }
        final byte[] dest = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(src, 0, dest, 0, image.getWidth() * image.getHeight());
        ImageIO.write(image, "PNG", new File("sea-coverage-grid.png"));
    }
}
