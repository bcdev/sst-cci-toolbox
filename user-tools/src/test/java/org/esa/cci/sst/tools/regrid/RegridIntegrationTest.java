package org.esa.cci.sst.tools.regrid;

import static org.esa.cci.sst.tools.regrid.RegriddingTool.PARAM_CLIMATOLOGY_DIR;
import static org.esa.cci.sst.tools.regrid.RegriddingTool.PARAM_COVERAGE_UNCERTAINTY_FILE_STDDEV;
import static org.esa.cci.sst.tools.regrid.RegriddingTool.PARAM_COVERAGE_UNCERTAINTY_FILE_X0SPACE;
import static org.esa.cci.sst.tools.regrid.RegriddingTool.PARAM_COVERAGE_UNCERTAINTY_FILE_X0TIME;
import static org.esa.cci.sst.tools.regrid.RegriddingTool.PARAM_END_DATE;
import static org.esa.cci.sst.tools.regrid.RegriddingTool.PARAM_FILENAME_REGEX;
import static org.esa.cci.sst.tools.regrid.RegriddingTool.PARAM_OUTPUT_DIR;
import static org.esa.cci.sst.tools.regrid.RegriddingTool.PARAM_PRODUCT_TYPE;
import static org.esa.cci.sst.tools.regrid.RegriddingTool.PARAM_SPATIAL_RESOLUTION;
import static org.esa.cci.sst.tools.regrid.RegriddingTool.PARAM_SST_DEPTH;
import static org.esa.cci.sst.tools.regrid.RegriddingTool.PARAM_START_DATE;
import static org.esa.cci.sst.tools.regrid.RegriddingTool.PARAM_TEMPORAL_RES;
import static org.esa.cci.sst.tools.regrid.RegriddingTool.PARAM_TOTAL_UNCERTAINTY;
import static org.junit.Assert.*;

import org.esa.beam.dataio.netcdf.GenericNetCdfReaderPlugIn;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.cci.sst.IoTestRunner;
import org.esa.cci.sst.TestUtil;
import org.esa.cci.sst.common.SpatialResolution;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.TemporalResolution;
import org.esa.cci.sst.product.ProductType;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.Parameter;
import org.junit.*;
import org.junit.runner.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RunWith(IoTestRunner.class)
public class RegridIntegrationTest {

    private File testDataDir;
    private RegriddingTool regriddingTool;
    private final int[] pixelX = new int[]{
                12, 13, 18, 2, 27, 32, 40, 43, 43, 45,
                51, 57, 66, 72, 79, 76, 89, 112, 121, 115,
                123, 118, 165, 172, 177, 157, 150, 152, 168, 145,
                140, 188, 234, 237, 247, 255, 258, 264, 239, 229,
                194, 218, 291, 282, 280, 288, 275, 316, 331, 354,
                358, 335, 309, 314, 324, 317, 347, 349, 355, 342
    };
    private final int[] pixelY = new int[]{
                62, 76, 96, 120, 123, 97, 62, 46, 90, 138,
                126, 100, 76, 103, 138, 119, 65, 57, 49, 73,
                137, 143, 135, 137, 123, 118, 127, 93, 51, 45,
                63, 21, 97, 126, 138, 116, 98, 73, 72, 117,
                55, 46, 70, 101, 119, 134, 133, 142, 136, 146,
                116, 124, 100, 76, 73, 62, 60, 72, 98, 95
    };
    private final static Map<String, Double[]> expectedBandValues;

