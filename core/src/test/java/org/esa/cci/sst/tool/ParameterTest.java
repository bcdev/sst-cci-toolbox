package org.esa.cci.sst.tool;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ParameterTest {

    @Test
    public void testConstructor() {
        final Parameter parameter = new Parameter("name", "argName", "default", "description");

        assertEquals("name", parameter.getName());
        assertEquals("argName", parameter.getArgName());
        assertEquals("default", parameter.getDefaultValue());
        assertEquals("description", parameter.getDescription());
    }
}
