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

package org.esa.cci.sst.product;

import org.esa.cci.sst.IoTestRunner;
import org.esa.cci.sst.TestL3ProductMaker;
import org.esa.cci.sst.aggregate.AggregationContext;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.file.FileType;
import org.esa.cci.sst.grid.Grid;
import org.junit.Test;
import org.junit.runner.RunWith;
import ucar.nc2.NetcdfFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(IoTestRunner.class)
public class CciL3FileIntegrationTest {

    private static final FileType FILE_TYPE = CciL3FileType.INSTANCE;


    @Test
    public void testReadGrids() throws Exception {
        NetcdfFile l3UFile = TestL3ProductMaker.readL3GridsSetup();
        //execution
        final AggregationContext context = FILE_TYPE.readSourceGrids(l3UFile, SstDepth.skin, new AggregationContext());

        //sea_surface_temperature
        final Grid sstGrid = context.getSstGrid();
        assertEquals(2000, sstGrid.getSampleInt(0, 0));
        assertEquals(293.14999344944954, sstGrid.getSampleDouble(0, 0), 1e-8);
        assertEquals(1000, sstGrid.getSampleInt(1, 0));
        assertEquals(283.14999367296696, sstGrid.getSampleDouble(1, 0), 1e-8);

        //quality_level
        final Grid qualityGrid = context.getQualityGrid();
        assertEquals(-127, qualityGrid.getSampleInt(0, 0));
        assertEquals(-127.0, qualityGrid.getSampleDouble(0, 0), 1e-8);
        assertEquals(-127, qualityGrid.getSampleInt(1, 0));
        assertEquals(-127.0, qualityGrid.getSampleDouble(1, 0), 1e-8);

        //uncorrelated_uncertainty
        final Grid randomUncertaintyGrid = context.getRandomUncertaintyGrid();
        assertEquals(-32768, randomUncertaintyGrid.getSampleInt(0, 0));
        assertTrue(Double.isNaN(randomUncertaintyGrid.getSampleDouble(0, 0)));
        assertEquals(-32768, randomUncertaintyGrid.getSampleInt(1, 0));
        assertTrue(Double.isNaN(randomUncertaintyGrid.getSampleDouble(1, 0)));

        //large_scale_correlated_uncertainty
        final Grid largeScaleUncertaintyGrid = context.getLargeScaleUncertaintyGrid();
        assertEquals(-32768, largeScaleUncertaintyGrid.getSampleInt(0, 0));
        assertTrue(Double.isNaN(largeScaleUncertaintyGrid.getSampleDouble(0, 0)));
        assertEquals(-32768, largeScaleUncertaintyGrid.getSampleInt(1, 0));
        assertTrue(Double.isNaN(largeScaleUncertaintyGrid.getSampleDouble(1, 0)));

        //synoptically_correlated_uncertainty
        final Grid synopticUncertaintyGrid = context.getSynopticUncertaintyGrid();
        assertEquals(-32768, synopticUncertaintyGrid.getSampleInt(0, 0));
        assertTrue(Double.isNaN(synopticUncertaintyGrid.getSampleDouble(0, 0)));
        assertEquals(-32768, synopticUncertaintyGrid.getSampleInt(1, 0));
        assertTrue(Double.isNaN(synopticUncertaintyGrid.getSampleDouble(1, 0)));

        //adjustment_uncertainty
        final Grid adjustmentUncertaintyGrid = context.getAdjustmentUncertaintyGrid();
        assertEquals(-32768, adjustmentUncertaintyGrid.getSampleInt(0, 0));
        assertTrue(Double.isNaN(adjustmentUncertaintyGrid.getSampleDouble(0, 0)));
        assertEquals(-32768, adjustmentUncertaintyGrid.getSampleInt(1, 0));
        assertTrue(Double.isNaN(adjustmentUncertaintyGrid.getSampleDouble(1, 0)));
    }
}
