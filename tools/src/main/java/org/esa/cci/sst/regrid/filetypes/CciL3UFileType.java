package org.esa.cci.sst.regrid.filetypes;

import org.esa.cci.sst.regrid.FileType;
import org.esa.cci.sst.regrid.SpatialResolution;
import org.esa.cci.sst.util.Grid;
import org.esa.cci.sst.util.GridDef;
import org.esa.cci.sst.util.NcUtils;
import org.esa.cci.sst.util.UTC;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * @author Bettina Scholze
 *         Date: 23.07.12 10:59
 */
public class CciL3UFileType implements FileType {

    public final static CciL3UFileType INSTANCE = new CciL3UFileType();
    public final DateFormat dateFormat = UTC.getDateFormat("yyyyMMdd");
    public final int filenameDateOffset = 0;

    @Override
    public Grid[] readSourceGrids(NetcdfFile file) throws IOException {
        GridDef associatedGridDef = SpatialResolution.DEGREE_0_05.getAssociatedGridDef();
        associatedGridDef.setTime(1);

        return NcUtils.readL3Grids(file, associatedGridDef);
    }

    @Override
    public Date parseDate(File file) throws ParseException {
        String dateString = file.getName().substring(filenameDateOffset, filenameDateOffset + 8);
        return dateFormat.parse(dateString);
    }

//    @Override
//    public CellFactory<AggregationCell5> getCell5Factory(final CoverageUncertaintyProvider coverageUncertaintyProvider) {
//        return new CellFactory<AggregationCell5>() {
//            @Override
//            public L3UCell5 createCell(int cellX, int cellY) {
//                return new L3UCell5(coverageUncertaintyProvider, cellX, cellY);
//            }
//        };
//    }

//    @Override
//    public CellFactory<AggregationCell90> getCell90Factory(final CoverageUncertaintyProvider coverageUncertaintyProvider) {
//        return new CellFactory<AggregationCell90>() {
//            @Override
//            public L3UCell90 createCell(int cellX, int cellY) {
//                return new L3UCell90(coverageUncertaintyProvider, cellX, cellY);
//            }
//        };
//    }


}
