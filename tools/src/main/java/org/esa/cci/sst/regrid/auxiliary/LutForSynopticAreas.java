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

package org.esa.cci.sst.regrid.auxiliary;

import org.esa.cci.sst.common.TemporalResolution;
import org.esa.cci.sst.regrid.SpatialResolution;

/**
 * In the Regridding Tool the synoptically correlated uncertainties are aggregated (equation 1.3)
 * For calculation of the synoptic areas in the new grid box eta is defined by equations 1.4 and 1.5.
 * This Lut gives the values d_t - the average time separation.
 * <p/>
 * {@author Bettina Scholze}
 * Date: 17.12.12 09:39
 */
public class LutForSynopticAreas {

    private TemporalResolution targetTemporalResolution;
    private SpatialResolution targetSpatialResolution;

    public LutForSynopticAreas(TemporalResolution targetTemporalResolution, SpatialResolution targetSpatialResolution) {
        this.targetTemporalResolution = targetTemporalResolution;
        this.targetSpatialResolution = targetSpatialResolution;
    }

    /**
     * @return average time separation in days
     */
    public double getDt() {

        if (targetTemporalResolution.equals(TemporalResolution.daily)) {
            return 0.0;
//        } else if (targetTemporalResolution.equals(TemporalResolution.weekly)) //todo introduce weekly first
        } else if (targetTemporalResolution.equals(TemporalResolution.monthly)) {
            if (targetSpatialResolution.getValue() <= 0.5) {
                return 10.0;
            } else if (targetSpatialResolution.getValue() <= 0.75) {
                return 9.0;
            } else if (targetSpatialResolution.getValue() == 0.8) {
                return 8.0;
            } else if (targetSpatialResolution.getValue() == 1.0) {
                return 6.0;
            } else {//if (targetSpatialResolution.getValue() >= 2.0) {
                return 0.0;
            }
        } else {
            return 0.0; //unit days
        }
    }
}