    static {
        expectedBandValues = new LinkedHashMap<>();
        expectedBandValues.put("sst_skin", new Double[]{
                    297.53552, 299.6954, 302.14374, 293.07208, 291.37454, 301.6425, 295.14438, 284.24957, 299.43237, 282.1154,
                    289.91583, 300.24045, 300.97415, 297.80212, 282.00833, 294.25424, 299.57224, 294.96246, 290.996, 301.3702,
                    279.4241, 278.64697, 280.03238, 276.25928, 291.53036, 294.46747, 288.71313, 300.7765, 290.58423, 288.255,
                    297.00266, 281.42133, 302.06076, 290.84097, 276.77298, 296.22708, 300.84122, 303.50943, 303.1781, 296.86008,
                    294.40005, 295.3691, 302.17758, 302.0669, 292.31366, 282.90527, 283.1001, 278.60718, 283.82056, 278.11765,
                    295.43542, 293.11172, 301.9494, 302.26733, 302.355, 297.2374, 297.1804, 301.2131, 302.831, 303.2647
        });
        expectedBandValues.put("sst_skin_anomaly", new Double[]{
                    1.0048109, -0.07716012, -0.18578544, -0.0077203647, -0.0571134, -0.22671428, 0.80122256, -0.47541666, -0.15676087, 0.46410257,
                    0.48499998, 0.07056818, 0.009546599, -0.4480851, 0.7783333, 0.19483607, -0.7430836, -0.8767536, -1.7152352, 0.3693093,
                    -1.5733663, -0.49537736, 0.1864, -0.22146341, 0.19718309, -0.3286802, -0.28, 0.030927151, -0.1434728, -0.2725,
                    0.22893333, 0.740425, 1.4464964, 0.50609756, -0.5744681, 0.6921519, 0.29689392, 0.27202165, 0.7484073, 0.78502053,
                    0.66315, 3.6799998, 0.5209848, 0.6333045, -0.39415, -0.13756023, -0.30380353, 0.1988, -0.117714286, -0.5441176,
                    -0.16648148, -0.0130612245, 0.43899158, -0.34471908, 0.27541062, -0.027348837, 1.294875, 0.5896104, 0.4865357, 0.61643565
        });
        expectedBandValues.put("coverage_uncertainty", new Double[]{
                    0.019995224, 0.012682332, 0.018245917, 0.018366383, 0.030415474, 0.017965578, 0.013662334, 0.056886535, 0.047940306, 0.053987924,
                    0.07741006, 0.017178958, 0.012892032, 0.031044835, 0.056514945, 0.020212907, 0.016227292, 0.015516445, 0.029215723, 0.010816077,
                    0.02673138, 0.018098159, 0.024968907, 0.019225625, 0.021809658, 0.023095977, 0.020794317, 0.014545944, 0.01725162, 0.1952727,
                    0.012012496, 0.016221877, 0.01929182, 0.050932713, 0.031909812, 0.015378201, 0.025592865, 0.016929956, 0.016234662, 0.017895376,
                    0.01846023, 0.057496972, 0.015848385, 0.017844785, 0.016638614, 0.016584674, 0.011115593, 0.015667837, 0.06530604, 0.06312325,
                    0.022655478, 0.04561728, 0.01774613, 0.01307599, 0.0157459, 0.026609097, 0.022943523, 0.010951769, 0.016882965, 0.017779684
        });
        expectedBandValues.put("uncorrelated_uncertainty", new Double[]{
                    0.0062638517, 0.006120342, 0.00570624, 0.0067979693, 0.011549152, 0.010195975, 0.0076023815, 0.016325943, 0.0059533073, 0.024188336,
                    0.027064094, 0.009367708, 0.0016109095, 0.018979153, 0.027108833, 0.00661441, 0.0074330447, 0.0036107611, 0.010558864, 0.0029544116,
                    0.010914578, 0.011437682, 0.008668841, 0.009056925, 0.0073723868, 0.007641069, 0.009313588, 0.010193083, 0.004063535, 0.10253048,
                    0.005579757, 4.9999997E-4, 0.010347278, 0.021466186, 0.017510263, 0.0076563475, 0.014926357, 0.0064683426, 0.0043638297, 0.012600121,
                    4.9999997E-4, 0.0118299285, 0.0040647937, 0.010216127, 0.0014426538, 0.0022696536, 0.0021873785, 0.0013124405, 0.027489515, 0.036852833,
                    0.0060664047, 0.01730096, 0.013376002, 0.0076629296, 0.008244609, 0.014598058, 0.0018975312, 0.0049372343, 0.005633582, 0.019182386
        });
        expectedBandValues.put("large_scale_correlated_uncertainty", new Double[]{
                    0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994,
                    0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994,
                    0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994,
                    0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994,
                    0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994,
                    0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994, 0.099999994
        });
        expectedBandValues.put("synoptically_correlated_uncertainty", new Double[]{
                    0.19472633, 0.32791182, 0.15113416, 0.30814626, 0.15012410, 0.46486246, 0.35800042, 0.24278147, 0.11716835, 0.22430323,
                    0.31806015, 0.42976707, 0.15693773, 0.15206642, 0.09828006, 0.29213628, 0.43012574, 0.15379893, 0.36263567, 0.15722428,
                    0.16929240, 0.17950907, 0.14223138, 0.13542875, 0.34284090, 0.14957681, 0.29697361, 0.14745774, 0.15042981, 0.33470052,
                    0.35648831, 0.12634646, 0.46659877, 0.15068125, 0.16385088, 0.36534515, 0.46776118, 0.44885790, 0.37372946, 0.40911337,
                    0.23054374, 0.29698944, 0.55128592, 0.53095871, 0.30659469, 0.08032747, 0.14370234, 0.08027320, 0.21236993, 0.16534969,
                    0.14939321, 0.26352763, 0.50448000, 0.50464874, 0.15470390, 0.35647240, 0.14450690, 0.30115994, 0.15650765, 0.58250069
        });
        expectedBandValues.put("adjustment_uncertainty", new Double[]{
                    0.04233116, 0.03068042, 0.03474048, 0.04763828, 0.00766085, 0.04344904, 0.02637302, 0.01778915, 0.02753752, 0.06247894,
                    0.05330995, 0.04352650, 0.03487505, 0.02623376, 0.00847605, 0.04650592, 0.02891167, 0.04408175, 0.01998018, 0.03493873,
                    0.06173926, 0.06535193, 0.01777892, 0.06229940, 0.05294253, 0.02474377, 0.05306900, 0.03464563, 0.04424406, 0.00916901,
                    0.02626980, 0.07193827, 0.04344959, 0.00886360, 0.06244321, 0.04604330, 0.04350253, 0.03477607, 0.03330050, 0.05275367,
                    0.01189892, 0.02567768, 0.02624795, 0.04280174, 0.04756107, 0.00887835, 0.05400473, 0.01783848, 0.06243246, 0.06302270,
                    0.02557183, 0.03886438, 0.04352939, 0.03470478, 0.03494868, 0.02637892, 0.04308173, 0.02419098, 0.02608460, 0.04340832
        });
    }

