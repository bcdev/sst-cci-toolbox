package org.esa.cci.sst.regrid.filetypes;

import org.esa.cci.sst.regrid.FileType_Deprecated;
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
public class CciL3UFileType implements FileType_Deprecated {
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
     * <li>variable and variables attributes in header</li>
     * <li>netcdfFile.createWith2DDoubleArray() write header</li>
     * <li>add arrays
     * 1) product specific (lat, lat_bnds, lon, lon_bnds, time, time_bnds)
     * 2) measurement related (sst etc.)
     * 3) copies time, time_bnds
     * </li>
     * </ul>
     */
    @Override
    public void writeFile(NetcdfFile inputFile, File outputDirectory, Map<String, ArrayGrid> targetGrids, SpatialResolution targetResolution) throws IOException {
        Map<String, Array> baseArrays = targetResolution.calculateBaseArrays();

        File inputFilePath = new File(inputFile.getLocation());
        final File outputFile = new File(outputDirectory, inputFilePath.getName());
        NetcdfFileWriteable netcdfFile = NetcdfFileWriteable.createNew(outputFile.getPath());
        //global attributes
        List<Attribute> inputGlobalAttributes = inputFile.getGlobalAttributes();
        for (Attribute globalAttribute : inputGlobalAttributes) {
            String name = globalAttribute.getName();
            netcdfFile.addGlobalAttribute(name, globalAttribute.getValues());
            if ("geospatial_lat_resolution".equals(name) || "geospatial_lon_resolution".equals(name)) {
                netcdfFile.addGlobalAttribute(name, targetResolution.getValue());
            }
        }
        //global dimensions
        GridDef gridDef = targetGrids.get(new ArrayList<String>(targetGrids.keySet()).get(0)).getGridDef();
        Dimension latDim = netcdfFile.addDimension("lat", gridDef.getHeight());
        Dimension lonDim = netcdfFile.addDimension("lon", gridDef.getWidth());
        Dimension timeDim = netcdfFile.addDimension("time", gridDef.getTime(), true, false, false);
        Dimension bndsDim = netcdfFile.addDimension("bnds", 2);

        //copy variable and variable attributes into header 
        Dimension[] dimensionMeasurementRelated = {timeDim, latDim, lonDim};
        List<Variable> inputVariables = inputFile.getVariables();
        for (Variable inputVariable : inputVariables) {
            Dimension[] dimensions = null;
            String variable = inputVariable.getName();
            if (targetGrids.keySet().contains(variable)) {
                dimensions = dimensionMeasurementRelated;
            } else {
                if ("lat".equals(variable)) {
                    dimensions = new Dimension[]{latDim};
                } else if ("lon".equals(variable)) {
                    dimensions = new Dimension[]{lonDim};
                } else if ("time".equals(variable)) {
                    dimensions = new Dimension[]{timeDim};
                } else if ("lon_bnds".equals(variable)) {
                    dimensions = new Dimension[]{lonDim, bndsDim};
                } else if ("lat_bnds".equals(variable)) {
                    dimensions = new Dimension[]{latDim, bndsDim};
                } else if ("time_bnds".equals(variable)) {
                    dimensions = new Dimension[]{timeDim, bndsDim};
                }
            }
            addVariableWithAttributesToNetCdfHeader(netcdfFile, dimensions, variable, inputVariable);
        }
        //add coverage_uncertainty to header
        netcdfFile.addVariable("coverage_uncertainty", DataType.SHORT, dimensionMeasurementRelated);
        Attribute attributeLongName = new Attribute("long_name", "Uncertainty from making a grid coarser and having NaN values in the source.");
        netcdfFile.addVariableAttribute("coverage_uncertainty", attributeLongName);
        netcdfFile.addVariableAttribute("coverage_uncertainty", new Attribute("scale_factor", "0.01f"));
        netcdfFile.addVariableAttribute("coverage_uncertainty", new Attribute("add_offset", "0.0f"));
        netcdfFile.addVariableAttribute("coverage_uncertainty", new Attribute("valid_min", "0s"));
        netcdfFile.addVariableAttribute("coverage_uncertainty", new Attribute("valid_max", "5000s"));
        netcdfFile.addVariableAttribute("coverage_uncertainty", new Attribute("_Netcdf4Dimid", "3"));

        //write header
        netcdfFile.create();

        //add data for regridded variables
        for (String variable : targetGrids.keySet()) {
            Array array = targetGrids.get(variable).getArray();
            writeDataToNetCdfFile(netcdfFile, variable, array);
        }
        for (String baseVariable : baseArrays.keySet()) {
            writeDataToNetCdfFile(netcdfFile, baseVariable, baseArrays.get(baseVariable));
        }
        //copy data for time
        for (Variable inputVariable : inputVariables) {
            String inputVariableName = inputVariable.getName();
            if ("time".equals(inputVariableName) || "time_bnds".equals(inputVariableName)) {
                writeDataToNetCdfFile(netcdfFile, inputVariableName, inputVariable.read());
            }
        }
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

    private void addVariableWithAttributesToNetCdfHeader(NetcdfFileWriteable netcdfFile, Dimension[] dimensions, String variable, Variable inputVariable) {
        DataType dataTypeFromInput = inputVariable.getDataType();
        List<Attribute> attributesFromInput = inputVariable.getAttributes();
        netcdfFile.addVariable(variable, dataTypeFromInput, dimensions);

        for (Attribute attribute : attributesFromInput) {
            netcdfFile.addVariableAttribute(variable, attribute);
        }
    }
}
