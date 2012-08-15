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

package org.esa.cci.sst.regavg.filetypes;

import org.esa.cci.sst.regavg.*;
import org.esa.cci.sst.util.*;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
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
    public Date parseDate(File file) throws ParseException {
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
    public ProcessingLevel getProcessingLevel() {
        throw notImplemented();
    }

    @Override
    public Date readDate(NetcdfFile file) throws IOException {
        throw notImplemented();
    }

    @Override
    public Grid[] readSourceGrids(NetcdfFile file, SstDepth sstDepth) throws IOException {
        throw notImplemented();
    }

    @Override
    public CellFactory<AggregationCell> getCellFactory() {
        throw notImplemented();
    }

    @Override
    public CellFactory<SpatialAggregationCell> getCell5Factory(CoverageUncertaintyProvider coverageUncertaintyProvider) {
        throw notImplemented();
    }

    @Override
    public CellFactory<AggregationCell90> getCell90Factory(CoverageUncertaintyProvider coverageUncertaintyProvider) {
        throw notImplemented();
    }

    @Override
    public AggregationFactory<SameMonthAggregation> getSameMonthAggregationFactory() {
        throw notImplemented();
    }

    @Override
    public AggregationFactory<MultiMonthAggregation> getMultiMonthAggregationFactory() {
        throw notImplemented();
    }

    @Override
    public Variable[] createOutputVariables(NetcdfFileWriteable file, SstDepth sstDepth, Dimension[] dims) {
        throw notImplemented();
    }

    private static IllegalStateException notImplemented() {
        return new IllegalStateException("Not implemented.");
    }

}
