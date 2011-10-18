package org.esa.cci.sst.regavg.filetypes;

import org.esa.cci.sst.regavg.FileType;
import org.esa.cci.sst.util.GridReader;
import org.esa.cci.sst.regavg.ProcessingLevel;
import org.esa.cci.sst.regavg.SstDepth;
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

import static java.lang.Math.round;

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
    public CellFactory getCellFactory() {
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
