package org.esa.cci.sst.common.auxiliary;

import org.esa.cci.sst.common.cellgrid.ArrayGrid;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;

/**
 * {@author Bettina Scholze}
 * Date: 20.11.12 14:33
 */
public class LutForStdDeviationTest {

    private File file;

    @Before
    public void setUp() throws Exception {
        file = new File("./src/main/conf/auxdata/20070321-UKMO-L4HRfnd-GLOB-v01-fv02-OSTIARANanom_stdev_subset.nc");
    }

    private LutForStdDeviation createLutForStdDeviation(GridDef gridDef) throws IOException {
        try {
            return LutForStdDeviation.create(file, gridDef);
        } catch (Exception e) {
            throw new RuntimeException("Lut '20070321-UKMO-L4HRfnd-GLOB-v01-fv02-OSTIARANanom_stdev.nc' not found. " +
                    "Get the Lut from fs1:/projects/ongoing/SST-CCI/docs/technical-specification/tools/ " +
                    "and put it in: ./src/main/conf/auxdata/");
        }
    }

    @Ignore
    @Test
    public void test90() throws Exception {
        LutForStdDeviation lutForStdDeviation = createLutForStdDeviation(GridDef.createGlobal(90.0));

        ArrayGrid grid = lutForStdDeviation.getStdDeviationGrid();
        assertEquals(2, grid.getHeight());
        assertEquals(4, grid.getWidth());

        final String expected = "" +
                " 47,20  40,11  23,81  39,62 \n" +
                " 53,52  34,81  35,37  35,54 \n";

        String result = createResultString(grid);
        assertEquals(expected.trim(), result.trim());
    }

    @Test
    public void test01() throws Exception {
        LutForStdDeviation lutForStdDeviation = createLutForStdDeviation(GridDef.createGlobal(0.1));

        ArrayGrid grid = lutForStdDeviation.getStdDeviationGrid();
        assertEquals(1800, grid.getHeight());
        assertEquals(3600, grid.getWidth());

//        grid.setVariable("uncertainty");
//        final Grid[] grids = new Grid[1];
//        grids[0] = grid;
//        writeIntermediate(grids);
    }

    private void writeIntermediate(Grid[] grids) throws IOException {
        GridDef gridDef = grids[0].getGridDef();
        final File file = new File("C:\\Users\\bettina\\Development\\test-data\\sst-cci\\arc\\output\\regridding\\2012-11-22", "intermediate.nc");
        //global attributes
        NetcdfFileWriteable netcdfFile = NetcdfFileWriteable.createNew(file.getPath());
        try {
            netcdfFile.addGlobalAttribute("title", "some title");
            //global dimensions
            Dimension latDim = netcdfFile.addDimension("lat", gridDef.getHeight());
            Dimension lonDim = netcdfFile.addDimension("lon", gridDef.getWidth());
            Dimension[] dimensionMeasurementRelated = {latDim, lonDim};

            Variable uuVar = netcdfFile.addVariable("uncertainty", DataType.FLOAT, dimensionMeasurementRelated);
            uuVar.addAttribute(new Attribute("units", "kelvin"));
            uuVar.addAttribute(new Attribute("long_name", String.format("uncorrelated_uncertainty")));
            uuVar.addAttribute(new Attribute("_FillValue", Float.NaN));

            Variable[] variables = new Variable[]{uuVar};
            netcdfFile.create();
            //add data variables
            for (int i = 0; i < variables.length; i++) {
                ArrayGrid grid = (ArrayGrid) grids[i];
                try {
                    netcdfFile.write(grid.getVariable(), grid.getArray());
                } catch (InvalidRangeException e) {
                    System.out.println("Regridding Tool" + "writeDataToNetCdfFile" + e.getMessage());
                }
            }
        } finally {
            try {
                netcdfFile.flush();
                netcdfFile.close();
            } catch (IOException e) {// ignore
            }
        }
    }

