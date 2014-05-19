package org.esa.beam.util;

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.Variable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VariableSampleSourceTest {

    public static final int HEIGHT = 34;
    public static final int WIDTH = 61;
    private VariableSampleSource sampleSource;

    @Before
    public void setUp() {
        final Variable variable = mock(Variable.class);
        when(variable.getRank()).thenReturn(2);
        when(variable.getShape(0)).thenReturn(HEIGHT);
        when(variable.getShape(1)).thenReturn(WIDTH);

        final Array array = mock(Array.class);
        sampleSource = new VariableSampleSource(variable, array);
    }

    @Test
    public void testGetWidth() {
        final int width = sampleSource.getWidth();
        assertEquals(WIDTH, width);
    }

    @Test
    public void testGetHeight() {
        final int height = sampleSource.getHeight();
        assertEquals(HEIGHT, height);
    }
}
