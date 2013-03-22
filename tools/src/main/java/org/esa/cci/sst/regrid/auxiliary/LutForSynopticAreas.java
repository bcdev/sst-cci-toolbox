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
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.regrid.SpatialResolution;

/**
 * In the Regridding Tool the synoptically correlated uncertainties are aggregated (equation 1.3)
 * For calculation of the synoptic areas in the new grid box eta is defined by equations 1.4 and 1.5.
 * This Lut gives the values d_t - the average time separation.
 * <p/>
 * {@author Bettina Scholze}
 */
public class LutForSynopticAreas {

    private final TemporalResolution targetTemporalResolution;
    private final SpatialResolution targetSpatialResolution;
    private final GridDef targetGridDef;

    public LutForSynopticAreas(TemporalResolution targetTemporalResolution, SpatialResolution targetSpatialResolution) {
        this.targetTemporalResolution = targetTemporalResolution;
        this.targetSpatialResolution = targetSpatialResolution;
        this.targetGridDef = GridDef.createGlobal(targetSpatialResolution.getResolution());

    }

    /**
     * Calculates the average separation distance.
     *
     * @param gridY The index of the cell in the target grid.
     *
     * @return the Average separation distance in km.
     */
    public double getDxy(int gridY) {
        final double targetResolution = targetSpatialResolution.getResolution();
        if (targetResolution <= 0.05) {
            return 0.0;
        }

        double aPole = 37.2069;
        double bPole = -0.101691;
        double dPole = aPole * targetResolution + bPole;

        double aEquator = 57.8881;
        double bEquator = 0.272744;
        double dEquator = aEquator * targetResolution + bEquator;

        final double lat = targetGridDef.getCenterLat(gridY);
        final double f = Math.abs(lat) / 90.0;

        return dPole * f + dEquator * (1.0 - f);
    }

    /**
     * Returns the average time separation in days.
     *
     * @return the average time separation in days.
     */
    public double getDt() {
        if (targetTemporalResolution.equals(TemporalResolution.daily)) {
            return 0.0;
        } else if (targetTemporalResolution.equals(TemporalResolution.weekly5d)) {
            if (targetSpatialResolution.getResolution() <= 1.5) {
                return 2.0;
            } else if (targetSpatialResolution.getResolution() <= 2.5) {
                return 1.0;
            } else {
                return 0.0;
            }
        } else if (targetTemporalResolution.equals(TemporalResolution.weekly7d)) {
            if (targetSpatialResolution.getResolution() <= 1.75) {
                return 2.0;
            } else if (targetSpatialResolution.getResolution() <= 2.5) {
                return 1.0;
            } else {
                return 0.0;
            }
        } else if (targetTemporalResolution.equals(TemporalResolution.monthly)) {
            if (targetSpatialResolution.getResolution() <= 0.5) {
                return 10.0;
            } else if (targetSpatialResolution.getResolution() <= 0.75) {
                return 9.0;
            } else if (targetSpatialResolution.getResolution() <= 0.8) {
                return 8.5;
            } else if (targetSpatialResolution.getResolution() <= 1.0) {
                return 6.0;
            } else if (targetSpatialResolution.getResolution() <= 1.2) {
                return 3.5;
            } else if (targetSpatialResolution.getResolution() <= 1.25) {
                return 3.0;
            } else if (targetSpatialResolution.getResolution() <= 2.00) {
                return 0.5;
            } else if (targetSpatialResolution.getResolution() <= 2.25) {
                return 0.25;
            } else if (targetSpatialResolution.getResolution() <= 2.5) {
                return 0.2;
            } else if (targetSpatialResolution.getResolution() <= 3.0) {
                return 0.1;
            } else {
                return 0.0;
            }
        } else {
            return 0.0;
        }
    }
}
