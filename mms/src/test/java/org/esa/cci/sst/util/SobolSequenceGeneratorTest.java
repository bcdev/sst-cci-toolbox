package org.esa.cci.sst.util;

import org.esa.cci.sst.util.SobolSequenceGenerator;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class SobolSequenceGeneratorTest {

    @Test
    public void testSkip() {
        SobolSequenceGenerator generator = new SobolSequenceGenerator(1);

        generator.nextVector();
        final double[] secondOutput = generator.nextVector();

        generator = new SobolSequenceGenerator(1);
        generator.skip(1);

        final double[] skippedOutput = generator.nextVector();
        assertArrayEquals(secondOutput, skippedOutput, 0.0);
    }

    @Test
    public void testSkipZero() {
        SobolSequenceGenerator generator = new SobolSequenceGenerator(1);

        final double[] firstOutput = generator.nextVector();

        generator = new SobolSequenceGenerator(1);
        generator.skip(0);

        final double[] skippedOutput = generator.nextVector();
        assertArrayEquals(firstOutput, skippedOutput, 0.0);
    }
}
