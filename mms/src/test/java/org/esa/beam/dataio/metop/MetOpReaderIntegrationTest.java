package org.esa.beam.dataio.metop;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.cci.sst.IoTestRunner;
import org.esa.cci.sst.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(IoTestRunner.class)
public class MetOpReaderIntegrationTest {

    @Test
    public void testReadProduct() throws IOException {
        final File file = TestUtil.getFileInTestDataDirectory("AVHR_xxx_1B_M02_20080211161603Z_20080211175803Z_N_O_20080211175632Z.nat");

        final MetopReaderPlugIn metopReaderPlugIn = new MetopReaderPlugIn();
        final DecodeQualification decodeQualification = metopReaderPlugIn.getDecodeQualification(file);
        assertEquals(DecodeQualification.INTENDED, decodeQualification);

        final MetopReader metopReader = new MetopReader(metopReaderPlugIn);
        final Product product = metopReader.readProductNodes(file, null);
        assertNotNull(product);
        try {
            final ProductData.UTC startTime = product.getStartTime();
            assertNotNull(startTime);
            assertEquals(1202746563080L, startTime.getAsDate().getTime());

            final ProductData.UTC endTime = product.getEndTime();
            assertNotNull(endTime);
            assertEquals(1202752683082L, endTime.getAsDate().getTime());

            assertTiePointValue("longitude", 10, 10, -123.30519104003906, product);
            assertTiePointValue("latitude", 20, 20, 79.19400024414062, product);

            assertPixelValue("reflec_1", 30, 30, 0.0019087587716057897, product);
            assertPixelValue("reflec_2", 40, 40, 0.0030889855697751045, product);
            assertPixelValue("reflec_3a", 50, 50, 0.0, product);
            assertPixelValue("temp_3b", 60, 60, 242.97332763671875, product);
            assertPixelValue("temp_4", 70, 70, 249.91310119628906, product);
            assertPixelValue("temp_5", 80, 80, 250.7367706298828, product);
            assertPixelValue("internal_target_temperature", 82, 82, 286.13, product);

            assertTiePointValue("sun_zenith", 90, 90, 96.77499389648438, product);
            assertTiePointValue("view_zenith", 100, 100, 59.54999923706055, product);
            assertTiePointValue("sun_azimuth", 110, 110, 134.47000122070312, product);
            assertTiePointValue("view_azimuth", 120, 120, 87.81999969482422, product);

            assertPixelValue("cloud_flags", 130, 130, 16678, product);
            // @todo 3 tb/tb check flags 2016-06-15
            assertPixelValue("quality_indicator_flags", 140, 140, 0, product);
            assertQualityIndicatorFlagCoding(product);

            assertPixelValue("scan_line_quality_flags", 150, 150, 0, product);
            assertScanlineQualityFlagCoding(product);

            assertPixelValue("calibration_quality_ch3b_flags", 160, 160, 0, product);
            assertCalibrationQualityFlagCoding("calibration_quality_ch3b_flags", product);
            assertPixelValue("calibration_quality_ch4_flags", 170, 170, 0, product);
            assertCalibrationQualityFlagCoding("calibration_quality_ch4_flags", product);
            assertPixelValue("calibration_quality_ch5_flags", 180, 180, 0, product);
            assertCalibrationQualityFlagCoding("calibration_quality_ch5_flags", product);

        } finally {
            product.dispose();
        }
    }

    private void assertQualityIndicatorFlagCoding(Product product) {
        final FlagCoding qualityIndicatorFlags = product.getFlagCodingGroup().get("quality_indicator_flags");
        assertNotNull(qualityIndicatorFlags);

        MetadataAttribute flag = qualityIndicatorFlags.getFlag("DO_NOT_USE_SCAN");
        assertNotNull(flag);
        assertEquals(-2147483648L, flag.getData().getElemUInt());
        assertEquals("DO_NOT_USE_SCAN", flag.getName());
        assertEquals("Do not use scan for product generation", flag.getDescription());

        flag = qualityIndicatorFlags.getFlag("FLYWHEEL");
        assertNotNull(flag);
        assertEquals(2097152L, flag.getData().getElemUInt());
        assertEquals("FLYWHEEL", flag.getName());
        assertEquals("Flywheeling detected during this frame- DEFAULT TO ZERO", flag.getDescription());

        flag = qualityIndicatorFlags.getFlag("PSEUDO_NOISE");
        assertNotNull(flag);
        assertEquals(1L, flag.getData().getElemUInt());
        assertEquals("PSEUDO_NOISE", flag.getName());
        assertEquals("Pseudo noise occurred on this frame", flag.getDescription());
    }

    private void assertScanlineQualityFlagCoding(Product product) {
        final FlagCoding qualityIndicatorFlags = product.getFlagCodingGroup().get("scan_line_quality_flags");
        assertNotNull(qualityIndicatorFlags);

        MetadataAttribute flag = qualityIndicatorFlags.getFlag("TIME_FIELD_INF");
        assertNotNull(flag);
        assertEquals(8388608L, flag.getData().getElemUInt());
        assertEquals("TIME_FIELD_INF", flag.getName());
        assertEquals("Time field is bad but can probably be inferred from the previous good time", flag.getDescription());

        flag = qualityIndicatorFlags.getFlag("SCAN_UNCALIB_CHAN");
        assertNotNull(flag);
        assertEquals(2048L, flag.getData().getElemUInt());
        assertEquals("SCAN_UNCALIB_CHAN", flag.getName());
        assertEquals("Some uncalibrated channels on this scan. (See channel indicators.)", flag.getDescription());

        flag = qualityIndicatorFlags.getFlag("BAD_EARTH_LOC_ANT");
        assertNotNull(flag);
        assertEquals(8L, flag.getData().getElemUInt());
        assertEquals("BAD_EARTH_LOC_ANT", flag.getName());
        assertEquals("Earth location questionable because of antenna position check", flag.getDescription());
    }

    private void assertCalibrationQualityFlagCoding(String codingName, Product product) {
        final FlagCoding qualityIndicatorFlags = product.getFlagCodingGroup().get(codingName);

        MetadataAttribute flag = qualityIndicatorFlags.getFlag("NOT_CALIB");
        assertNotNull(flag);
        assertEquals(128L, flag.getData().getElemUInt());
        assertEquals("NOT_CALIB", flag.getName());
        assertEquals("This channel is not calibrated", flag.getDescription());

        flag = qualityIndicatorFlags.getFlag("BAD_SPACE_VIEW");
        assertNotNull(flag);
        assertEquals(16L, flag.getData().getElemUInt());
        assertEquals("BAD_SPACE_VIEW", flag.getName());
        assertEquals("All bad space view counts for scan line", flag.getDescription());

        flag = qualityIndicatorFlags.getFlag("MARG_SPACE_VIEW");
        assertNotNull(flag);
        assertEquals(2L, flag.getData().getElemUInt());
        assertEquals("MARG_SPACE_VIEW", flag.getName());
        assertEquals("Marginal space view counts for this line", flag.getDescription());
    }

    private void assertTiePointValue(String gridName, int x, int y, double expected, Product product) {
        final TiePointGrid latitude = product.getTiePointGrid(gridName);
        assertNotNull(latitude);
        assertEquals(expected, latitude.getPixelDouble(x, y), 1e-8);
    }

    private void assertPixelValue(String bandName, int x, int y, double expected, Product product) throws IOException {
        final double[] doubles = new double[1];
        final Band band = product.getBand(bandName);
        assertNotNull(band);
        band.readPixels(x, y, 1, 1, doubles);
        assertEquals(expected, doubles[0], 1e-8);
    }
}
