package org.esa.cci.sst.reader;

import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Debug;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;

/**
 * A BEAM reader for Ocean & Sea Ice SAF data products.
 *
 * @author Thomas Storm
 */
public class SeaIceProductReader extends BasicNetcdfProductReader {

    static final String NH_GRID = "OSISAF_NH";
    static final String SH_GRID = "OSISAF_SH";

    private static final String SEA_ICE_PARAMETER_BANDNAME = "sea_ice_concentration";
    private static final String QUALITY_FLAG_BANDNAME = "quality_flag";
    private static final String VARIABLE_NAME = "Data/" + NetcdfFile.escapeName("data[00]");
    private static final String DESCRIPTION_SEA_ICE = "A data product containing information about sea ice " +
                                                      "concentration: it indicates the areal fraction of a given grid " +
                                                      "point covered by ice.";
    private static final String DESCRIPTION_QUALITY_FLAG = "A data product containing a quality flag for sea ice " +
                                                           "concentration: it indicates the confidence level " +
                                                           "corresponding to the quality of the calculated sea ice " +
                                                           "parameter and information on the processing conditions.";

    private int sceneRasterWidth;
    private int sceneRasterHeight;

    SeaIceProductReader(SeaIceProductReaderPlugIn plugin) {
        super(plugin);
    }

    @Override
    protected Product createProduct(NetcdfFile netcdfFile) throws IOException {
        final File inputFile = new File(netcdfFile.getLocation());
        final Variable header = netcdfFile.findVariable("Header");
        final Structure headerStructure = (Structure) header;

        final String productName = headerStructure.findVariable("product").readScalarString();
        sceneRasterWidth = headerStructure.findVariable("iw").readScalarInt();
        sceneRasterHeight = headerStructure.findVariable("ih").readScalarInt();

        final int year = headerStructure.findVariable("year").readScalarInt();
        final int month = headerStructure.findVariable("month").readScalarInt();
        final int day = headerStructure.findVariable("day").readScalarInt();
        final int hour = headerStructure.findVariable("hour").readScalarInt();
        final int minute = headerStructure.findVariable("minute").readScalarInt();

        final Product product = new Product(productName, getReaderPlugIn().getFormatNames()[0], sceneRasterWidth,
                                            sceneRasterHeight);
        setTimes(product, year, month, day, hour, minute);
        final Band band;
        if (isSeaIceFile(inputFile)) {
            band = product.addBand(SEA_ICE_PARAMETER_BANDNAME, ProductData.TYPE_FLOAT32);
            band.setNoDataValue(-32767.0);
            band.setNoDataValueUsed(true);
            product.setDescription(DESCRIPTION_SEA_ICE);
        } else {
            band = product.addBand(QUALITY_FLAG_BANDNAME, ProductData.TYPE_INT16);
            band.setNoDataValue(-32767);
            band.setNoDataValueUsed(true);
            product.setDescription(DESCRIPTION_QUALITY_FLAG);
        }
        product.setFileLocation(inputFile);
        product.setProductReader(this);
        product.getMetadataRoot().addElement(getMetadata(headerStructure));
        product.setGeoCoding(createGeoCoding(headerStructure));
        // IMPORTANT - resulting image is wrong when tile size is different from image dimension
        product.setPreferredTileSize(sceneRasterWidth, sceneRasterHeight);
        band.setSourceImage(createSourceImage(band));
        return product;
    }

    MetadataElement getMetadata(Structure headerStructure) {
        final MetadataElement element = new MetadataElement("Header");
        for (Variable variable : headerStructure.getVariables()) {
            int type = DataTypeUtils.getEquivalentProductDataType(variable.getDataType(), false, false);
            ProductData productData = null;
            try {
                switch (type) {
                case ProductData.TYPE_ASCII: {
                    productData = ProductData.createInstance(variable.readScalarString());
                    break;
                }
                case ProductData.TYPE_INT8: {
                    productData = ProductData.createInstance(new int[]{variable.readScalarByte()});
                    break;
                }
                case ProductData.TYPE_INT16: {
                    productData = ProductData.createInstance(new int[]{variable.readScalarInt()});
                    break;
                }
                case ProductData.TYPE_INT32: {
                    productData = ProductData.createInstance(new int[]{variable.readScalarInt()});
                    break;
                }
                case ProductData.TYPE_FLOAT32: {
                    productData = ProductData.createInstance(new float[]{variable.readScalarFloat()});
                    break;
                }
                case ProductData.TYPE_FLOAT64: {
                    productData = ProductData.createInstance(new double[]{variable.readScalarDouble()});
                    break;
                }
                default: {
                    break;
                }
                }
            } catch (IOException e) {
                Debug.trace(e.getMessage());
            }
            MetadataAttribute attribute = new MetadataAttribute(variable.getName(), productData, true);
            element.addAttribute(attribute);
        }
        return element;
    }

    GeoCoding createGeoCoding(Structure headerStructure) throws IOException {
        try {
            final String grid = headerStructure.findVariable("area").readScalarString();
            String code;
            if (NH_GRID.equals(grid)) {
                code = "EPSG:3411";
            } else if (SH_GRID.equals(grid)) {
                code = "EPSG:3412";
            } else {
                // code for computing math transform for higher latitude grid is to be found
                // in commit e9a32d1c6d18670c358f8e9434a7d2becb149449
                throw new IllegalStateException(
                        "Grid support for grids different from 'Northern Hemisphere Grid' and " +
                        "'Southern Hemisphere Grid' not yet implemented.");
            }
            final double easting = headerStructure.findVariable("Bx").readScalarFloat() * 1000.0;
            final double northing = headerStructure.findVariable("By").readScalarFloat() * 1000.0;
            final double pixelSizeX = headerStructure.findVariable("Ax").readScalarFloat() * 1000.0;
            final double pixelSizeY = headerStructure.findVariable("Ay").readScalarFloat() * 1000.0;
            return new CrsGeoCoding(CRS.decode(code),
                                    sceneRasterWidth,
                                    sceneRasterHeight,
                                    easting, northing,
                                    pixelSizeX,
                                    pixelSizeY, 0.0, 0.0);
        } catch (FactoryException e) {
            Debug.trace(e);
        } catch (TransformException e) {
            Debug.trace(e);
        }
        return null;
    }


    @Override
    protected RenderedImage createSourceImage(Band band) {
        final Variable variable = getNetcdfFile().findVariable(VARIABLE_NAME);
        final int dataBufferType = ImageManager.getDataBufferType(band.getDataType());
        return new VariableOpImage(variable, dataBufferType,
                                   band.getSceneRasterWidth(),
                                   band.getSceneRasterHeight(),
                                   band.getProduct().getPreferredTileSize()
        );
    }

    void setTimes(Product product, int year, int month, int day, int hour, int minute) {
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
        product.setEndTime(startTime);
    }

    static boolean isSeaIceFile(File file) {
        return !file.getName().contains("_qual_");
    }

}
