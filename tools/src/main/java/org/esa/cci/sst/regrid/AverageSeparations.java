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

package org.esa.cci.sst.regrid;

import org.esa.cci.sst.common.SpatialResolution;
import org.esa.cci.sst.common.TemporalResolution;
import org.esa.cci.sst.common.cellgrid.GridDef;

/**
 * Calculates average spatial and temporal separations for calculating synoptic uncertainties.
 * <p/>
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public class AverageSeparations {

    private final TemporalResolution temporalResolution;
    private final SpatialResolution spatialResolution;
    private final GridDef gridDef;

    /**
     * Creates a new instance of this class.
     *
     * @param spatialResolution  The spatial target resolution.
     * @param temporalResolution The temporal target resolution.
     */
    public AverageSeparations(SpatialResolution spatialResolution, TemporalResolution temporalResolution) {
        this.temporalResolution = temporalResolution;
        this.spatialResolution = spatialResolution;
        this.gridDef = GridDef.createGlobal(spatialResolution.getResolution());

    }

    /**
     * Calculates the average synoptic separation distance.
     *
     * @param y The index of a cell in the target grid.
     *
     * @return the average separation distance (km).
     */
    public double getDxy(int y) {
        final double targetResolution = spatialResolution.getResolution();
        if (targetResolution <= 0.05) {
            return 0.0;
        }

        double aPole = 37.2069;
        double bPole = -0.101691;
        double dPole = aPole * targetResolution + bPole;

        double aEquator = 57.8881;
        double bEquator = 0.272744;
        double dEquator = aEquator * targetResolution + bEquator;

        final double lat = gridDef.getCenterLat(y);
        final double f = Math.abs(lat) / 90.0;

        return dPole * f + dEquator * (1.0 - f);
    }

    /**
     * Returns the average synoptic time separation.
     *
     * @return the average time separation (days).
     */
    public double getDt() {
        if (temporalResolution.equals(TemporalResolution.daily)) {
            return 0.0;
        } else if (temporalResolution.equals(TemporalResolution.weekly5d)) {
            if (spatialResolution.getResolution() <= 1.5) {
                return 2.0;
            } else if (spatialResolution.getResolution() <= 2.5) {
                return 1.0;
            } else {
                return 0.0;
            }
        } else if (temporalResolution.equals(TemporalResolution.weekly7d)) {
            if (spatialResolution.getResolution() <= 1.75) {
                return 2.0;
            } else if (spatialResolution.getResolution() <= 2.5) {
                return 1.0;
            } else {
                return 0.0;
            }
        } else if (temporalResolution.equals(TemporalResolution.monthly)) {
            if (spatialResolution.getResolution() <= 0.5) {
                return 10.0;
            } else if (spatialResolution.getResolution() <= 0.75) {
                return 9.0;
            } else if (spatialResolution.getResolution() <= 0.8) {
                return 8.5;
            } else if (spatialResolution.getResolution() <= 1.0) {
                return 6.0;
            } else if (spatialResolution.getResolution() <= 1.2) {
                return 3.5;
            } else if (spatialResolution.getResolution() <= 1.25) {
                return 3.0;
            } else if (spatialResolution.getResolution() <= 2.00) {
                return 0.5;
            } else if (spatialResolution.getResolution() <= 2.25) {
                return 0.25;
            } else if (spatialResolution.getResolution() <= 2.5) {
                return 0.2;
            } else if (spatialResolution.getResolution() <= 3.0) {
                return 0.1;
            } else {
                return 0.0;
            }
        } else {
            return 0.0;
        }
    }
}
