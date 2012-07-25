package org.esa.cci.sst.regrid;

import org.esa.cci.sst.util.Grid;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
 * @author Bettina Scholze
 *         Date: 23.07.12 10:54
 */
public interface FileType {

    Grid[] readSourceGrids(NetcdfFile file) throws IOException;

    Date parseDate(File file) throws ParseException;
}
