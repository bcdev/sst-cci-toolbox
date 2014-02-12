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

import java.util.ArrayList;
import java.util.List;

public class ClearSkyPointRemover {

    public void removeSamples(List<SamplingPoint> samples) {
        final ArrayList<SamplingPoint> remainingSamples = new ArrayList<>(samples.size());
        final ClearSkyPriors clearSkyPriors = Container.CLEAR_SKY_PRIORS;
        for (final SamplingPoint point : samples) {
            final double f = 0.05 / clearSkyPriors.getSample(point.getLon(), point.getLat());
            if (point.getRandom() <= f) {
                remainingSamples.add(point);
            }
        }

        samples.clear();
        samples.addAll(remainingSamples);
    }

    private static class Container {

        private static final ClearSkyPriors CLEAR_SKY_PRIORS = new ClearSkyPriors();
    }
}
