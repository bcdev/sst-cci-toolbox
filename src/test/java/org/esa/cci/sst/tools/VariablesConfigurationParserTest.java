package org.esa.cci.sst.tools;

import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.rules.RuleException;
import org.esa.cci.sst.util.IoUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;

import static org.junit.Assert.*;

public class VariablesConfigurationParserTest {

    @Before
    public void initRegistry() throws IOException {
        final VariableDescriptorRegistry registry = VariableDescriptorRegistry.getInstance();
        registerSourceDescriptors(registry, "seviri.nc", "seviri");
        registerSourceDescriptors(registry, "metop.nc", "metop");
        registerSourceDescriptors(registry, "aatsr_md.nc", "aatsr_md");
        registerSourceDescriptors(registry, "amsre.nc", "amsre");
        registerSourceDescriptors(registry, "tmi.nc", "tmi");
        registerSourceDescriptors(registry, "atsr.1.nc", "atsr1");
        registerSourceDescriptors(registry, "atsr.2.nc", "atsr2");
        registerSourceDescriptors(registry, "atsr.3.nc", "atsr3");
    }

    @Test
    public void testParsing() throws ParseException, RuleException {
        final InputStream is = getClass().getResourceAsStream("mmd-variables.txt");
        final VariableDescriptorRegistry registry = VariableDescriptorRegistry.getInstance();
        final List<String> nameList = registry.registerDescriptors(is);

        assertNotNull(nameList);
        assertEquals(81, nameList.size());

        assertEquals("metop.brightness_temperature.037", nameList.get(5));

        final VariableDescriptor targetDescriptor = registry.getDescriptor("metop.brightness_temperature.037");

        assertEquals("matchup metop.ni metop.nj", targetDescriptor.getDimensions());
        assertNotNull(registry.getRule(targetDescriptor));
        assertNotNull(registry.getSourceDescriptor(targetDescriptor));
    }

    private void registerSourceDescriptors(VariableDescriptorRegistry registry, String fileName, String sensor) throws IOException {
        NetcdfFile netcdfFile = null;
        try {
            final String sensorFile = getClass().getResource(fileName).getFile();
            netcdfFile = NetcdfFile.open(sensorFile);
            for (final Variable variable : netcdfFile.getVariables()) {
                final VariableDescriptor variableDescriptor = IoUtil.createVariableDescriptor(variable, sensor);
                registry.register(variableDescriptor);
            }
        } finally {
            if (netcdfFile != null) {
                netcdfFile.close();
            }
        }
    }

    private VariableDescriptor createDescriptor(final String name, final String dims) {
        VariableDescriptor descriptor = new VariableDescriptor(name);
        descriptor.setDimensions(dims);
        return descriptor;
    }

    private VariableDescriptor createDescriptor(final String name, final String dims, final DataType type) {
        final VariableDescriptor descriptor = createDescriptor(name, dims);
        descriptor.setType(type.name());
        return descriptor;
    }

    @After
    public void clearRegistry() {
        VariableDescriptorRegistry.getInstance().clear();
    }

}
