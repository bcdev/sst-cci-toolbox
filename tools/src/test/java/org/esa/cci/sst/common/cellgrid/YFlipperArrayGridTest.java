package org.esa.cci.sst.common.cellgrid;

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.util.Locale;

import static junit.framework.Assert.assertEquals;

/**
 * {@author Bettina Scholze}
 * Date: 22.11.12 16:18
 */
public class YFlipperArrayGridTest {

    private static final double[] input = new double[]{
            999, 0.2, 999, 0.4, 0.1, 0.2, 12.0, 0.4,
            999, 0.3, 2.0, 9.0, 0.2, 0.3, 0.4,  2.2,
            0.3, 999, 0.1, 0.2, 0.3, 0.4, 33.0, 1.1,
            999, 0.1, 0.2, 8.3, 0.4, 0.1, 1.0,  0.1,
    };
    private GridDef gridDef;
    private ArrayGrid arrayGrid;
    private YFlipperArrayGrid yFlipperArrayGrid;


    @Before
    public void setUp() throws Exception {
        gridDef = GridDef.createGlobalGrid(8, 4);
        int[] shape = {gridDef.getHeight(), gridDef.getWidth()};
        Array array = Array.factory(DataType.DOUBLE, shape, input);
        arrayGrid = new ArrayGrid(gridDef, array, 999, 1.0, 0.0);
        yFlipperArrayGrid = new YFlipperArrayGrid(arrayGrid);
    }

    @Test
    public void testTestFlip() throws Exception {

        final String expected = "" +
                "   NaN   0.10   0.20   8.30   0.40   0.10   1.00   0.10 \n" +
                "  0.30    NaN   0.10   0.20   0.30   0.40  33.00   1.10 \n" +
                "   NaN   0.30   2.00   9.00   0.20   0.30   0.40   2.20 \n" +
                "   NaN   0.20    NaN   0.40   0.10   0.20  12.00   0.40 \n";

        String result = createResultString();
        assertEquals(expected, result);
    }

    private String createResultString() {
        final Locale theDefault = Locale.getDefault();
        Locale.setDefault(new Locale("en", "US"));

        StringBuilder stringBuilder = new StringBuilder();
        for (int y = 0; y < gridDef.getHeight(); y++) {
            for (int x = 0; x < gridDef.getWidth(); x++) {
                double value = yFlipperArrayGrid.getSampleDouble(x, y);
                stringBuilder.append(String.format("%6.2f ", value));
            }
            stringBuilder.append("\n");
        }
        Locale.setDefault(theDefault);
        return stringBuilder.toString();
    }
}
