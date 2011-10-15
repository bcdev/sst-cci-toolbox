package org.esa.cci.sst.regavg.filetypes;

import org.esa.cci.sst.regavg.FileType;
import org.esa.cci.sst.regavg.ProcessingLevel;
import org.esa.cci.sst.regavg.SstDepth;
import org.esa.cci.sst.regavg.VariableType;
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
 * Represents an unsupported file type.
 *
 * @author Norman Fomferra
 */
public abstract class UnsupportedFileType implements FileType {

    @Override
    public Date parseDate(File file) throws ParseException {
        throw new IllegalStateException("Not implemented.");
    }

    @Override
    public String getDefaultFilenameRegex() {
        throw new IllegalStateException("Not implemented.");
    }

    @Override
    public GridDef getGridDef() {
        throw new IllegalStateException("Not implemented.");
    }

    @Override
    public ProcessingLevel getProcessingLevel() {
        throw new IllegalStateException("Not implemented.");
    }

    @Override
    public Date readDate(NetcdfFile file) throws IOException {
        throw new IllegalStateException("Not implemented.");
    }

    @Override
    public VariableType[] getVariableTypes(SstDepth sstDepth) {
        throw new IllegalStateException("Not implemented.");
    }
}
