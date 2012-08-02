package org.esa.cci.sst.regrid;

import org.esa.cci.sst.util.ArrayGrid;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

/**
 * @author Bettina Scholze
 *         Date: 23.07.12 10:54
 */
public interface FileType {

    Map<String, ArrayGrid> readSourceGrids(NetcdfFile file) throws IOException;

    Date parseDate(File file) throws ParseException;

    void writeFile(NetcdfFile inputFile, File outputDirectory, Map<String, ArrayGrid> targetGrids, SpatialResolution targetResolution) throws IOException;
}
