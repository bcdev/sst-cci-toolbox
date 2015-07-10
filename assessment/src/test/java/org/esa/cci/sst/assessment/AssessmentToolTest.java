package org.esa.cci.sst.assessment;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AssessmentToolTest {

    @Test
    public void testIsFigureProperty() {
        assertFalse(AssessmentTool.isFigureProperty("image"));
        assertFalse(AssessmentTool.isFigureProperty("figure.bla.scale"));

        assertTrue(AssessmentTool.isFigureProperty("figure.really"));
    }

    @Test
    public void testCreateScaleName() {
        assertEquals("holla.scale", AssessmentTool.createScaleName("holla"));
        assertEquals("figure.thing.scale", AssessmentTool.createScaleName("figure.thing"));
    }

    @Test
    public void testMakeWordVariable() {
         assertEquals("${yeah}", AssessmentTool.makeWordVariable("yeah"));
         assertEquals("${Oh.yeah}", AssessmentTool.makeWordVariable("Oh.yeah"));
    }
}
