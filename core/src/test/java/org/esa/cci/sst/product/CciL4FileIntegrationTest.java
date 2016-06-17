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
import org.esa.cci.sst.ScalarGrid;
import org.esa.cci.sst.TestL3ProductMaker;
import org.esa.cci.sst.aggregate.Aggregation;
import org.esa.cci.sst.aggregate.AggregationCell;
import org.esa.cci.sst.aggregate.AggregationContext;
import org.esa.cci.sst.aggregate.SpatialAggregationCell;
import org.esa.cci.sst.cell.CellAggregationCell;
import org.esa.cci.sst.cell.CellFactory;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.file.FileType;
import org.esa.cci.sst.grid.Grid;
import org.esa.cci.sst.grid.GridDef;
import org.junit.Test;
import org.junit.runner.RunWith;
import ucar.nc2.NetcdfFile;

import java.awt.*;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(IoTestRunner.class)
public class CciL4FileIntegrationTest {
    private static final FileType FILE_TYPE = CciL4FileType.INSTANCE;

    @Test
    public void testReadSourceGrids() throws Exception {
        NetcdfFile l4File = TestL3ProductMaker.readL4GridsSetup();
        //execution
        final AggregationContext context = FILE_TYPE.readSourceGrids(l4File, SstDepth.skin, new AggregationContext());

        // analysed_sst
        final Grid sstGrid = context.getSstGrid();
        assertEquals(2000, sstGrid.getSampleInt(0, 3599));
        assertEquals(293.14999344944954, sstGrid.getSampleDouble(0, 3599), 1e-8);
        assertEquals(2000, sstGrid.getSampleInt(1, 3599));
        assertEquals(293.14999344944954, sstGrid.getSampleDouble(1, 3599), 1e-8);

        // analysis_error
        final Grid randomUncertaintyGrid = context.getRandomUncertaintyGrid();
        assertEquals(-32768, randomUncertaintyGrid.getSampleInt(0, 3599));
        assertTrue(Double.isNaN(randomUncertaintyGrid.getSampleDouble(0, 3599)));
        assertEquals(-32768, randomUncertaintyGrid.getSampleInt(1, 3599));
        assertTrue(Double.isNaN(randomUncertaintyGrid.getSampleDouble(1, 3599)));

        // sea_ice_fraction
        final Grid seaIceFractionGrid = context.getSeaIceFractionGrid();
        assertEquals(-128, seaIceFractionGrid.getSampleInt(0, 3599));
        assertTrue(Double.isNaN(seaIceFractionGrid.getSampleDouble(0, 3599)));
        assertEquals(-128, seaIceFractionGrid.getSampleInt(1, 0));
        assertTrue(Double.isNaN(seaIceFractionGrid.getSampleDouble(1, 3599)));
    }

}
