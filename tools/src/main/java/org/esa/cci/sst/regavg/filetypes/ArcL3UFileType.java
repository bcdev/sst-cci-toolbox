package org.esa.cci.sst.regavg.filetypes;

import org.esa.cci.sst.regavg.FileType;
import org.esa.cci.sst.regavg.ProcessingLevel;
import org.esa.cci.sst.regavg.SstDepth;
import org.esa.cci.sst.regavg.VariableType;
import org.esa.cci.sst.util.*;
import org.esa.cci.sst.util.accumulators.RandomUncertaintyAccumulator;
import org.esa.cci.sst.util.accumulators.WeightedMeanAccumulator;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static java.lang.Math.round;

/**
 * Represents the ARC_L3U file type.
 * <p/>
 * The filename regex pattern is <code>AT[12S]_AVG_3PAARC\d{8}_[DTEM]_[nd][ND][23][bms][.]nc[.]gz</code>
 * with
 * <p/>
 * AT[12S] = ATSR1, ATSR2, AATSR<br/>
 * \d{8} = date in the format YYYYMMDD <br/>
 * [DTEM] = daily, ?, ?, monthly
 * [nd] = night or day<br/>
 * [ND] = Nadir or Dual view<br/>
 * [23] = 2 or 3 channel retrieval (3 channel only valid during night)<br/>
 * [bms] = bayes, min-bayes, SADIST cloud screening<br/>
 * <p/>
 * Find more info in the <a href="https://www.wiki.ed.ac.uk/display/arcwiki/Test+Data#TestData-NetCDFDataFiles">arcwiki</a>.
 *
 * @author Norman Fomferra
 */
public class ArcL3UFileType implements FileType {

    public final static ArcL3UFileType INSTANCE = new ArcL3UFileType();
    public final DateFormat dateFormat = UTC.getDateFormat("yyyyMMdd");
    public final int filenameDateOffset = "ATS_AVG_3PAARC".length();
    public final GridDef gridDef = GridDef.createGlobalGrid(3600, 1800);

    @Override
    public Date parseDate(File file) throws ParseException {
        String dateString = file.getName().substring(filenameDateOffset, filenameDateOffset + 8);
        return dateFormat.parse(dateString);
    }

    @Override
    public String getFilenameRegex() {
        return "AT[12S]_AVG_3PAARC\\d{8}_[DTEM]_[nd][ND][23][bms][.]nc[.]gz";
    }

    @Override
    public GridDef getGridDef() {
        return gridDef;
    }

    @Override
    public ProcessingLevel getProcessingLevel() {
        return ProcessingLevel.L3U;
    }

    @Override
    public Date readDate(NetcdfFile file) throws IOException {
        Variable variable = file.findTopVariable("time");
        if (variable == null) {
            throw new IOException("Missing variable 'time' in file '" + file.getLocation() + "'");
        }
        // The time of ARC files is encoded as seconds since 01.01.1981
        int secondsSince1981 = round(variable.readScalarFloat());
        Calendar calendar = UTC.createCalendar(1981);
        calendar.add(Calendar.SECOND, secondsSince1981);
        return calendar.getTime();
    }

    @Override
    public VariableType[] getVariableTypes(SstDepth sstDepth) {
        if (sstDepth == SstDepth.depth_20) {
            return new VariableType[]{
                    new SstDepth20VariableType(),
                    new UncertaintyVariableType(),
            };
        } else if (sstDepth == SstDepth.depth_100) {
            return new VariableType[]{
                    new SstDepth100VariableType(),
                    new UncertaintyVariableType(),
            };
        } else /*if (sstDepth == SstDepth.skin)*/ {
            return new VariableType[]{
                    new SstSkinVariableType(),
                    new UncertaintyVariableType(),
            };
        }

    }

    @Override
    public Grid[] readGrids(NetcdfFile file, SstDepth sstDepth) throws IOException {
        Grid[] grids = new Grid[2];
        if (sstDepth == SstDepth.depth_20) {
            grids[0] = NcUtils.readGrid(file, "sst_depth", getGridDef(), 0);
        } else if (sstDepth == SstDepth.depth_100) {
            grids[0] = NcUtils.readGrid(file, "sst_depth", getGridDef(), 1);
        } else /*if (sstDepth == SstDepth.skin)*/ {
            grids[0] = NcUtils.readGrid(file, "sst_skin", getGridDef(), 0);
        }
        grids[1] = NcUtils.readGrid(file, "uncertainty", getGridDef(), 0);
        return grids;
    }

    private static abstract class SstVariableType implements VariableType {

        @Override
        public Accumulator createAccumulator() {
            return new WeightedMeanAccumulator();
        }
    }

    private class SstSkinVariableType extends SstVariableType {
        @Override
        public Grid readGrid(NetcdfFile netcdfFile) throws IOException {
            return NcUtils.readGrid(netcdfFile, "sst_skin", getGridDef(), 0);
        }
    }

    private class SstDepth20VariableType extends SstVariableType {
        @Override
        public Grid readGrid(NetcdfFile netcdfFile) throws IOException {
            return NcUtils.readGrid(netcdfFile, "sst_depth", getGridDef(), 0);
        }
    }

    private class SstDepth100VariableType extends SstVariableType {
        @Override
        public Grid readGrid(NetcdfFile netcdfFile) throws IOException {
            return NcUtils.readGrid(netcdfFile, "sst_depth", getGridDef(), 1);
        }
    }

    private class UncertaintyVariableType implements VariableType {
        @Override
        public Grid readGrid(NetcdfFile netcdfFile) throws IOException {
            return NcUtils.readGrid(netcdfFile, "uncertainty", getGridDef(), 0);
        }

        @Override
        public Accumulator createAccumulator() {
            return new RandomUncertaintyAccumulator();
        }
    }
}
