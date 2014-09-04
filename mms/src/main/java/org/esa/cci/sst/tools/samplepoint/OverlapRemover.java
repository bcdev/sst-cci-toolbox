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

import org.esa.cci.sst.tools.overlap.RegionOverlapFilter;
import org.esa.cci.sst.util.SamplingPoint;

import java.util.List;

public class OverlapRemover {

    private final int subSceneWidth;
    private final int subSceneHeight;

    public OverlapRemover(int subSceneWidth, int subSceneHeight) {
        this.subSceneWidth = subSceneWidth;
        this.subSceneHeight = subSceneHeight;
    }

    public void removeSamples(List<SamplingPoint> samples) {
        final RegionOverlapFilter regionOverlapFilter = new RegionOverlapFilter(subSceneWidth, subSceneHeight);
        final List<SamplingPoint> remainingSamples = regionOverlapFilter.filterOverlaps(samples);
        samples.clear();
        samples.addAll(remainingSamples);
    }
}
