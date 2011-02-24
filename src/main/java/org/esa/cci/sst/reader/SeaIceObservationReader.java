package org.esa.cci.sst.reader;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
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
    private int sceneRasterWidth;
    private int sceneRasterHeight;

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

        String productName = getVariable("Header.product", headerStructure).readScalarString();
        sceneRasterWidth = getVariable("Header.iw", headerStructure).readScalarInt();
        sceneRasterHeight = getVariable("Header.ih", headerStructure).readScalarInt();
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
        readChunk(seaIceSourcefile.getAbsolutePath(), new long[]{0, 0}, new long[]{10, 10});
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
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                                       int sourceHeight,
                                                       int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                                       int destOffsetY, int destWidth, int destHeight,
                                                       ProductData destBuffer,
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

    private void readCompact(String filename) {
        int file_id = -1;
        int filespace_id = -1;
        int dataset_id = -1;
        int dcpl_id = -1;
        float[][] dsetData = new float[sceneRasterWidth][sceneRasterHeight];

        // Open file and dataset using the default properties.
        try {
            file_id = H5.H5Fopen(filename, HDF5Constants.H5F_ACC_RDONLY,
                                 HDF5Constants.H5P_DEFAULT);
            // Open an existing dataset.
            String DATASETNAME = "Data/data[00]";
            if (file_id >= 0) {
                dataset_id = H5.H5Dopen(file_id, DATASETNAME);
            }

            // Retrieve the dataset creation property list.
            if (dataset_id >= 0) {
                dcpl_id = H5.H5Dget_create_plist(dataset_id);
            }

            // Read the data using the default properties.
            if (dataset_id >= 0) {
                H5.H5Dread(dataset_id, HDF5Constants.H5T_FLOAT,
                           HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,
                           HDF5Constants.H5P_DEFAULT, dsetData);
            }

            // Output the data to the screen.
            System.out.println("Data for " + DATASETNAME + " is: ");
            for (int indx = 0; indx < sceneRasterWidth; indx++) {
                System.out.print(indx + ": [ ");
                for (int jndx = 0; jndx < sceneRasterHeight; jndx++) {
                    System.out.print(dsetData[indx][jndx] + " ");
                }
                System.out.println("]");
            }
            System.out.println();

            // End access to the dataset and release resources used by it.
            if (dcpl_id >= 0) {
                H5.H5Pclose(dcpl_id);
            }
            if (dataset_id >= 0) {
                H5.H5Dclose(dataset_id);
            }
            if (filespace_id >= 0) {
                H5.H5Sclose(filespace_id);
            }

            // Close the file.
            if (file_id >= 0) {
                H5.H5Fclose(file_id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void readChunk(String filename, long[] origin, long[] shape) {
        final int width = (int) shape[0];
        final int height = (int) shape[1];
        float[][] dsetData = new float[width][height];

        try {
            // Open an existing file.
            int file_id = H5.H5Fopen(filename, HDF5Constants.H5F_ACC_RDONLY,
                                     HDF5Constants.H5P_DEFAULT);

            // Open an existing dataset.
            int dataset_id = H5.H5Dopen(file_id, "Data/data[00]");

            // Retrieve the dataset creation property list.
            int dcpl_id = H5.H5Dget_create_plist(dataset_id);

//            // Read the data using the default properties.
//            H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT,
//                       HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,
//                       HDF5Constants.H5P_DEFAULT, dsetData);

//            // Initialize the read array.
//            for (int indx = 0; indx < width; indx++) {
//                for (int jndx = 0; jndx < height; jndx++) {
//                    dset_data[indx][jndx] = 0;
//                }
//            }

            // Define and select the hyperslab to use for reading.
            int filespace_id = H5.H5Dget_space(dataset_id);

            long[] stride = {1, 1};
            long[] block = {origin[0] + width, origin[1] + height};

            H5.H5Sselect_hyperslab(filespace_id, HDF5Constants.H5S_SELECT_SET,
                                   origin, stride, shape, block);

            // Read the data using the previously defined hyperslab.
            H5.H5Dread(dataset_id, HDF5Constants.H5T_INTEGER,
                       HDF5Constants.H5S_ALL, filespace_id, HDF5Constants.H5P_DEFAULT,
                       dsetData);

            // Output the data to the screen.
            System.out.println("Data as read from disk by hyberslab:");
            for (int indx = 0; indx < width; indx++) {
                System.out.print(" [ ");
                for (int jndx = 0; jndx < height; jndx++) {
                    System.out.print(dsetData[indx][jndx] + " ");
                }
                System.out.println("]");
            }
            System.out.println();

            // End access to the dataset and release resources used by it.
            H5.H5Pclose(dcpl_id);
            H5.H5Dclose(dataset_id);
            H5.H5Sclose(filespace_id);
            H5.H5Fclose(file_id);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
