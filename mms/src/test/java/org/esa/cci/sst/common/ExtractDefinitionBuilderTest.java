package org.esa.cci.sst.common;

import org.esa.cci.sst.data.ReferenceObservation;
import org.junit.Before;
import org.junit.Test;
import org.postgis.PGgeometry;

import java.sql.SQLException;
import java.util.Date;

import static org.junit.Assert.*;

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

    @Test
    public void testBuild_empty() {
        final ExtractDefinition ed = builder.build();
        assertNotNull(ed);

        final int[] shape = ed.getShape();
        assertEquals(1, shape.length);
        assertEquals(1, shape[0]);
    }

    @Test
    public void testBuild_referenceObservation() throws SQLException {
        final Date time = new Date(100000000);
        final ReferenceObservation referenceObservation = new ReferenceObservation();
        referenceObservation.setPoint(new PGgeometry("POINT(12 45)"));
        referenceObservation.setTime(time);

        builder.referenceObservation(referenceObservation);

        final ExtractDefinition ed = builder.build();
        assertNotNull(ed);

        assertEquals(12.0, ed.getLon(), 1e-8);
        assertEquals(45.0, ed.getLat(), 1e-8);
        assertEquals(time.getTime(), ed.getDate().getTime());
    }

    @Test
    public void testBuild_referenceObservation_withoutLocation() throws SQLException {
        final Date time = new Date(100000000);
        final ReferenceObservation referenceObservation = new ReferenceObservation();
        referenceObservation.setTime(time);

        builder.referenceObservation(referenceObservation);

        final ExtractDefinition ed = builder.build();
        assertNotNull(ed);

        assertEquals(Double.NaN, ed.getLon(), 1e-8);
        assertEquals(Double.NaN, ed.getLat(), 1e-8);
        assertEquals(time.getTime(), ed.getDate().getTime());
    }

    @Test
    public void testBuild_recordNo() {
        builder.recordNo(85);

        final ExtractDefinition ed = builder.build();
        assertEquals(85, ed.getRecordNo());
    }

    @Test
    public void testBuild_shape() {
        builder.shape(new int[]{5, 7});

        final ExtractDefinition ed = builder.build();
        final int[] shape = ed.getShape();
        assertEquals(2, shape.length);
        assertEquals(1, shape[0]);
        assertEquals(7, shape[1]);
    }

    @Test
    public void testBuild_fillValue() {
         builder.fillValue(7855);

        final ExtractDefinition ed = builder.build();
        assertEquals(7855, ed.getFillValue());
    }
}
