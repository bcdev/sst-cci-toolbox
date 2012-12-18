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
     *
     * @param cellY index of the cell in the target grid
     * @return Average separation distance in km
     */
    public double getDxy(double cellY) {//todo implement it!
        if (targetSpatialResolution.getValue() <= 0.05) {
            return 0.0;
        }

        final GridDef gridDef = GridDef.createGlobal(targetSpatialResolution.getValue());
        final double lat = gridDef.getLat(cellY); //unit: degree

        double slope = interpolateSlope(lat);
        return slope * targetSpatialResolution.getValue();
    }

    double interpolateSlope(double latitude) {  //todo implement it!
        double aPole = 0.0;  //todo
        double aEquator = 10.0; //todo

        return (aPole + aEquator) * (Math.abs(latitude) / 90.0);
    }

    /**
     * @return Average time separation in days
     */
    public double getDt() {

        if (targetTemporalResolution.equals(TemporalResolution.daily)) {
            return 0.0;
        } else if (targetTemporalResolution.equals(TemporalResolution.weekly5d)) {
            if (targetSpatialResolution.getValue() <= 1.5) {
                return 2.0;
            } else if (targetSpatialResolution.getValue() <= 2.5) {
                return 1.0;
            } else {
                return 0.0;
            }
        } else if (targetTemporalResolution.equals(TemporalResolution.weekly7d)) {
            if (targetSpatialResolution.getValue() <= 1.75) {
                return 2.0;
            } else if (targetSpatialResolution.getValue() <= 2.5) {
                return 1.0;
            } else {
                return 0.0;
            }
        } else if (targetTemporalResolution.equals(TemporalResolution.monthly)) {
            if (targetSpatialResolution.getValue() <= 0.5) {
                return 10.0;
            } else if (targetSpatialResolution.getValue() <= 0.75) {
                return 9.0;
            } else if (targetSpatialResolution.getValue() <= 0.8) {
                return 8.5;
            } else if (targetSpatialResolution.getValue() <= 1.0) {
                return 6.0;
            } else if (targetSpatialResolution.getValue() <= 1.2) {
                return 3.5;
            } else if (targetSpatialResolution.getValue() <= 1.25) {
                return 3.0;
            } else if (targetSpatialResolution.getValue() <= 2.00) {
                return 0.5;
            } else if (targetSpatialResolution.getValue() <= 2.25) {
                return 0.25;
            } else if (targetSpatialResolution.getValue() <= 2.5) {
                return 0.2;
            } else if (targetSpatialResolution.getValue() <= 3.0) {
                return 0.1;
            } else {
                return 0.0;
            }
        } else {
            return 0.0; //unit days
        }
    }
}
