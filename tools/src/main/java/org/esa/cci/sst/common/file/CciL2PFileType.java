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

import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.cellgrid.ArrayGrid;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.util.NcUtils;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Not yet implemented.
 *
 * @author Bettina Scholze
 */
public class CciL2PFileType extends CciL3FileType {
    public final static CciL2PFileType INSTANCE = new CciL2PFileType();
    private final String FORMAT = "NetCDF-CF";


    @Override
    public Grid[] readSourceGrids(NetcdfFile netcdfFile, SstDepth sstDepth) throws IOException {
        File file = new File(netcdfFile.getLocation());
        Grid[] grids = new Grid[6];
        String variable;

        if (sstDepth == SstDepth.depth_20 || sstDepth == SstDepth.depth_100) {
            variable =  "sea_surface_temperature_depth";
            CciL2PReprojection sstReprojection = new CciL2PReprojection();
            Array sstDepthArray = sstReprojection.doReprojection(file, FORMAT, variable);
            grids[0] = createArrayGrid(sstReprojection, sstDepthArray);
        } else /*if (sstDepth == SstDepth.skin)*/ {
            variable = "sea_surface_temperature";
            CciL2PReprojection sstReprojection = new CciL2PReprojection();
            Array sstArray = sstReprojection.doReprojection(file, FORMAT, variable);
            grids[0] = createArrayGrid(sstReprojection, sstArray);
        }
        variable = "quality_level";
        CciL2PReprojection qualityArrayReprojection = new CciL2PReprojection();
        Array qualityArray = qualityArrayReprojection.doReprojection(file, FORMAT, variable);
        grids[1] = createArrayGrid(qualityArrayReprojection, qualityArray);

        variable = "uncorrelated_uncertainty";
        CciL2PReprojection uncorrelatedUncertaintyReprojection = new CciL2PReprojection();
        Array uncorrelatedUncertaintyArray = uncorrelatedUncertaintyReprojection.doReprojection(file, FORMAT, variable);
        grids[2] = createArrayGrid(uncorrelatedUncertaintyReprojection, uncorrelatedUncertaintyArray);

        variable = "large_scale_correlated_uncertainty";
        CciL2PReprojection largeScaleCorrelatedUncertaintyReprojection = new CciL2PReprojection();
        Array largeScaleCorrelatedUncertaintyArray = largeScaleCorrelatedUncertaintyReprojection.doReprojection(file, FORMAT, variable);
        grids[3] = createArrayGrid(largeScaleCorrelatedUncertaintyReprojection, largeScaleCorrelatedUncertaintyArray);

        variable = "synoptically_correlated_uncertainty";
        CciL2PReprojection synopticallyCorrelatedUncertaintyReprojection = new CciL2PReprojection();
        Array synopticallyCorrelatedUncertaintyArray = synopticallyCorrelatedUncertaintyReprojection.doReprojection(file, FORMAT, variable);
        grids[4] = createArrayGrid(synopticallyCorrelatedUncertaintyReprojection, synopticallyCorrelatedUncertaintyArray);

        variable = "adjustment_uncertainty";
        if (NcUtils.missesVariable(netcdfFile, variable)) {
            return Arrays.copyOf(grids, 5);
        }
        CciL2PReprojection adjustmentUncertaintyReprojection = new CciL2PReprojection();
        Array adjustmentUncertaintyArray = adjustmentUncertaintyReprojection.doReprojection(file, FORMAT, variable);
        grids[5] = createArrayGrid(adjustmentUncertaintyReprojection, adjustmentUncertaintyArray);

        return grids;
    }

    @Override
    public String getFilenameRegex() {
        return "\\d{14}-" + getRdac() + "-L2P_GHRSST-SST((skin)|(subskin)|(depth)|(fnd))[-]" +
                "((ATSR1)|(ATSR2)|(AATSR)|(AMSRE)|(SEVIRI_SST)|(TMI))[-]((LT)|(DM))-" +
                "v\\d{1,2}\\.\\d{1}-fv\\d{1,2}\\.\\d{1}.nc";
    }

    private ArrayGrid createArrayGrid(CciL2PReprojection cciL2PReprojection, Array array) {
        Number fillValue = cciL2PReprojection.getFillValue();
        double scaling = cciL2PReprojection.getScaling();
        double offset = cciL2PReprojection.getOffset();
        return new ArrayGrid(getGridDef(), array, fillValue, scaling, offset);
    }
}
