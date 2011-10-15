package org.esa.cci.sst.regavg.filetypes;

import org.esa.cci.sst.regavg.*;
import org.esa.cci.sst.util.Grid;
import org.esa.cci.sst.util.GridDef;
import org.esa.cci.sst.util.UTC;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import static java.lang.Math.round;

/**
 * Represents the ARC_L3U file type.
 * <p/>
 * Filename convention:
 * <p/>
 * n/d = night or day<br/>
 * N/D = Nadir or Dual view<br/>
 * 2/3 = 2 or 3 channel retrieval (3 chan only valid during night)<br/>
 * b/m = bayes or min-bayes cloud screening<br/>
 *
 * @author Norman Fomferra
 */
public class ArcL3UFileType implements FileType {
    public final DateFormat dateFormat = UTC.getDateFormat("yyyyMMdd");
    public final int filenameDateOffset = "ATS_AVG_3PAARC".length();
    public final String filenameRegex = "AT[12S]_AVG_3PAARC\\d{8}_[DTEM]_[nd][ND][23][bms][.]nc[.]gz";
    public final GridDef gridDef = GridDef.createGlobalGrid(3600, 1800);

    @Override
    public Date parseDate(File file) throws ParseException {
        String dateString = file.getName().substring(filenameDateOffset, filenameDateOffset + 8);
        return dateFormat.parse(dateString);
    }

    @Override
    public String getDefaultFilenameRegex() {
        return filenameRegex;
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
        int secondsSince1981 = round(variable.readScalarFloat());
        Calendar calendar = UTC.createCalendar(1981);
        calendar.add(Calendar.SECOND, secondsSince1981);
        return calendar.getTime();
    }

    @Override
    public VariableType[] getVariableTypes(SstDepth sstDepth) {
        return new VariableType[] {};
    }
}
