package org.esa.cci.sst.util;

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
}
