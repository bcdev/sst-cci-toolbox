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
import org.esa.cci.sst.tools.ToolException;
import org.junit.Test;

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
}
