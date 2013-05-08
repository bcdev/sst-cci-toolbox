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
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.GridDef;
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
 * Represents an unsupported file type.
 *
 * @author Norman Fomferra
 */
public abstract class UnsupportedFileType implements FileType {

    @Override
    public Date parseDate(String filename) throws ParseException {
        throw notImplemented();
    }

    @Override
    public String getRdac() {
        throw notImplemented();
    }

    @Override
    public String getFilenameRegex() {
        throw notImplemented();
    }

    @Override
    public GridDef getGridDef() {
        throw notImplemented();
    }

    @Override
    public Date readDate(NetcdfFile datafile) throws IOException {
        throw notImplemented();
    }

    @Override
    public AggregationContext readSourceGrids(NetcdfFile dataFile, SstDepth sstDepth, AggregationContext context) throws IOException {
        throw notImplemented();
    }

    @Override
    public AggregationFactory<SameMonthAggregation<AggregationCell>> getSameMonthAggregationFactory() {
        throw notImplemented();
    }

    @Override
    public AggregationFactory<MultiMonthAggregation<RegionalAggregation>> getMultiMonthAggregationFactory() {
        throw notImplemented();
    }

    @Override
    public CellFactory<SpatialAggregationCell> getSpatialAggregationCellFactory(AggregationContext context) {
        throw notImplemented();
    }

    @Override
    public Variable[] addResultVariables(NetcdfFileWriteable file, Dimension[] dims, SstDepth sstDepth) {
        throw notImplemented();
    }

    private static IllegalStateException notImplemented() {
        return new IllegalStateException("Not implemented.");
    }

}
