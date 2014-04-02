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

public class CloudySubsceneRemoverTest {

    @Test
    public void testPrimaryIsDefault() throws Exception {
        final CloudySubsceneRemover remover = new CloudySubsceneRemover();

        assertTrue(remover.getPrimary());
    }

    @Test
    public void testSetPrimary() throws Exception {
        final CloudySubsceneRemover remover = new CloudySubsceneRemover().primary(false);

        assertFalse(remover.getPrimary());
    }

    @Test
    public void testGetColumnFillValue() {
        final double expectedFill = 675.88;
        final Column column = (Column) new ColumnBuilder().fillValue(expectedFill).build();
        final ColumnStorage columnStorage = mock(ColumnStorage.class);

        when(columnStorage.getColumn("orb_atsr.2.cloud_band")).thenReturn(column);

        final Number fillValue = CloudySubsceneRemover.getColumnFillValue("atsr.2", "cloud_band", columnStorage);
        assertEquals(expectedFill, fillValue.doubleValue(), 1e-8);

        verify(columnStorage, times(1)).getColumn("orb_atsr.2.cloud_band");
        verifyNoMoreInteractions(columnStorage);
    }

    @Test
    public void testGetColumnFillValue_invalidSensorName() {
        final ColumnStorage columnStorage = mock(ColumnStorage.class);

        try {
            CloudySubsceneRemover.getColumnFillValue("invalid", "cloud_band", columnStorage);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        verifyNoMoreInteractions(columnStorage);
    }

    @Test
    public void testGetColumnFillValue_columnNotStored() {
        final ColumnStorage columnStorage = mock(ColumnStorage.class);

        when(columnStorage.getColumn("orb_atsr.2.cloud_band")).thenReturn(null);

        try {
            CloudySubsceneRemover.getColumnFillValue("atsr.2", "cloud_band", columnStorage);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }

        verify(columnStorage, times(1)).getColumn("orb_atsr.2.cloud_band");
        verifyNoMoreInteractions(columnStorage);
    }

    @Test
    public void testSplitByFileId_oneFile_primary() {
        final ArrayList<SamplingPoint> points = new ArrayList<>();
        points.add(createSamplingPointWithReference(19));
        points.add(createSamplingPointWithReference(19));

        final Map<Integer, List<SamplingPoint>> listByFileId = CloudySubsceneRemover.splitByFileId(points, true);
        assertEquals(1, listByFileId.size());
        final List<SamplingPoint> fileList = listByFileId.get(19);
        assertNotNull(fileList);
        assertEquals(2, fileList.size());
    }

    @Test
    public void testSplitByFileId_oneFile_secondary() {
        final ArrayList<SamplingPoint> points = new ArrayList<>();
        points.add(createSamplingPointWithSecondaryReference(21));
        points.add(createSamplingPointWithSecondaryReference(21));
        points.add(createSamplingPointWithSecondaryReference(21));

        final Map<Integer, List<SamplingPoint>> listByFileId = CloudySubsceneRemover.splitByFileId(points, false);
        assertEquals(1, listByFileId.size());
        final List<SamplingPoint> fileList = listByFileId.get(21);
        assertNotNull(fileList);
        assertEquals(3, fileList.size());
    }

    @Test
    public void testSplitByFileId_twoFiles_primary() {
        final ArrayList<SamplingPoint> points = new ArrayList<>();
        points.add(createSamplingPointWithReference(22));
        points.add(createSamplingPointWithReference(19));
        points.add(createSamplingPointWithReference(22));
        points.add(createSamplingPointWithReference(22));
        points.add(createSamplingPointWithReference(19));

        final Map<Integer, List<SamplingPoint>> listByFileId = CloudySubsceneRemover.splitByFileId(points, true);
        assertEquals(2, listByFileId.size());
         List<SamplingPoint> fileList = listByFileId.get(19);
        assertNotNull(fileList);
        assertEquals(2, fileList.size());

        fileList = listByFileId.get(22);
        assertNotNull(fileList);
        assertEquals(3, fileList.size());
    }

    @Test
    public void testIsInsituCase() {
        final Configuration configuration = new Configuration();
        assertFalse(CloudySubsceneRemover.isInsituCase(configuration));

        configuration.put(Configuration.KEY_MMS_SAMPLING_GENERATOR, "sobol");
        assertFalse(CloudySubsceneRemover.isInsituCase(configuration));

        configuration.put(Configuration.KEY_MMS_SAMPLING_GENERATOR, "insitu");
        assertTrue(CloudySubsceneRemover.isInsituCase(configuration));
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
