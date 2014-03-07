package org.esa.cci.sst.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class SobolSequenceGeneratorTest {

    @Test
    public void testSkipOneResultsInSameVectorThanCallingNextVectorTwice() {
        SobolSequenceGenerator generator = new SobolSequenceGenerator(1);

        generator.nextVector();
        final double[] secondOutput = generator.nextVector();

        generator = new SobolSequenceGenerator(1);
        generator.skip(1);

        final double[] skippedOutput = generator.nextVector();
        assertArrayEquals(secondOutput, skippedOutput, 0.0);
    }

    @Test
    public void testSkipZeroResultsInSameVector() {
        SobolSequenceGenerator generator = new SobolSequenceGenerator(1);

        final double[] firstOutput = generator.nextVector();

        generator = new SobolSequenceGenerator(1);
        generator.skip(0);

        final double[] skippedOutput = generator.nextVector();
        assertArrayEquals(firstOutput, skippedOutput, 0.0);
    }

    @Test
    public void testConstructionFailsOnIllegalDimension() {
        try {
            new SobolSequenceGenerator(0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Illegal input dimension: 0", expected.getMessage());
        }

        try {
            new SobolSequenceGenerator(1001);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Illegal input dimension: 1001", expected.getMessage());
        }
    }
}
