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
import java.util.logging.Logger;

/**
 * @author Bettina Scholze
 *         Date: 23.07.12 10:59
 */
public class CciL3UFileType implements FileType {
    public static final Logger LOGGER = Logger.getLogger("org.esa.cci.sst.regrid.filetypes.CciL3UFileType");

    public final static CciL3UFileType INSTANCE = new CciL3UFileType();
    public final DateFormat dateFormat = UTC.getDateFormat("yyyyMMdd");
    public final int filenameDateOffset = 0;

    @Override
    public Map<String, ArrayGrid> readSourceGrids(NetcdfFile file) throws IOException {
        double gridResolution = NcUtils.getGridResolution(file);
        SpatialResolution spatialResolution = SpatialResolution.getFromValue(String.valueOf(gridResolution));
        GridDef gridDef = spatialResolution.getAssociatedGridDef();

        return NcUtils.readL3Grids(file, gridDef);
    }

    @Override
    public Date parseDate(File file) throws ParseException {
        String dateString = file.getName().substring(filenameDateOffset, filenameDateOffset + 8);
        return dateFormat.parse(dateString);
    }

    /**
     * <p/>
     * <ul>
     * <li>init</li>
     * <li>global attributes / dimensions</li>
     * <li>variable and variables attributes in header
     * 1) measurement related (sst etc.)
     * 2) product specific (lat, lat_bnds, lon, lon_bnds, time, time_bnds)
     * </li>
     * <li>netcdfFile.createWith2DDoubleArray() write header</li>
     * <li>add arrays
     * 1) product specific (lat, lat_bnds, lon, lon_bnds, time, time_bnds)
     * 2) measurement related (sst etc.)   -> loop it
     * </li>
     * </ul>
     */
    @Override
    public void writeFile(NetcdfFile inputFile, File outputDirectory, Map<String, ArrayGrid> targetGrids, Map<String, Array> baseArrays) throws IOException {
        File inputFilePath = new File(inputFile.getLocation());
        final File outputFile = new File(outputDirectory, inputFilePath.getName());
        NetcdfFileWriteable netcdfFile = NetcdfFileWriteable.createNew(outputFile.getPath());
        //global attributes
        List<Attribute> globalAttributes = inputFile.getGlobalAttributes();
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
            for (Variable inputVariable : inputFile.getVariables()) {
                if (variable.equals(inputVariable.getName())) {
                    addVariableWithAttributestoNetCdfHeader(netcdfFile, dimensionMeasurementRelated, variable, inputVariable);
                }
            }
        }
        for (String baseVariable : baseArrays.keySet()) {
            for (Variable inputVariable : inputFile.getVariables()) {
                if (baseVariable.equals(inputVariable.getName())) {
                    Dimension[] dimensions = null;
                    if ("lat".equals(baseVariable)) {
                        dimensions = new Dimension[]{latDim};
                    } else if ("lon".equals(baseVariable)) {
                        dimensions = new Dimension[]{lonDim};
                    } else if ("lon_bnds".equals(baseVariable)) {
                        dimensions = new Dimension[]{lonDim, bndsDim};
                    } else if ("lat_bnds".equals(baseVariable)) {
                        dimensions = new Dimension[]{latDim, bndsDim};
                    }
                    addVariableWithAttributestoNetCdfHeader(netcdfFile, dimensions, baseVariable, inputVariable);
                }
            }
        }

        //todo add time band
        //todo add time_bnds band

        //write header
        netcdfFile.create();

        //add values for regridded variables
        for (String variable : targetGrids.keySet()) {
            Array array = targetGrids.get(variable).getArray();
            writeDataToNetCdfFile(netcdfFile, variable, array);
        }
        for (String baseVariable : baseArrays.keySet()) {
            writeDataToNetCdfFile(netcdfFile, baseVariable, baseArrays.get(baseVariable));
        }
        //todo add time band
        //todo add time_bnds band

        netcdfFile.flush();
        netcdfFile.close();
    }

    private void writeDataToNetCdfFile(NetcdfFileWriteable netcdfFile, String variable, Array array) throws IOException {
        try {
            netcdfFile.write(variable, array);
        } catch (InvalidRangeException e) {
            LOGGER.throwing("CciL3UFileType", "writeDataToNetCdfFile", e);
        }
    }

    private void addVariableWithAttributestoNetCdfHeader(NetcdfFileWriteable netcdfFile, Dimension[] dimensionMeasurementRelated, String baseVariable, Variable inputVariable) {
        DataType dataTypeFromInput = inputVariable.getDataType();
        List<Attribute> attributesFromInput = inputVariable.getAttributes();
        netcdfFile.addVariable(baseVariable, dataTypeFromInput, dimensionMeasurementRelated);

        for (Attribute attribute : attributesFromInput) {
            netcdfFile.addVariableAttribute(baseVariable, attribute);
        }
    }
}
