/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.accumulate;

import org.junit.Before;
import org.junit.Test;

import static java.lang.Double.NaN;
import static org.junit.Assert.assertEquals;

public class ArithmeticMeanAccumulatorTest {

    private ArithmeticMeanAccumulator accumulator;

    @Before
    public void setUp() {
        accumulator = new ArithmeticMeanAccumulator();
    }

    @Test
    public void testAccumulateAndCombine_noSamples() {
        assertEquals(0, accumulator.getSampleCount());
        assertEquals(Double.NaN, accumulator.combine(), 1e-8);
    }

    @Test
    public void testAccumulateAndCombine_sumX_zero() {
        accumulator.accumulateSample(0.0, 1.0);

        assertEquals(1, accumulator.getSampleCount());
        assertEquals(0.0, accumulator.combine(), 1e-8);
    }

    @Test
    public void testAccumulateAndCombine_sumWeights_zero() {
        accumulator.accumulateSample(1.0, 1.0);
        accumulator.accumulateSample(2.0, -1.0);

        assertEquals(2, accumulator.getSampleCount());
        assertEquals(Double.NaN, accumulator.combine(), 1e-8);
    }

    @Test
    public void testAccumulateAndCombine() {
        accumulator.accumulateSample(1.0, 0.0);
        accumulator.accumulateSample(2.0, 0.5);

        assertEquals(2, accumulator.getSampleCount());
        assertEquals(2.0, accumulator.combine(), 1e-8);
    }

    @Test
    public void testAccumulateAndCombine_NaNRejected() throws Exception {
        accumulator.accumulate(1.0, 0.7);
        accumulator.accumulate(2.0, 0.5);

        accumulator.accumulate(NaN, 0.5);
        accumulator.accumulate(2.0, NaN);

        assertEquals(2, accumulator.getSampleCount());
        assertEquals(1.4166666666666667, accumulator.combine(), 1e-8);
    }

    @Test
    public void testAccumulateAndCombine_withoutWeights() {
        accumulator.accumulate(2.0);
        accumulator.accumulate(4.0);

        assertEquals(2, accumulator.getSampleCount());
        assertEquals(3.0, accumulator.combine(), 1e-8);
    }
}
