package org.esa.cci.sst.common.auxiliary;

import org.esa.beam.util.math.MathUtils;
import org.esa.cci.sst.common.cellgrid.ArrayGrid;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.regrid.SpatialResolution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static java.lang.Math.floor;

/**
 * {@author Bettina Scholze}
 * Date: 09.11.12 15:32
 */
public class LutForXTime {

    private static final GridDef sourceGridDef = GridDef.createGlobal(2.0);
    private static final int LENGTH_OF_A_LUT_ENTRY = 9;

    private SpatialResolution targetResolution;
    private final double fillValue;
    private Grid lutGrid;

    private LutForXTime(Grid lutGrid, SpatialResolution targetResolution, double fillValue) {
        this.lutGrid = lutGrid;
        this.fillValue = fillValue;
        this.targetResolution = targetResolution;
    }

    public static LutForXTime read(File file, SpatialResolution targetResolution, double fillValue) throws IOException {
        ArrayGrid lut2Degree = readInAndFlip(file, targetResolution, fillValue);

        //todo interpolate to 0.05°
        ArrayGrid lut005Degree = interpolateTo005(lut2Degree);
        //todo scale down to targetResolution


//        LutForXTime lutForXTime = new LutForXTime(lutGridTargetResolution, targetResolution, fillValue)
//        return lutForXTime;
        return null;
    }

    static ArrayGrid readInAndFlip(File file, SpatialResolution targetResolution, double fillValue) throws IOException {
        int width = sourceGridDef.getWidth();
        int height = sourceGridDef.getHeight();
        double[] data = new double[width * height];
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));

            for (int y = 0; y < height; y++) {
                String line = bufferedReader.readLine();
                for (int x = 0; x < width; x++) {
                    String substring = line.substring(x * LENGTH_OF_A_LUT_ENTRY, x * LENGTH_OF_A_LUT_ENTRY + LENGTH_OF_A_LUT_ENTRY);
                    double value = Double.parseDouble(substring);
                    if (value == -32768.0) {
                        value = Double.NaN;
                    }
                    int flippedY = height - y - 1; //origin of lut is left-down, we need upper-left corner
                    data[width * flippedY + x] = value;
                }
            }
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                //ignore
            }
        }

        return ArrayGrid.createWith2DDoubleArray(sourceGridDef, data);
    }

    /**
     * Get X-time in target resolution
     *
     * @param cellX
     * @param cellY
     * @return
     */
    public double getXTime(int cellX, int cellY) {
        return this.lutGrid.getSampleDouble(cellX, cellY);
    }

    static ArrayGrid interpolateTo005(ArrayGrid lut) {
        ArrayGrid arrayGrid = ArrayGrid.createWith2DDoubleArray(GridDef.createGlobal(0.05));

        int sourceHeight = lut.getHeight();
        int sourceWidth = lut.getWidth();
        int height = arrayGrid.getHeight();
        int width = arrayGrid.getWidth();
        int resolutionX = width / sourceWidth;
        int resolutionY = height / sourceHeight;

        for (int h = 0; h < height; h++) { //finer (target)
            for (int w = 0; w < width; w++) {
                double sourceIndexWExact = w * 1.0 / resolutionX;
                int sourceIndexWLeft = calculateFirstSourceIndex(sourceWidth, resolutionX, w, sourceIndexWExact);
                double sourceIndexHExact = h * 1.0 / resolutionY;
                int sourceIndexHUp = calculateFirstSourceIndex(sourceHeight, resolutionY, h, sourceIndexHExact);
//                System.out.print("sourceIndexWLeft = " + sourceIndexWLeft);
//                System.out.println("    sourceIndexWExact = " + sourceIndexWExact + "  w=" + w);
//                System.out.print("sourceIndexHUp = " + sourceIndexHUp);
//                System.out.println("    sourceIndexHExact = " + sourceIndexHExact + " h=" + h);

                //calculate weight between the corners
                double ww = -MathUtils.round(sourceIndexWLeft + 0.5 - sourceIndexWExact, 1000);
                double wh = -MathUtils.round(sourceIndexHUp + 0.5 - sourceIndexHExact, 1000);
//                System.out.print("ww = " + ww);
//                System.out.println("    wh = " + wh);

                //get corners' values
                double v00 = lut.getSampleDouble(sourceIndexWLeft, sourceIndexHUp);
                double v10 = lut.getSampleDouble(sourceIndexWLeft + 1, sourceIndexHUp);
                double v01 = lut.getSampleDouble(sourceIndexWLeft, sourceIndexHUp + 1);
                double v11 = lut.getSampleDouble(sourceIndexWLeft + 1, sourceIndexHUp + 1);
                //interpolate
                double value = MathUtils.interpolate2D(ww, wh, v00, v01, v10, v11);
                arrayGrid.setSample(w, h, value);
            }
        }
        return arrayGrid;
    }

    private static int calculateFirstSourceIndex(int grossGridExtend, int resolution, int indexInFinerGrid, double exactIndexInGrosserGrid) {
        int indexInGrosserGridFloored;
        if (exactIndexInGrosserGrid <= 0.5) {
            indexInGrosserGridFloored = 0; //boundary
        } else if (exactIndexInGrosserGrid >= grossGridExtend - 1) {
            indexInGrosserGridFloored = grossGridExtend - 2; //boundary
        } else {
            int halfBox = resolution / 2;
            indexInGrosserGridFloored = (int) floor((indexInFinerGrid - halfBox) / resolution); //shifted because value is in the middle of the grid box
//            indexInGrosserGridFloored = (int) floor((indexInFinerGrid - 0.5) / resolution); //shifted because value is in the middle of the grid box
        }
        return indexInGrosserGridFloored;
    }

//    private Grid scaleToTargetResolution() {
//
//    }
//

}
