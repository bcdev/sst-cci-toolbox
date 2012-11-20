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

/**
 * {@author Bettina Scholze}
 * Date: 09.11.12 15:32
 */
public class LutForXTimeSpace {

    private static final GridDef sourceGridDef = GridDef.createGlobal(2.0);
    private static final int LENGTH_OF_A_LUT_ENTRY = 9;

    private Grid lutGrid; //in target resolution

    private LutForXTimeSpace(Grid lutGrid) {
        this.lutGrid = lutGrid;
    }

    /**
     * Reads in the LUT for Xtime, which is in 2.0 degree, and scales it to the target resolution.
     *
     * @param file             path to the LUT in 2.0 degree resolution
     * @param targetResolution grid resolution of the result product
     * @param fillValue        The fill value in the LUT file
     * @return LutForXTime object scaled into target resolution
     * @throws IOException
     */
    public static LutForXTimeSpace read(File file, SpatialResolution targetResolution, double fillValue) throws IOException {

        ArrayGrid lutIn2Degree = readInAndFlip(file, fillValue);

        ArrayGrid lutIn005Degree = interpolateTo005(lutIn2Degree);

        int scaleFactor = (int) (targetResolution.getValue() / 0.05);
        ArrayGrid lutInTargetResolution = lutIn005Degree.scaleDown(scaleFactor, scaleFactor);

        return new LutForXTimeSpace(lutInTargetResolution);
    }

    /**
     * Get X-time in target resolution
     *
     * @param cellX x
     * @param cellY y
     * @return
     */
    public double getXTime(int cellX, int cellY) {
        return this.lutGrid.getSampleDouble(cellX, cellY);
    }

    static ArrayGrid readInAndFlip(File file, double fillValue) throws IOException {
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
                    if (value == fillValue) {
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

    static ArrayGrid interpolateTo005(ArrayGrid lut) {
        ArrayGrid finerArrayGrid = ArrayGrid.createWith2DDoubleArray(GridDef.createGlobal(0.05));

        int sourceHeight = lut.getHeight();
        int sourceWidth = lut.getWidth();
        int targetHeight = finerArrayGrid.getHeight();
        int targetWidth = finerArrayGrid.getWidth();
        double sizeX = 0.05; //in degree
        double sizeY = 0.05; //in degree

        for (int y = 0; y < targetHeight; y++) { //finer (target)
            for (int x = 0; x < targetWidth; x++) {
                double shift = 0.025; //in degree
                double modelX = x * sizeX + shift; //in degree
                int cornerIndexX = calculateCorner(modelX);
                if (cornerIndexX == sourceWidth - 1) {
                    cornerIndexX--;
                }
                double modelY = y * sizeY + shift; //in degree
                int cornerIndexY = calculateCorner(modelY);
                if (cornerIndexY == sourceHeight - 1) {
                    cornerIndexY--;
                }

                //calculate weight between the corners
                double ww = Math.min(1.0, Math.max(0.0, (modelX - (cornerIndexX * 2.0 + 1.0)) / 2.0));
                double wh = Math.min(1.0, Math.max(0.0, (modelY - (cornerIndexY * 2.0 + 1.0)) / 2.0));

                //get corners' values
                double v00 = lut.getSampleDouble(cornerIndexX, cornerIndexY);
                double v10 = lut.getSampleDouble(cornerIndexX + 1, cornerIndexY);
                double v01 = lut.getSampleDouble(cornerIndexX, cornerIndexY + 1);
                double v11 = lut.getSampleDouble(cornerIndexX + 1, cornerIndexY + 1);
                //interpolate
                double value = MathUtils.interpolate2D(ww, wh, v00, v10, v01, v11);
                finerArrayGrid.setSample(x, y, value);
            }
        }
        return finerArrayGrid;
    }

    static int calculateCorner(double positionInDegree) {
        return (int) Math.max(0.0, Math.floor((positionInDegree - 1.0) / 2.0));
    }
}