    @Before
    public void setUp() throws IOException {
        testDataDir = TestUtil.getTestDataDirectory();
        regriddingTool = new RegriddingTool();
    }

    @Test
    public void testNoCmdLineArgs() {
        try {
            regriddingTool.run(new Configuration(), new String[0]);
            fail("ToolException expected");
        } catch (Exception expected) {
        }
    }

    @Test
    @Ignore
    public void test_L3U_one_day_10_deg_noTotalUncert() throws IOException {
        System.out.print(testDataDir.getAbsolutePath());
        System.out.println("testDataDir = " + testDataDir.getAbsolutePath());

        final Configuration configuration = new Configuration();
        configuration.setToolHome(testDataDir.getAbsolutePath());

        configuration.put(PARAM_PRODUCT_TYPE.getName(), "CCI_L3U");
        configuration.put("CCI_L3U.dir", new File(testDataDir, "L3U").getAbsolutePath());

        configuration.put(PARAM_START_DATE.getName(), "2007-05-15");
        configuration.put(PARAM_END_DATE.getName(), "2007-07-15");
        configuration.put(PARAM_SPATIAL_RESOLUTION.getName(), SpatialResolution.DEGREE_1_00.getResolution() + "");
        configuration.put(PARAM_TEMPORAL_RES.getName(), TemporalResolution.daily.toString());
        File outDir = new File(testDataDir, "out");
        configuration.put(PARAM_OUTPUT_DIR.getName(), outDir.getAbsolutePath());

        configuration.put(PARAM_SST_DEPTH.getName(), SstDepth.skin.toString());
        configuration.put(PARAM_TOTAL_UNCERTAINTY.getName(), "false");
        configuration.put(PARAM_CLIMATOLOGY_DIR.getName(), new File(testDataDir.getAbsolutePath(), "climatology").getAbsolutePath());

        configuration.put(PARAM_FILENAME_REGEX.getName(), ProductType.CCI_L3U.getDefaultFilenameRegex());

        putFileParam(configuration, PARAM_COVERAGE_UNCERTAINTY_FILE_STDDEV);
        putFileParam(configuration, PARAM_COVERAGE_UNCERTAINTY_FILE_X0TIME);
        putFileParam(configuration, PARAM_COVERAGE_UNCERTAINTY_FILE_X0SPACE);

        regriddingTool.run(configuration, new String[0]);

        final File[] files = outDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("\\d{8}-\\d{8}-.*");
            }
        });
        assertEquals(1, files.length);
        final File productFile = files[0];
        productFile.deleteOnExit();

        final GenericNetCdfReaderPlugIn rpi = new GenericNetCdfReaderPlugIn();
        final ProductReader readerInstance = rpi.createReaderInstance();
        final Product product = readerInstance.readProductNodes(productFile, null);

        assertNotNull(product);
        for (String bandName : expectedBandValues.keySet()) {
            assertBand(product, bandName);
        }
    }

    private void assertBand(Product product, String bandName) throws IOException {
        final Band band = product.getBand(bandName);
        assertNotNull(bandName, band);
        final Double[] expected = expectedBandValues.get(bandName);
        band.readRasterDataFully();
        for (int i = 0; i < expected.length; i++) {
            final int x = pixelX[i];
            final int y = pixelY[i];
            final double v = band.getPixelDouble(x, y);
            final Double exp = expected[i];
            assertEquals(bandName + " array pos[" + i + "]", exp, v, 1e-4);
        }
    }

    private void putFileParam(Configuration configuration, Parameter fileParam) {
        configuration.put(fileParam.getName(), removeRelativeDot(fileParam.getDefaultValue()));
    }

    private String removeRelativeDot(String filePath) {
        if (filePath != null && filePath.startsWith(".")) {
            return filePath.substring(1);
        }
        return filePath;
    }
}
