package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.util.UTC;
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
 * {@author Bettina Scholze}
 * Date: 17.09.12 10:46
 */
public abstract class AbstractCciFileType implements FileType {
    public final GridDef GRID_DEF = GridDef.createGlobalGrid(7200, 3600); //source per default on 0.05 ° resolution

    @Override
    public Date parseDate(File file) throws ParseException {
        String dateFormatString = "yyyyMMdd";
        final DateFormat dateFormat = UTC.getDateFormat(dateFormatString);
        String dateString = file.getName().substring(0, dateFormatString.length());
        return dateFormat.parse(dateString);
    }

    @Override
    public Date readDate(NetcdfFile file) throws IOException {
        Variable variable = file.findTopVariable("time");
        if (variable == null) {
            throw new IOException("Missing variable 'time' in file '" + file.getLocation() + "'");
        }
        // The time of L4 files is encoded as seconds since 01.01.1981
        int secondsSince1981 = round(variable.readScalarFloat());
        Calendar calendar = UTC.createCalendar(1981);
        calendar.add(Calendar.SECOND, secondsSince1981);
        return calendar.getTime();
    }

    @Override
    public String getRdac() {
        return "ESACCI";
    }

    @Override
    public GridDef getGridDef() {
        return GRID_DEF;
    }

}
