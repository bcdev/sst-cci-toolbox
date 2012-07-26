package org.esa.cci.sst.regrid.filetypes;

import org.esa.cci.sst.regrid.FileType;
import org.esa.cci.sst.regrid.SpatialResolution;
import org.esa.cci.sst.util.ArrayGrid;
import org.esa.cci.sst.util.GridDef;
import org.esa.cci.sst.util.NcUtils;
import org.esa.cci.sst.util.UTC;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

/**
 * @author Bettina Scholze
 *         Date: 23.07.12 10:59
 */
public class CciL3UFileType implements FileType {

    public final static CciL3UFileType INSTANCE = new CciL3UFileType();
    public final DateFormat dateFormat = UTC.getDateFormat("yyyyMMdd");
    public final int filenameDateOffset = 0;

    @Override
    public Map<String, ArrayGrid> readSourceGrids(NetcdfFile file) throws IOException {
        double gridResolution = NcUtils.getGridResolution(file);
        SpatialResolution spatialResolution = SpatialResolution.getFromValue(String.valueOf(gridResolution));
        GridDef associatedGridDef = spatialResolution.getAssociatedGridDef();
        associatedGridDef.setTime(1);

        return NcUtils.readL3Grids(file, associatedGridDef);
    }

    @Override
    public Date parseDate(File file) throws ParseException {
        String dateString = file.getName().substring(filenameDateOffset, filenameDateOffset + 8);
        return dateFormat.parse(dateString);
    }
}
