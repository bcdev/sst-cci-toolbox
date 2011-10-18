package org.esa.cci.sst.regavg.filetypes;

import org.esa.cci.sst.regavg.*;
import org.esa.cci.sst.util.*;
import org.esa.cci.sst.util.accumulators.RandomUncertaintyAccumulator;
import org.esa.cci.sst.util.accumulators.WeightedMeanAccumulator;
import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.nc2.Dimension;

import java.awt.*;
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
    public Grid[] readSourceGrids(NetcdfFile file, SstDepth sstDepth) throws IOException {
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

    @Override
    public Variable[] createOutputVariables(NetcdfFileWriteable file, SstDepth sstDepth, Dimension[] dims) {

        Variable sstVar = file.addVariable(String.format("sst_%s_mean", sstDepth), DataType.FLOAT, dims);
        sstVar.addAttribute(new Attribute("units", "kelvin"));
        sstVar.addAttribute(new Attribute("long_name", String.format("mean of sst %s in kelvin.", sstDepth)));
        sstVar.addAttribute(new Attribute("_FillValue", Double.NaN));

        Variable sstAnomalyVar = file.addVariable(String.format("sst_%s_anomaly_mean", sstDepth), DataType.FLOAT, dims);
        sstAnomalyVar.addAttribute(new Attribute("units", "kelvin"));
        sstAnomalyVar.addAttribute(new Attribute("long_name", String.format("mean of sst %s anomaly in kelvin.", sstDepth)));
        sstAnomalyVar.addAttribute(new Attribute("_FillValue", Double.NaN));

        Variable uncertaintyVar = file.addVariable("uncertainty_mean", DataType.FLOAT, dims);
        uncertaintyVar.addAttribute(new Attribute("units", "kelvin"));
        uncertaintyVar.addAttribute(new Attribute("long_name", String.format("mean of uncertainty in kelvin.", sstDepth)));
        uncertaintyVar.addAttribute(new Attribute("_FillValue", Double.NaN));

        Variable sampleCountVar = file.addVariable(String.format("sample_count", sstDepth), DataType.DOUBLE, dims);
        uncertaintyVar.addAttribute(new Attribute("units", "1"));
        sampleCountVar.addAttribute(new Attribute("long_name", String.format("counts of sst %s contributions.", sstDepth)));

        return new Variable[]{
                sstVar,
                sstAnomalyVar,
                uncertaintyVar,
                sampleCountVar,
        };
    }


    @Override
    public CellFactory getCellFactory() {
        return new CellFactory<ArcL3UCell>() {
            @Override
            public ArcL3UCell createCell() {
                return new ArcL3UCell();
            }
        };
    }

    private static class ArcL3UCell extends SstCell {

        private Accumulator sstAccu = new WeightedMeanAccumulator();
        private Accumulator sstAnomalyAccu = new WeightedMeanAccumulator();
        private Accumulator uncertaintyAccu = new RandomUncertaintyAccumulator();

        @Override
        public boolean isEmpty() {
            return sstAccu.getSampleCount() == 0;
        }

        @Override
        public void aggregateSourceRect(SstCellContext sstCellContext, Rectangle rect) {
            final Grid sstGrid = sstCellContext.getSourceGrids()[0];
            final Grid uncertaintyGrid = sstCellContext.getSourceGrids()[1];
            final Grid analysedSstGrid = sstCellContext.getAnalysedSstGrid();
            final Grid seaCoverageGrid = sstCellContext.getSeaCoverageGrid();

            final int x0 = rect.x;
            final int y0 = rect.y;
            final int x1 = x0 + rect.width - 1;
            final int y1 = y0 + rect.height - 1;
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    final double seaCoverage = seaCoverageGrid.getSampleDouble(x, y);
                    if (seaCoverage > 0.0) {
                        sstAccu.accumulate(sstGrid.getSampleDouble(x, y), seaCoverage);
                        sstAnomalyAccu.accumulate(sstGrid.getSampleDouble(x, y) - analysedSstGrid.getSampleDouble(x, y), seaCoverage);
                        uncertaintyAccu.accumulate(uncertaintyGrid.getSampleDouble(x, y), seaCoverage);
                    }
                }
            }
        }

        @Override
        public void accumulate(Cell cell) {
            ArcL3UCell otherCell = (ArcL3UCell) cell;
            sstAccu.accumulate(otherCell.sstAccu);
            sstAnomalyAccu.accumulate(otherCell.sstAnomalyAccu);
            uncertaintyAccu.accumulate(otherCell.uncertaintyAccu);
        }

        @Override
        public void accumulateAverage(Cell cell, double weight) {
            ArcL3UCell otherCell = (ArcL3UCell) cell;
            sstAccu.accumulateAverage(otherCell.sstAccu, weight);
            sstAnomalyAccu.accumulateAverage(otherCell.sstAnomalyAccu, weight);
            uncertaintyAccu.accumulateAverage(otherCell.uncertaintyAccu, weight);
        }

        @Override
        public ArcL3UCell clone() {
            ArcL3UCell clone = (ArcL3UCell) super.clone();
            clone.sstAccu = sstAccu.clone();
            clone.sstAnomalyAccu = sstAnomalyAccu.clone();
            clone.uncertaintyAccu = uncertaintyAccu.clone();
            return clone;
        }

        @Override
        public Number[] getResults() {
            // Note: Result types must match those defined in FileType.createOutputVariables().
            return new Number[]{
                    (float) sstAccu.computeAverage(),
                    (float) sstAnomalyAccu.computeAverage(),
                    (float) uncertaintyAccu.computeAverage(),
                    (double) sstAccu.getSampleCount(),
            };
        }
    }
}
