package org.esa.cci.sst.tools;

import org.esa.cci.sst.DescriptorRegistry;
import org.esa.cci.sst.data.Descriptor;
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
    public void testRegisterDescriptors() throws ParseException, RuleException {
        final DescriptorRegistry registry = DescriptorRegistry.getInstance();
        final InputStream is = getClass().getResourceAsStream("mmd-variables.txt");

        assertNotNull(is);

        final List<String> nameList = registry.registerDescriptors(is);

        assertNotNull(nameList);
        assertEquals(81, nameList.size());

        assertEquals("aatsr_md.atsr.L2_confidence_word", nameList.get(0));
        assertEquals("tmi.wind_speed", nameList.get(nameList.size() - 1));

        testMetopDescriptor();
    }

    private void testMetopDescriptor() {
        final DescriptorRegistry registry = DescriptorRegistry.getInstance();
        final Descriptor targetDescriptor = registry.getDescriptor("metop.brightness_temperature.037");

        assertEquals("matchup metop.ni metop.nj", targetDescriptor.getDimensions());
        assertNotNull(registry.getConverter(targetDescriptor));
        assertNotNull("metop.IR037", registry.getSourceDescriptor(targetDescriptor).getName());
    }

    @BeforeClass
    public static void initRegistry() throws IOException, URISyntaxException {
        registerSourceDescriptors("seviri.nc", "seviri");
        registerSourceDescriptors("metop.nc", "metop");
        registerSourceDescriptors("aatsr_md.nc", "aatsr_md");
        registerSourceDescriptors("amsre.nc", "amsre");
        registerSourceDescriptors("tmi.nc", "tmi");
        registerSourceDescriptors("atsr.1.nc", "atsr1");
        registerSourceDescriptors("atsr.2.nc", "atsr2");
        registerSourceDescriptors("atsr.3.nc", "atsr3");
    }

    @AfterClass
    public static void clearRegistry() {
        DescriptorRegistry.getInstance().clear();
    }

    private static void registerSourceDescriptors(String fileName, String sensor) throws IOException,
                                                                                         URISyntaxException {
        NetcdfFile netcdfFile = null;
        try {
            final File sensorFile = new File(TargetVariableConfigurationTest.class.getResource(fileName).toURI());
            netcdfFile = NetcdfFile.open(sensorFile.getPath());
            final DescriptorRegistry registry = DescriptorRegistry.getInstance();
            for (final Variable variable : netcdfFile.getVariables()) {
                final Descriptor variableDescriptor = IoUtil.createDescriptorBuilder(variable, sensor).build();
                registry.register(variableDescriptor);
            }
        } finally {
            if (netcdfFile != null) {
                netcdfFile.close();
            }
        }
    }

}
