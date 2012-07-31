package org.esa.cci.sst.regrid.filetypes;

import org.esa.cci.sst.regrid.FileType;
import org.esa.cci.sst.regrid.SpatialResolution;
import org.esa.cci.sst.util.ArrayGrid;
import org.esa.cci.sst.util.GridDef;
import org.esa.cci.sst.util.NcUtils;
import org.esa.cci.sst.util.UTC;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    /**
     *- init
     *
     *- global attributes / dimensions
     *- variable and variables attributes in header
     *    1) product specific (lat, lat_bnds, lon, lon_bnds, time, time_bnds)
     *    2) measurement related (sst etc.)   -> loop it
     *- netcdfFile.create() write header
     *- add arrays
     *    1) product specific (lat, lat_bnds, lon, lon_bnds, time, time_bnds)
     *    2) measurement related (sst etc.)   -> loop it
    */
    public void writeFile(NetcdfFile netcdfFileInput, File outputDirectory, Map<String, ArrayGrid> targetGrids) throws IOException {
        File inputFile = new File(netcdfFileInput.getLocation());
        final File outputFile = new File(outputDirectory, inputFile.getName());
        NetcdfFileWriteable netcdfFile = NetcdfFileWriteable.createNew(outputFile.getPath());
        //global attributes
        List<Attribute> globalAttributes = netcdfFileInput.getGlobalAttributes();
        for (Attribute globalAttribute : globalAttributes) {
            netcdfFile.addGlobalAttribute(globalAttribute.getName(), globalAttribute.getValues());
        }
        //global dimensions
        GridDef gridDef = targetGrids.get(new ArrayList<String>(targetGrids.keySet()).get(0)).getGridDef();
        Dimension latDim = netcdfFile.addDimension("lat", gridDef.getHeight());
        Dimension lonDim = netcdfFile.addDimension("lon", gridDef.getWidth());
        Dimension timeDim = netcdfFile.addDimension("time", gridDef.getTime(), true, false, false);
        Dimension bndsDim = netcdfFile.addDimension("bnds", 2);

        //header information input variables (measurement related data)
        Dimension[] dimensionMeasurementRelated = {timeDim, latDim, lonDim};
        for (String variable : targetGrids.keySet()) {
            for (Variable inputVariable : netcdfFileInput.getVariables()) {

                if (variable.equals(inputVariable.getName())) {

                    DataType dataTypeFromInput = inputVariable.getDataType();
                    List<Attribute> attributesFromInput = inputVariable.getAttributes();
                    netcdfFile.addVariable(variable, dataTypeFromInput, dimensionMeasurementRelated);

                    for (Attribute attribute : attributesFromInput) {
                        netcdfFile.addVariableAttribute(variable, attribute);
                    }
                }
            }
        }

        //header information lat lon time data

        //todo add lat band
        //todo add lat_bnds band
        //todo add lon band
        //todo add lon_bnds band
        //todo add time band
        //todo add time_bnds band

        //write header
        netcdfFile.create();

        //add values for regridded variables
        for (String variable : targetGrids.keySet()) {
            ArrayGrid arrayGrid = targetGrids.get(variable);
            Array array = arrayGrid.getArray();
            try {
                netcdfFile.write(variable, array);
            } catch (InvalidRangeException e) {
                e.printStackTrace();  //todo implement it!
            }
        }

        //todo add lat band
        //todo add lat_bnds band
        //todo add lon band
        //todo add lon_bnds band
        //todo add time band
        //todo add time_bnds band

        netcdfFile.flush();
        netcdfFile.close();
    }

}
