package org.esa.cci.sst.tools.samplepoint;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.cci.sst.IoTestRunner;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SamplingPointPlotter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(IoTestRunner.class)
public class LandPointRemoverTest {

    private List<SamplingPoint> samples;

    @Before
    public void setUp() throws Exception {
        samples = new SobolSamplePointGenerator().createSamples(1000, 0, 0, 1000);
    }

    @Test
    public void testRemove() throws Exception {
        final LandPointRemover remover = new LandPointRemover();
        remover.removeSamples(samples);

        // one thirds of the Earth are covered by land, so two thirds of the samples shall remain
        assertEquals(657, samples.size());
    }

    public static void main(String[] args) throws Exception {
        final List<SamplingPoint> samples = new SobolSamplePointGenerator().createSamples(10000, 0, 0, 1000);
        final LandPointRemover remover = new LandPointRemover();
        remover.removeSamples(samples);

        new SamplingPointPlotter()
                .samples(samples)
                .windowTitle(LandPointRemoverTest.class.getCanonicalName())
                .plot();
    }

}
