package org.esa.cci.sst.common.auxiliary;

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
public class LutForXTime {

    private static final GridDef sourceGridDef = GridDef.createGlobal(2.0);
    private SpatialResolution targetResolution;
    private final double fillValue;
    private Grid lutGrid;

    private LutForXTime(Grid lutGrid, SpatialResolution targetResolution, double fillValue) {
        this.lutGrid = lutGrid;
        this.fillValue = fillValue;
        this.targetResolution = targetResolution;
    }

    public static LutForXTime read(File file, SpatialResolution targetResolution, double fillValue) throws IOException {
        int width = sourceGridDef.getWidth();
        int height = sourceGridDef.getHeight();
        double[] data = new double[width * height];
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));

            for (int y = 0; y < height; y++) {
                String line = bufferedReader.readLine();
                for (int x = 0; x < width; x++) {
                    String substring = line.substring(x * 9, x * 9 + 9);
                    data[width * y + x] = Double.parseDouble(substring);
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

        ArrayGrid lutGrid = ArrayGrid.createWith2DDoubleArray(sourceGridDef, data);
        return new LutForXTime(lutGrid, targetResolution, fillValue);
    }

    public double getXTime(int cellX, int cellY) {
       return this.lutGrid.getSampleDouble(cellX,cellY);
    }

//    private Grid read() {
//
//    }
//
//    private Grid scaleTo005() {
//
//    }
//
//    private Grid scaleToTargetResolution() {
//
//    }
//

}
