package org.esa.cci.sst.tools;

import org.esa.cci.sst.data.VariableDescriptor;
import org.esa.cci.sst.rules.RuleException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VariablesConfigurationParserTest {

    @Test
    public void testParsing() throws ParseException, RuleException {
        final InputStream is = getClass().getResourceAsStream("mmd-variables.txt");
        final List<String> nameList = VariableDescriptorRegistry.getInstance().loadDescriptors(is,
                                                                                               new ArrayList<String>());

        assertNotNull(nameList);
        assertEquals(1, nameList.size());

        assertEquals("metop.brightness_temperature.037", nameList.get(0));
    }

    @Before
    public void initRegistry() {
        final VariableDescriptorRegistry registry = VariableDescriptorRegistry.getInstance();

        VariableDescriptor descriptor;
        descriptor = new VariableDescriptor("metop.IR037");
        descriptor.setDimensions("n ny nx");
        registry.register(descriptor);
    }

    @After
    public void clearRegistry() {
        VariableDescriptorRegistry.getInstance().clear();
    }

}
