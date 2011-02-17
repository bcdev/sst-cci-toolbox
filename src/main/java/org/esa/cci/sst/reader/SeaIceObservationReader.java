package org.esa.cci.sst.reader;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * A BEAM reader for Ocean & Sea Ice SAF data products.
 *
 * @author Thomas Storm
 */
public class SeaIceObservationReader extends AbstractProductReader {

    private static final String SEA_ICE_PARAMETER_BANDNAME = "sea_ice_parameter";
    private static final String QUALITY_FLAG_BANDNAME = "quality_flag";
    private File seaIceSourcefile;
    private File qualityFlagSourcefile;

    public SeaIceObservationReader(SeaIceObservationReaderPlugIn plugin) {
        super(plugin);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final String pathname = getInput().toString();
        seaIceSourcefile = getSeaIceSourceFile(pathname);
        qualityFlagSourcefile = getQualityFlagSourceFile(pathname);
        final NetcdfFile ncFile = NetcdfFile.open(seaIceSourcefile.getPath());
        final List<Variable> variables = ncFile.getVariables();
        Structure headerStructure = getHeaderStructure(variables);
        String productName;

        productName = getVariable("Header.product", headerStructure).readScalarString();
        int sceneRasterWidth = getVariable("Header.iw", headerStructure).readScalarInt();
        int sceneRasterHeight = getVariable("Header.ih", headerStructure).readScalarInt();
        int year = getVariable("Header.year", headerStructure).readScalarInt();
        int month = getVariable("Header.month", headerStructure).readScalarInt();
        int day = getVariable("Header.day", headerStructure).readScalarInt();
        int hour = getVariable("Header.hour", headerStructure).readScalarInt();
        int minute = getVariable("Header.minute", headerStructure).readScalarInt();

        final Product product = new Product(productName, getReaderPlugIn().getFormatNames()[0], sceneRasterWidth,
                                            sceneRasterHeight);
        setStartTime(product, year, month, day, hour, minute);
        final Band seaIceBand = product.addBand(SEA_ICE_PARAMETER_BANDNAME, ProductData.TYPE_FLOAT32);
        seaIceBand.setNoDataValue(-32767.0);
        final Band qualityFlagBand = product.addBand(QUALITY_FLAG_BANDNAME, ProductData.TYPE_INT16);
        qualityFlagBand.setNoDataValue(-32768);
        product.setGeoCoding(createGeoCoding());
        ncFile.close();
        return product;
    }

    private GeoCoding createGeoCoding() {
        return null;
    }

    void setStartTime(Product product, int year, int month, int day, int hour, int minute) {
        StringBuilder builder = new StringBuilder();
        builder.append(year);
        builder.append("-");
        if (month < 10) {
            builder.append("0");
        }
        builder.append(month);
        builder.append("-");
        if (day < 10) {
            builder.append("0");
        }
        builder.append(day);
        builder.append("-");
        if (hour < 10) {
            builder.append("0");
        }
        builder.append(hour);
        builder.append("-");
        if (minute < 10) {
            builder.append("0");
        }
        builder.append(minute);
        ProductData.UTC startTime = null;
        try {
            startTime = ProductData.UTC.parse(builder.toString(), "yyyy-MM-dd-HH-mm");
        } catch (ParseException e) {
            Debug.trace("No start time could be set due to the following exception:");
            Debug.trace(e);
        }
        product.setStartTime(startTime);
    }

    Variable getVariable(String varName, Structure headerStructure) {
        for (Variable variable : headerStructure.getVariables()) {
            if (varName.equals(variable.getName())) {
                return variable;
            }
        }
        throw new IllegalArgumentException("No variable with name '" + varName + "'.");
    }

    static File getSeaIceSourceFile(String pathname) {
        final File file = new File(pathname);
        if (file.getName().contains("_qual_")) {
            String name = file.getName();
            name = name.replace("_qual", "");
            return new File(file.getParent(), name);
        }
        return file;
    }

    static File getQualityFlagSourceFile(String pathname) {
        final File file = new File(pathname);
        if (file.getName().contains("_qual_")) {
            return file;
        }
        String name = file.getName();
        int lastUnderscoreIndex = name.lastIndexOf("_");
        final StringBuilder builder = new StringBuilder(name).insert(lastUnderscoreIndex, "_qual");
        return new File(file.getParent(), builder.toString());
    }

    static Structure getHeaderStructure(List<Variable> variables) {
        for (Variable variable : variables) {
            if ("Header".equals(variable.getName()) && variable instanceof Structure) {
                return (Structure) variable;
            }
        }
        throw new IllegalStateException(
                "HDF-File does not contain a header variable; should not have been opened using '" +
                SeaIceObservationReader.class.getSimpleName() + "'.");
    }

    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        File sourceFile;
        if (SEA_ICE_PARAMETER_BANDNAME.equals(destBand.getName())) {
            sourceFile = seaIceSourcefile;
        } else if (QUALITY_FLAG_BANDNAME.equals(destBand.getName())) {
            sourceFile = qualityFlagSourcefile;
        } else {
            return;
        }

        final NetcdfFile ncFile = NetcdfFile.open(sourceFile.getAbsolutePath());
        final Variable variable = ncFile.findVariable("Data/" + NetcdfFile.escapeName("data[00]"));
        int[] origin = new int[]{destOffsetX, destOffsetY};
        int[] shape = new int[]{destWidth, destHeight};
        Array array;
        try {
            array = variable.read(origin, shape);
        } catch (InvalidRangeException e) {
            throw new IOException("Unable to read in NetCDF-variable '" + variable.getName() + "'.", e);
        }
        destBuffer.setElems(array.copyTo1DJavaArray());
    }

}
