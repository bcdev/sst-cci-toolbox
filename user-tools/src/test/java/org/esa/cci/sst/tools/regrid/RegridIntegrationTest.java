package org.esa.cci.sst.tools.regrid;

import org.esa.cci.sst.IoTestRunner;
import org.esa.cci.sst.TestUtil;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.Tool;
import org.esa.cci.sst.tool.ToolException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.fail;

@RunWith(IoTestRunner.class)
public class RegridIntegrationTest {

    private File testDataDirectory;
    private RegriddingTool regriddingTool;

    @Before
    public void setUp() throws IOException {
        testDataDirectory = TestUtil.getTestDataDirectory();
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
    public void test_L3U_one_day_10_deg_noTotalUncert() {

        System.out.print(new File("").getAbsolutePath());
        final Configuration configuration = new Configuration();
        configuration.setToolHome(new File("").getAbsolutePath());
        configuration.put("productType", "CCI_L3U");
        configuration.put("totalUncertainty", "false");
        configuration.put("climatologyDir", "/auxdata");

        regriddingTool.run(configuration, new String[0]);
    }
}
