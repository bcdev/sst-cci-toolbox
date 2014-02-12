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

import org.esa.cci.sst.util.SamplingPoint;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ClearSkyPointRemoverTest {
    private List<SamplingPoint> samples;

    @Before
    public void setUp() throws Exception {
        samples = new SobolSamplePointGenerator().createSamples(1000, 0, 0, 1000);
    }

    @Test
    public void testRemove() throws Exception {
        final ClearSkyPointRemover remover = new ClearSkyPointRemover();
        remover.removeSamples(samples);

        // roughly 50 percent of points shall remain
        assertEquals(533, samples.size());
    }

}
