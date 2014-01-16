package org.esa.cci.sst.common;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ExtractDefinitionBuilderTest {

    private ExtractDefinitionBuilder builder;

    @Before
    public void setUp() {
        builder = new ExtractDefinitionBuilder();
    }

    @Test
    public void testBuild_lat() {
        final ExtractDefinitionBuilder returnedBuilder = builder.lat(0.5);
        assertSame(builder, returnedBuilder);

        final ExtractDefinition ed = builder.build();
        assertEquals(0.5, ed.getLat(), 0.0);

    }

    @Test
    public void testBuild_lon() {
        final ExtractDefinitionBuilder returnedBuilder = builder.lon(9.45);
        assertSame(builder, returnedBuilder);

        final ExtractDefinition ed = builder.build();
        assertEquals(9.45, ed.getLon(), 0.0);

    }
}
