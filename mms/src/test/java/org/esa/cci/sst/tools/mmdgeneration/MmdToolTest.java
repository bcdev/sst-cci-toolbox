package org.esa.cci.sst.tools.mmdgeneration;


import org.esa.cci.sst.tools.Configuration;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MmdToolTest {

    @Test
    public void testCreateNetCDFWriter() throws IOException {
        final Configuration configuration = new Configuration();
        configuration.put(Configuration.KEY_MMS_TARGET_DIR, "/here/we");
        configuration.put(Configuration.KEY_MMS_TARGET_FILENAME, "are_now.nc");

        final NetcdfFileWriter netCDFWriter = MmdTool.createNetCDFWriter(configuration);
        assertNotNull(netCDFWriter);
        assertEquals(NetcdfFileWriter.Version.netcdf4, netCDFWriter.getVersion());
        final NetcdfFile netcdfFile = netCDFWriter.getNetcdfFile();
        // this assert is platform-specific
        assertEquals("/here/we/are_now.nc", netcdfFile.getLocation());
    }

    @Test
    public void testCreateNetCDFWriter_withConfigurationDefaultValues() throws IOException {
        final Configuration configuration = new Configuration();

        final NetcdfFileWriter netCDFWriter = MmdTool.createNetCDFWriter(configuration);
        assertNotNull(netCDFWriter);
        assertEquals(NetcdfFileWriter.Version.netcdf4, netCDFWriter.getVersion());
        final NetcdfFile netcdfFile = netCDFWriter.getNetcdfFile();
        // this assert is platform-specific
        assertEquals("./mmd.nc", netcdfFile.getLocation());
    }
}
