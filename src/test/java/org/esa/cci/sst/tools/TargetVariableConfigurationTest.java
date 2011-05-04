package org.esa.cci.sst.tools;

import org.esa.cci.sst.ColumnRegistry;
import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.rules.RuleException;
import org.esa.cci.sst.util.IoUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TargetVariableConfigurationTest {

    @Test
    public void testRegisterColumns() throws ParseException, RuleException {
        final ColumnRegistry registry = ColumnRegistry.getInstance();
        final InputStream is = getClass().getResourceAsStream("mmd-variables.txt");

        assertNotNull(is);

        final List<String> nameList = registry.registerColumns(is);

        assertNotNull(nameList);
        assertEquals(81, nameList.size());

        assertEquals("aatsr_md.atsr.L2_confidence_word", nameList.get(0));
        assertEquals("tmi.wind_speed", nameList.get(nameList.size() - 1));

        testMetopColumn();
    }

    private void testMetopColumn() {
        final ColumnRegistry registry = ColumnRegistry.getInstance();
        final Column targetColumn = registry.getColumn("metop.brightness_temperature.037");

        assertEquals("matchup metop.ni metop.nj", targetColumn.getDimensions());
        assertNotNull(registry.getConverter(targetColumn));
        assertNotNull("metop.IR037", registry.getSourceColumn(targetColumn).getName());
    }

    @BeforeClass
    public static void initRegistry() throws IOException, URISyntaxException {
        registerSourceColumns("seviri.nc", "seviri");
        registerSourceColumns("metop.nc", "metop");
        registerSourceColumns("aatsr_md.nc", "aatsr_md");
        registerSourceColumns("ams.nc", "amsre");
        registerSourceColumns("tmi.nc", "tmi");
        registerSourceColumns("atsr.1.nc", "atsr1");
        registerSourceColumns("atsr.2.nc", "atsr2");
        registerSourceColumns("atsr.3.nc", "atsr3");
    }

    @AfterClass
    public static void clearRegistry() {
        ColumnRegistry.getInstance().clear();
    }

    private static void registerSourceColumns(String fileName, String sensor) throws IOException,
                                                                                     URISyntaxException {
        NetcdfFile netcdfFile = null;
        try {
            final File sensorFile = new File(TargetVariableConfigurationTest.class.getResource(fileName).toURI());
            netcdfFile = NetcdfFile.open(sensorFile.getPath());
            final ColumnRegistry registry = ColumnRegistry.getInstance();
            for (final Variable variable : netcdfFile.getVariables()) {
                final Column column = IoUtil.createColumnBuilder(variable, sensor).build();
                registry.register(column);
            }
        } finally {
            if (netcdfFile != null) {
                netcdfFile.close();
            }
        }
    }

}
