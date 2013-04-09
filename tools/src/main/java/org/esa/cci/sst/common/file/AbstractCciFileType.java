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

package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.AggregationContext;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.DefaultSpatialAggregationCellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
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

/**
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public abstract class AbstractCciFileType implements FileType {

    public static final GridDef GRID_DEF = GridDef.createGlobal(7200, 3600);

    @Override
    public final Date parseDate(File file) throws ParseException {
        final String dateFormatString = "yyyyMMdd";
        final DateFormat dateFormat = UTC.getDateFormat(dateFormatString);
        final String dateString = file.getName().substring(0, dateFormatString.length());

        return dateFormat.parse(dateString);
    }

    @Override
    public final Date readDate(NetcdfFile dataFile) throws IOException {
        final Variable variable = dataFile.findTopVariable("time");
        if (variable == null) {
            throw new IOException("Missing variable 'time' in file '" + dataFile.getLocation() + "'");
        }
        // time is encoded as seconds since 01.01.1981
        final int secondsSince1981 = variable.readScalarInt();
        final Calendar calendar = UTC.createCalendar(1981);
        calendar.add(Calendar.SECOND, secondsSince1981);

        return calendar.getTime();
    }

    @Override
    public final GridDef getGridDef() {
        return GRID_DEF;
    }

    @Override
    public final String getRdac() {
        return "ESACCI";
    }

    @Override
    public final CellFactory<SpatialAggregationCell> getSpatialAggregationCellFactory(AggregationContext context) {
        return new DefaultSpatialAggregationCellFactory(context);
    }
}
