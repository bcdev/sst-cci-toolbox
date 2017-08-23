package org.esa.cci.sst.tools;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CircularExtractMaskTest {

    @Test
    public void testConstructAndGetValue() {
        final CircularExtractMask mask = new CircularExtractMask(13, 13, 4.5, 1.1);

        assertFalse(mask.getValue(0, 4));
        assertFalse(mask.getValue(1, 4));
        assertFalse(mask.getValue(2, 4));
        assertTrue(mask.getValue(3, 4));
        assertTrue(mask.getValue(4, 4));
        assertTrue(mask.getValue(5, 4));
        assertTrue(mask.getValue(6, 4));
        assertTrue(mask.getValue(7, 4));
        assertTrue(mask.getValue(8, 4));
        assertTrue(mask.getValue(9, 4));
        assertFalse(mask.getValue(10, 4));
        assertFalse(mask.getValue(11, 4));
        assertFalse(mask.getValue(12, 4));

        assertFalse(mask.getValue(5, 0));
        assertFalse(mask.getValue(5, 1));
        assertFalse(mask.getValue(5, 2));
        assertTrue(mask.getValue(5, 3));
        assertTrue(mask.getValue(5, 4));
        assertTrue(mask.getValue(5, 5));
        assertTrue(mask.getValue(5, 6));
        assertTrue(mask.getValue(5, 7));
        assertTrue(mask.getValue(5, 8));
        assertTrue(mask.getValue(5, 9));
        assertFalse(mask.getValue(5, 10));
        assertFalse(mask.getValue(5, 11));
        assertFalse(mask.getValue(5, 12));
    }

    @Test
    public void testConstructAndGetValue_varyX() {
        final CircularExtractMask mask = new CircularExtractMask(7, 9, 3.2, 1.1);

        assertFalse(mask.getValue(0, 5));
        assertTrue(mask.getValue(1, 5));
        assertTrue(mask.getValue(2, 5));

        assertFalse(mask.getValue(6, 2));
        assertFalse(mask.getValue(6, 3));
        assertFalse(mask.getValue(6, 4));
    }

    @Test
    public void testConstructAndGetValue_varyY() {
        final CircularExtractMask mask = new CircularExtractMask(9, 7, 3.2, 1.1);

        assertFalse(mask.getValue(0, 4));
        assertFalse(mask.getValue(1, 4));
        assertTrue(mask.getValue(2, 4));

        assertTrue(mask.getValue(6, 4));
        assertTrue(mask.getValue(6, 5));
        assertFalse(mask.getValue(6, 6));
    }

    @Test
    public void testConstructAndGetValue_varyRadius() {
        final CircularExtractMask mask = new CircularExtractMask(13, 13, 5.5, 1.1);

        assertFalse(mask.getValue(0, 4));
        assertFalse(mask.getValue(1, 4));
        assertTrue(mask.getValue(2, 4));
        assertTrue(mask.getValue(3, 4));

        assertTrue(mask.getValue(9, 4));
        assertTrue(mask.getValue(10, 4));
        assertFalse(mask.getValue(11, 4));
        assertFalse(mask.getValue(12, 4));

        assertFalse(mask.getValue(5, 0));
        assertFalse(mask.getValue(5, 1));
        assertTrue(mask.getValue(5, 2));
        assertTrue(mask.getValue(5, 3));

        assertTrue(mask.getValue(5, 9));
        assertTrue(mask.getValue(5, 10));
        assertFalse(mask.getValue(5, 11));
        assertFalse(mask.getValue(5, 12));
    }

    @Test
    public void testConstructAndGetValue_varyPixelSize() {
        final CircularExtractMask mask = new CircularExtractMask(13, 13, 4.5, 1.8);

        assertFalse(mask.getValue(0, 4));
        assertFalse(mask.getValue(1, 4));
        assertFalse(mask.getValue(2, 4));
        assertFalse(mask.getValue(3, 4));
        assertFalse(mask.getValue(4, 4));
        assertTrue(mask.getValue(5, 4));
        assertTrue(mask.getValue(6, 4));
        assertTrue(mask.getValue(7, 4));
        assertFalse(mask.getValue(8, 4));
        assertFalse(mask.getValue(9, 4));
        assertFalse(mask.getValue(10, 4));
        assertFalse(mask.getValue(11, 4));
        assertFalse(mask.getValue(12, 4));

        assertFalse(mask.getValue(5, 0));
        assertFalse(mask.getValue(5, 1));
        assertFalse(mask.getValue(5, 2));
        assertFalse(mask.getValue(5, 3));
        assertTrue(mask.getValue(5, 4));
        assertTrue(mask.getValue(5, 5));
        assertTrue(mask.getValue(5, 6));
        assertTrue(mask.getValue(5, 7));
        assertTrue(mask.getValue(5, 8));
        assertFalse(mask.getValue(5, 9));
        assertFalse(mask.getValue(5, 10));
        assertFalse(mask.getValue(5, 11));
        assertFalse(mask.getValue(5, 12));
    }
}