    @Ignore
    @Test
    public void test10() throws Exception {
        LutForStdDeviation lutForStdDeviation = createLutForStdDeviation(GridDef.createGlobal(10.0));

        ArrayGrid grid = lutForStdDeviation.getStdDeviationGrid();
        assertEquals(18, grid.getHeight());
        assertEquals(36, grid.getWidth());

        final String expected = "" +
                 "  7,65   2,67   0,39   0,01   0,00   0,00   0,00   0,01   0,03   0,10   0,08   0,53   0,00   0,00   0,02   0,48   4,63   6,22   9,64  17,45  14,13  11,93  12,02   7,99   4,99   5,98   6,65   3,76   1,46   2,38   7,22  17,26  16,46  13,30  11,87   9,80 \n" +
                "115,93  88,99  64,02  64,68  60,70  52,52  15,25  15,72  25,07  11,91  29,06  46,37  19,89   0,00   0,00   6,50  47,82  62,75  72,07  59,77  57,06  85,97 112,69  90,57 100,92  69,36  44,52  14,64   8,42  33,06  62,59  74,32  53,57  77,31 103,38 125,87 \n" +
                " 98,61  66,49   0,84   3,14   6,43   3,20   4,85   4,26  13,04  42,05  29,80  42,53  61,20  13,60  48,83  62,93  55,76  61,69  57,99  26,87  15,49  18,75  36,77  22,32   7,17   2,53   0,00   0,00   0,00   0,00   0,00   0,00   0,00   6,78  10,78  27,48 \n" +
                " 61,23  66,74  57,01  67,08  56,20   5,70   0,00   0,00  12,87  66,03  20,00  10,75  63,27  75,25  78,11  65,01  58,61  35,61  57,73  39,69  19,82   0,03   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00  11,81  98,53  69,81  65,06  60,80 \n" +
                " 78,50  80,23  80,11  82,79  80,68  49,33   0,00   0,00   0,00   0,00   6,02  83,78 105,15  97,76  81,45  81,18  65,92  35,23  24,72  25,82  10,06  46,71  17,58  22,27   0,00   0,00   0,00   0,00   0,00   0,01   2,58  40,42  84,72  93,58  91,16  88,99 \n" +
                " 99,47  95,97  92,55  91,06  79,59  73,63  14,59   0,00   0,16   5,04  64,62  74,63  58,11  59,35  63,34  66,88  61,41  17,37  20,59  58,20  40,82  16,20   2,79  13,30   0,00   0,00   0,00   0,00   0,00   4,30  65,85  67,28  90,70 101,84  97,21 100,24 \n" +
                " 64,81  64,82  59,63  58,14  60,59  66,58  94,71  19,24  42,00  55,61  54,97  49,70  48,56  45,96  47,37  56,97  37,63   0,05   0,00   0,00   0,00  13,74   4,16  24,03  31,02   2,08   3,85   4,21   3,78  22,51  68,43  77,87  75,13  66,54  67,41  67,48 \n" +
                " 43,72  42,99  41,51  47,68  51,50  56,18  56,62  52,80  49,10  35,24  35,40  36,23  38,72  42,34  53,74  68,99  36,94   0,00   0,00   0,00   0,00   4,37  19,04  50,03  51,13  22,61  45,81  41,34  19,79  65,98  49,86  51,34  45,14  43,70  42,75  42,43 \n" +
                " 66,48  74,99  83,79  92,92  97,51  97,18  97,05  97,34 103,01  99,67  15,95   0,65  18,54  40,56  45,41  52,85  49,98  26,17  26,81   0,00   0,00   0,00  18,24  68,82  53,04  54,14  47,86  40,29  45,12  35,36  47,56  52,24  50,28  51,64  55,26  59,06 \n" +
                " 71,11  78,17  86,20 100,81 112,55 112,46 113,33 118,25 132,14 161,32   6,14   0,00   0,20   6,91  24,40  36,25  40,39  43,65  54,07  10,93   0,00   2,02  59,06  69,90  59,64  56,74  56,95  56,89  43,51  40,59  45,28  37,10  28,50  47,74  58,58  65,76 \n" +
                " 64,50  61,22  52,83  47,74  47,77  43,58  42,08  50,47  66,61  81,89  56,53   0,00   0,00   0,01  37,12  43,17  37,37  35,15  48,45  23,12   0,00   5,62  31,08  56,32  65,81  73,60  67,04  55,88  53,71  63,07  37,75  26,07  39,27  52,14  52,20  59,32 \n" +
                " 80,01  82,67  88,08  77,53  68,08  65,57  57,35  53,19  52,08  55,21  69,01   0,00   0,02  43,35  65,39  56,66  49,49  49,44  44,05  31,76   0,00  36,09  46,77  53,15  57,05  63,71  71,14  69,39  60,98  31,09   0,00   0,00   1,72  51,95  60,33  69,49 \n" +
                " 62,04  58,99  62,85  75,16  83,34  90,73  97,52  86,00  76,60  59,02  58,28   2,10  57,03  80,85  73,07  76,60  70,43  60,70  57,88  72,18  54,63  78,00  70,48  66,58  64,03  59,91  62,62  79,02  69,65  58,91  37,30  35,27  10,97  71,99  59,23  53,34 \n" +
                " 68,39  60,43  59,75  75,83  79,14  77,72  72,54  63,12  59,46  60,03  40,76  41,85  83,20  76,60  77,96  68,62  57,46  49,14  46,82  72,99  85,99  73,89  70,54  70,50  66,47  60,89  58,94  55,69  51,87  52,10  48,56  54,05  63,23  67,32  58,48  61,56 \n" +
                " 51,89  52,74  58,01  58,57  63,44  70,79  71,19  59,10  54,93  49,07  43,37  52,18  48,17  49,26  54,65  45,93  35,94  28,69  27,62  30,00  38,14  36,46  35,59  39,27  38,57  35,26  42,02  41,37  37,96  46,87  52,88  59,41  56,91  57,84  54,68  51,50 \n" +
                " 38,17  33,85  30,17  34,23  47,08  54,20  53,98  49,31  45,96  44,12  40,98  25,25  19,09  33,36  28,65  28,31  23,21  22,74  20,94  21,60  25,46  27,65  23,92  18,29  22,87  27,24  16,46  14,57  18,54  21,22  19,92  19,48  20,76  21,64  28,61  35,85 \n" +
                " 15,35  13,20   9,88   6,83   7,02   5,20   6,01   5,93   2,31   4,85   4,64   0,36   9,70   8,73   9,20  11,68   4,40   0,54   0,12   0,01   0,25   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   5,74  16,15 \n" +
                "  0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00   0,00 \n";

        String result = createResultString(grid);
        assertEquals(expected.trim(), result.trim());
    }

    private String createResultString(ArrayGrid grid) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                double value = grid.getSampleDouble(x, y);
                stringBuilder.append(String.format("%6.2f ", value));
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}
