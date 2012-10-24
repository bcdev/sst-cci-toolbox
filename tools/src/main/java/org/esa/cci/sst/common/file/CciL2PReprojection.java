package org.esa.cci.sst.common.file;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;
import org.esa.cci.sst.common.cellgrid.GridDef;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.awt.*;
import java.io.*;
import java.util.Arrays;

/**
 * This class does a reprojection of an cci-L2P product per variable/band.
 * Satellite coordinates are changed to Lat-Lon coordinate system.
 * The result is fully comparable with an sst-cci-L3U product.
 * The result is collected in a NetCdf-Array.
 *
 * {@author Bettina Scholze, Sabine Embacher}
 * Date: 18.10.12 09:28
 */
public class CciL2PReprojection {

    private String variable; /*band name*/
    private Array array; /*One variable to be put in one ArrayGrid*/
    GridDef gridDef = AbstractCciFileType.GRID_DEF;
    private final int STEP_SIZE = 1000;
    private Number fillValue;
    private double scaling;
    private double offset;
    private static File logfile = new File("C:/Users/bettina/Desktop/log.txt");


    public Array doReprojection(File file, String format, String variable) throws IOException {
        append(variable);

        this.array = initialiseArray();
        this.variable = variable;
        Product product = ProductIO.readProduct(file, format); //opens product
        this.fillValue = product.getBand(variable).getNoDataValue();
        this.scaling = product.getBand(variable).getScalingFactor();
        this.offset = product.getBand(variable).getScalingOffset();

        int maxSteps = calculateMaxSteps(product.getSceneRasterHeight());
        for (int step = 0; step < maxSteps; step++) {
            doReprojectionStep(step, product);
        }
        return array;
    }

    int calculateMaxSteps(int height) {
        return (int) Math.ceil(height / (double) STEP_SIZE);
    }

    void doReprojectionStep(int step, Product product) throws IOException {
        Product subProduct = subset(product, step);
        //determine bounding box from product subset, align them to target resolution by a shift up-left
        double latMin = subProduct.getBand("lat").getStx(true, ProgressMonitor.NULL).getMinimum();
        double latMax = subProduct.getBand("lat").getStx(true, ProgressMonitor.NULL).getMaximum();
        double lonMin = subProduct.getBand("lon").getStx(true, ProgressMonitor.NULL).getMinimum();
        double lonMax = subProduct.getBand("lon").getStx(true, ProgressMonitor.NULL).getMaximum();

        double resolution = gridDef.getResolution();
        double lonMinLeftShifted = shiftLeft(lonMin, resolution);
        double latMaxUpShifted = shiftUp(latMax, resolution);
        DoublePoint origin = new DoublePoint(lonMinLeftShifted, latMaxUpShifted); //unit: degree
        IntPoint extent = createBoundingBoxInPixels(origin, lonMax, latMin, resolution); //unit: number of pixels

        Product reprojectProduct = reproject(subProduct, origin, extent); //product piece with aligned bounding box

        //transfer values from bounding box in reprojected product to array
        double[] values = new double[extent.getWidth() * extent.getHeight()];
        reprojectProduct.getBand(variable).readPixels(0, 0, extent.getWidth(), extent.getHeight(), values);

        int offsetY = (int) Math.round((90 - latMaxUpShifted) / 0.05);
        int offsetX = (int) Math.round(Math.abs(-180 - lonMinLeftShifted) / 0.05);
        int startIndex = offsetY * extent.getWidth() + offsetX;

        int existentValueCounter = 0;
        int nonNanCounter = 0;
        int valueCounter = 0;
        for (int y = 0; y < extent.getHeight(); y++) {
            for (int x = 0; x < extent.getWidth(); x++) {
                valueCounter++;
                int sourceIndex = y * extent.getWidth() + x;
                int targetIndex = startIndex + y * gridDef.getWidth() + x;
                double existentValue = this.array.getDouble(targetIndex);
                double recentValue = values[sourceIndex];
                if (!Double.isNaN(existentValue)) {
                    recentValue = (existentValue + recentValue) / 2.0;
                    existentValueCounter++;
                }
                this.array.setDouble(targetIndex, recentValue);
                //debug
                if (!Double.isNaN(recentValue)) {
                    nonNanCounter++;
                }
            }
        }
        append("Had to overwrite: " + existentValueCounter + " values.");
        append(" There are: " + valueCounter + " entries in values.");
        append(" Non-NaN values : " + nonNanCounter);
    }

