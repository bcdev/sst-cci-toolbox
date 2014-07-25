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

import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.orm.ColumnStorage;
import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.SamplingPoint;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DirtySubsceneRemoverTest {

    @Test
    public void testPrimaryIsDefault() throws Exception {
        final DirtySubsceneRemover remover = new DirtySubsceneRemover();

        assertTrue(remover.getPrimary());
    }

    @Test
    public void testSetPrimary() throws Exception {
        final DirtySubsceneRemover remover = new DirtySubsceneRemover().primary(false);

        assertFalse(remover.getPrimary());
    }

    @Test
    public void testSplitByFileId_oneFile_primary() {
        final List<SamplingPoint> points = new ArrayList<>();
        points.add(createSamplingPointWithReference(19));
        points.add(createSamplingPointWithReference(19));

        final Map<Integer, List<SamplingPoint>> listByFileId = DirtySubsceneRemover.splitByFileId(points, true);
        assertEquals(1, listByFileId.size());
        final List<SamplingPoint> fileList = listByFileId.get(19);
        assertNotNull(fileList);
        assertEquals(2, fileList.size());
    }

    @Test
    public void testSplitByFileId_oneFile_secondary() {
        final List<SamplingPoint> points = new ArrayList<>();
        points.add(createSamplingPointWithSecondaryReference(21));
        points.add(createSamplingPointWithSecondaryReference(21));
        points.add(createSamplingPointWithSecondaryReference(21));

        final Map<Integer, List<SamplingPoint>> listByFileId = DirtySubsceneRemover.splitByFileId(points, false);
        assertEquals(1, listByFileId.size());
        final List<SamplingPoint> fileList = listByFileId.get(21);
        assertNotNull(fileList);
        assertEquals(3, fileList.size());
    }

    @Test
    public void testSplitByFileId_twoFiles_primary() {
        final List<SamplingPoint> points = new ArrayList<>();
        points.add(createSamplingPointWithReference(22));
        points.add(createSamplingPointWithReference(19));
        points.add(createSamplingPointWithReference(22));
        points.add(createSamplingPointWithReference(22));
        points.add(createSamplingPointWithReference(19));

        final Map<Integer, List<SamplingPoint>> listByFileId = DirtySubsceneRemover.splitByFileId(points, true);
        assertEquals(2, listByFileId.size());
         List<SamplingPoint> fileList = listByFileId.get(19);
        assertNotNull(fileList);
        assertEquals(2, fileList.size());

        fileList = listByFileId.get(22);
        assertNotNull(fileList);
        assertEquals(3, fileList.size());
    }

    private SamplingPoint createSamplingPointWithReference(int reference) {
        final SamplingPoint samplingPoint = new SamplingPoint();
        samplingPoint.setReference(reference);
        return samplingPoint;
    }

    private SamplingPoint createSamplingPointWithSecondaryReference(int reference) {
        final SamplingPoint samplingPoint = new SamplingPoint();
        samplingPoint.setReference2(reference);
        return samplingPoint;
    }
}
