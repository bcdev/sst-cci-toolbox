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
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellAggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.regavg.MultiMonthAggregation;
import org.esa.cci.sst.regavg.SameMonthAggregation;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
 * Provides data and behaviour for a specific file-type.
 *
 * @author Norman Fomferra
 */
public interface FileType {
    /**
     * @param file The file path.
     * @return The date as parsed from the file path.
     * @throws java.text.ParseException If the date could not be parsed.
     */
    Date parseDate(File file) throws ParseException;

    /**
     * @param dataFile The NetCDF file.
     * @return The date as read from the NetCDF file.
     * @throws java.io.IOException If the date could not be read.
     */
    Date readDate(NetcdfFile dataFile) throws IOException;

    /**
     * Gives the RDAC (Regional Data Assembly Center) of the origin product.
     *
     * @return RDAC
     */
    String getRdac();

    String getFilenameRegex();

    GridDef getGridDef();

    AggregationContext readSourceGrids(NetcdfFile dataFile, SstDepth sstDepth, AggregationContext context) throws IOException;

    Variable[] createOutputVariables(NetcdfFileWriteable file, SstDepth sstDepth, boolean totalUncertainty, Dimension[] dims);

    AggregationFactory<SameMonthAggregation> getSameMonthAggregationFactory();

    AggregationFactory<MultiMonthAggregation> getMultiMonthAggregationFactory();

    CellFactory<SpatialAggregationCell> getSpatialAggregationCellFactory(final AggregationContext context);

    CellFactory<CellAggregationCell<AggregationCell>> getTemporalAggregationCellFactory();

    CellFactory getCellFactory(final AggregationContext context, CellTypes cellType);

    boolean hasSynopticUncertainties();

}