    public static void append(String entry) throws IOException {
        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(logfile, true));
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.print(entry);
        printWriter.flush();
        printWriter.close();
    }

    IntPoint createBoundingBoxInPixels(DoublePoint origin, double lonMax, double latMin, double resolution) {
        double widthInDegree = lonMax - origin.getX();
        double heightInDegree = origin.getY() - latMin;
        int width = (int) Math.ceil(widthInDegree / resolution);
        int height = (int) Math.ceil(heightInDegree / resolution);
        return new IntPoint(width, height);
    }

    double shiftUp(double latMax, double resolution) {
        double latMaxUpShifted;
        double remain = latMax % resolution;
        if (latMax < 0 || remain == 0.0) {
            latMaxUpShifted = latMax - remain;
        } else {
            latMaxUpShifted = latMax - remain + 0.5;
        }
        return latMaxUpShifted;
    }

    double shiftLeft(double lonMin, double resolution) {
        double lonMinLeftShifted;
        double remain = lonMin % resolution;
        if (lonMin > 0 || remain == 0.0) {
            lonMinLeftShifted = lonMin - remain;
        } else {
            lonMinLeftShifted = lonMin - remain - resolution;
        }
        return lonMinLeftShifted;
    }

    Array initialiseArray() {
        double[] data = new double[gridDef.getWidth() * gridDef.getHeight()];
        Arrays.fill(data, Double.NaN);
        Array newArray = Array.factory(DataType.DOUBLE, new int[]{gridDef.getHeight(), gridDef.getWidth()}, data);
        this.array = newArray;
        return newArray;
    }

    Product subset(Product product, int step) { //step starts with 0
        SubsetOp subsetOp = new SubsetOp();
        subsetOp.setSourceProduct(product);

        int stepSizeY = 1000;
        int indexY = step * stepSizeY; //starts with 0;
        int x = product.getSceneRasterWidth();
        subsetOp.setRegion(new Rectangle(0, indexY, x, stepSizeY));
        return subsetOp.getTargetProduct();
    }

    Product reproject(Product subProduct, DoublePoint origin, IntPoint boundingBoxExtension) {
        ReprojectionOp reprojectionOp = new ReprojectionOp();
        reprojectionOp.setSourceProduct(subProduct);

        reprojectionOp.setParameter("crs", "GEOGCS[\"WGS84(DD)\", \n" +
                "  DATUM[\"WGS84\", \n" +
                "    SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], \n" +
                "  PRIMEM[\"Greenwich\", 0.0], \n" +
                "  UNIT[\"degree\", 0.017453292519943295], \n" +
                "  AXIS[\"Geodetic longitude\", EAST], \n" +
                "  AXIS[\"Geodetic latitude\", NORTH]]");

        reprojectionOp.setParameter("resampling", "Nearest");
        reprojectionOp.setParameter("easting", origin.getX());
        reprojectionOp.setParameter("northing", origin.getY());
        reprojectionOp.setParameter("referencePixelX", 0.0);
        reprojectionOp.setParameter("referencePixelY", 0.0);
        reprojectionOp.setParameter("orientation", 0.0);
        reprojectionOp.setParameter("pixelSizeX", gridDef.getResolutionX());
        reprojectionOp.setParameter("pixelSizeY", gridDef.getResolutionY());
        reprojectionOp.setParameter("width", boundingBoxExtension.getWidth());
        reprojectionOp.setParameter("height", boundingBoxExtension.getHeight());
        reprojectionOp.setParameter("orthorectify", false);
        reprojectionOp.setParameter("noDataValue", "NaN");
        return reprojectionOp.getTargetProduct();
    }

    public Array getArray() {
        return array;
    }

    public Number getFillValue() {
        return fillValue;
    }

    public double getScaling() {
        return scaling;
    }

    public double getOffset() {
        return offset;
    }

    class DoublePoint {

        private final double x;
        private final double y;

        DoublePoint(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

    }

    class IntPoint {

        private final int width;
        private final int height;

        public IntPoint(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

    }

    void setVariable(String variable) { //for test only
        this.variable = variable;
    }
}
