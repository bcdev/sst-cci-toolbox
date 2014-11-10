/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.common.file;

import org.esa.cci.sst.common.AggregationContext;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.ArrayGrid;
import org.esa.cci.sst.common.GridDef;
import org.esa.cci.sst.util.NcUtils;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents the SST-CCI L2P file type.
 * <p/>
 * The implementation reads the L2P product file and makes a re-projection onto the L3U grid.
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
class CciL2FileType extends CciL3FileType {

    final static FileType INSTANCE = new CciL2FileType();

    private static final String QUALITY_LEVEL = "quality_level";
    private static final String UNCORRELATED_UNCERTAINTY = "uncorrelated_uncertainty";
    private static final String LARGE_SCALE_CORRELATED_UNCERTAINTY = "large_scale_correlated_uncertainty";
    private static final String SYNOPTIC_UNCERTAINTY = "synoptically_correlated_uncertainty";
    private static final String ADJUSTMENT_UNCERTAINTY = "adjustment_uncertainty";

    @Override
    public AggregationContext readSourceGrids(NetcdfFile datafile, SstDepth sstDepth, AggregationContext context) throws
                                                                                                                  IOException {
        final List<String> variableNames = new ArrayList<String>(8);
        if (sstDepth == SstDepth.skin) {
            variableNames.add("sea_surface_temperature");
        } else {
            variableNames.add("sea_surface_temperature_depth");
        }
        variableNames.add(QUALITY_LEVEL);
        variableNames.add(UNCORRELATED_UNCERTAINTY);
        variableNames.add(LARGE_SCALE_CORRELATED_UNCERTAINTY);
        variableNames.add(SYNOPTIC_UNCERTAINTY);
        if (NcUtils.hasVariable(datafile, ADJUSTMENT_UNCERTAINTY)) {
            variableNames.add(ADJUSTMENT_UNCERTAINTY);
        }
        final GridDef gridDef = getGridDef();
        final Projector projector = new Projector(gridDef, Logger.getLogger("org.esa.cci.sst"));
        final float[][] data = projector.createProjectedData(datafile, variableNames);

        context.setSstGrid(ArrayGrid.create(gridDef, data[0]));
        context.setQualityGrid(ArrayGrid.create(gridDef, data[1]));
        context.setRandomUncertaintyGrid(ArrayGrid.create(gridDef, data[2]));
        context.setLargeScaleUncertaintyGrid(ArrayGrid.create(gridDef, data[3]));
        context.setSynopticUncertaintyGrid(ArrayGrid.create(gridDef, data[4]));
        if (variableNames.size() > 5) {
            context.setAdjustmentUncertaintyGrid(ArrayGrid.create(gridDef, data[5]));
        }

        return context;
    }

    @Override
    public String getFilenameRegex() {
        return "\\d{14}-ESACCI-L2P_GHRSST-SST((skin)|(subskin)|(depth)|(fnd))-((ATSR1)|(ATSR2)|(AATSR)|(AVHRR\\d{2}_G)|(AMSRE)|(SEVIRI_SST)|(TMI))-((LT)|(DM))-v\\d{1,2}\\.\\d{1}-fv\\d{1,2}\\.\\d{1}.nc";
    }
}
