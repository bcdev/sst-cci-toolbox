package org.esa.cci.sst.tools.regavg;

import org.esa.cci.sst.common.ProcessingLevel;
import org.esa.cci.sst.product.ProductType;
import org.esa.cci.sst.tool.Parameter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Norman Fomferra
 */
public class AveragingToolTest {

    private AveragingTool tool;

    @Before
    public void setUp() {
        tool = new AveragingTool();
    }

    @Test
    public void testOutputName() throws Exception {
        tool.setProductType(ProductType.CCI_L3U);

        final String filename = tool.getOutputFilename("20000101", "20101231", "Global", ProcessingLevel.L3U, "SSTskin", "PS", "DM");
        assertEquals("20000101-20101231-Global_average-ESACCI-L3U_GHRSST-SSTskin-PS-DM-v3.0-fv1.1.nc", filename);
    }

    @Test
    public void testValidTemporalResolutions() throws Exception {
        assertEquals("[daily, monthly, seasonal, annual]", AveragingTool.validTemporalResolutions());
    }

    @Test
    public void testGetName() {
        assertEquals("org/esa/cci/sst/tools/regavg", tool.getName());
    }

    @Test
    public void testGetVersion() {
        assertEquals("3.0", tool.getVersion());
    }

    @Test
    public void testGetSyntax() {
        assertEquals("org/esa/cci/sst/tools/regavg [OPTIONS]", tool.getSyntax());
    }

    @Test
    public void testGetHeader() {
        assertEquals("\nThe regavg tool is used to generate regional average time-series from ARC (L2P, L3U) and SST_cci (L3U, L3P, L4) product files given a time interval and a list of regions. An output NetCDF file will be written for each region.\n" +
                "OPTIONS may be one or more of the following:\n", tool.getHeader());
    }

    @Test
    public void testGetToolHome() {
        assertEquals(".", tool.getToolHome());
    }

    @Test
    public void testGetParameter() {
        final Parameter[] parameters = tool.getParameters();
        assertEquals(17, parameters.length);

        final Parameter lut_1_parameter = getByName("lut1File", parameters);
        assertEquals("FILE", lut_1_parameter.getArgName());
        assertEquals("./auxdata/coverage_uncertainty_parameters.nc", lut_1_parameter.getDefaultValue());
        assertEquals("A NetCDF file that provides lookup table 1.", lut_1_parameter.getDescription());

        final Parameter lut_2_parameter = getByName("lut2File", parameters);
        assertEquals("FILE", lut_2_parameter.getArgName());
        assertEquals("./auxdata/RegionalAverage_LUT2.txt", lut_2_parameter.getDefaultValue());
        assertEquals("A plain text file that provides lookup table 2.", lut_2_parameter.getDescription());
    }

    private Parameter getByName(String name, Parameter[] parameters) {
        for (final Parameter parameter : parameters) {
            if (parameter.getName().equals(name)) {
                return parameter;
            }
        }
        fail("Parameter not found with name: " + name);
        return null;
    }
}
