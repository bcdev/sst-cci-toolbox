package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.CellFactory;
import org.esa.cci.sst.util.Grid;
import org.esa.cci.sst.util.GridDef;
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
     * @param file The NetCDF file.
     * @return The date as read from the NetCDF file.
     * @throws java.io.IOException If the date could not be read.
     */
    Date readDate(NetcdfFile file) throws IOException;

    String getFilenameRegex();

    ProcessingLevel getProcessingLevel();

    GridDef getGridDef();

    Grid[] readSourceGrids(NetcdfFile file, SstDepth sstDepth) throws IOException;

    Variable[] createOutputVariables(NetcdfFileWriteable file, SstDepth sstDepth, Dimension[] dims);

    CellFactory<AggregationCell5> getCell5Factory(CoverageUncertaintyProvider coverageUncertaintyProvider);

    CellFactory<AggregationCell90> getCell90Factory(CoverageUncertaintyProvider coverageUncertaintyProvider);

    AggregationFactory<SameMonthAggregation> getSameMonthAggregationFactory();

    AggregationFactory<MultiMonthAggregation> getMultiMonthAggregationFactory();
}
