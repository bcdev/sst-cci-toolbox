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
import org.esa.cci.sst.common.AggregationFactory;
import org.esa.cci.sst.common.RegionalAggregation;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellAggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.GridDef;
import org.esa.cci.sst.regavg.MultiMonthAggregation;
import org.esa.cci.sst.regavg.SameMonthAggregation;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
 * Provides data and behaviour for a specific file-type.
 *
 * @author Norman Fomferra
 */
public interface FileType {

    Date parseDate(String filename) throws ParseException;

    Date readDate(NetcdfFile datafile) throws IOException;

    /**
     * Returns the Regional Data Assembly Center (RDAC) of the product.
     *
     * @return the RDAC.
     */
    String getRdac();

    String getFilenameRegex();

    GridDef getGridDef();

    AggregationContext readSourceGrids(NetcdfFile datafile, SstDepth sstDepth, AggregationContext context) throws IOException;

    Variable[] addResultVariables(NetcdfFileWriteable datafile, Dimension[] dims, SstDepth sstDepth);

    AggregationFactory<SameMonthAggregation<AggregationCell>> getSameMonthAggregationFactory();

    AggregationFactory<MultiMonthAggregation<RegionalAggregation>> getMultiMonthAggregationFactory();

    CellFactory<SpatialAggregationCell> getCellFactory5(final AggregationContext context);

    CellFactory<CellAggregationCell<AggregationCell>> getCellFactory90(final AggregationContext context);

    CellFactory<SpatialAggregationCell> getSpatialAggregationCellFactory(final AggregationContext context);

    CellFactory<CellAggregationCell<AggregationCell>> getMultiMonthAggregationCellFactory();

    CellFactory<SpatialAggregationCell> getSingleDayAggregationCellFactory(AggregationContext aggregationContext);
}
