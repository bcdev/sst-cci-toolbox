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

package org.esa.cci.sst.common;

import org.esa.cci.sst.aggregate.AggregationCell;

/**
 * Calculates average spatial and temporal separations for calculating synoptic uncertainties.
 * <p/>
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public class SynopticUncertaintyProvider {

    private static final double LXY = 100.0; // 100 km
    private static final double ONE_DIV_90 = 1.0 / 90.0;

    private final double spatialResolution;
    private final TemporalResolution temporalResolution;

    /**
     * Creates a new instance of this class.
     *
     * @param spatialResolution  The spatial target resolution.
     * @param temporalResolution The temporal target resolution.
     */
    public SynopticUncertaintyProvider(SpatialResolution spatialResolution, TemporalResolution temporalResolution) {
        this.spatialResolution = spatialResolution.getResolution();
        this.temporalResolution = temporalResolution;
    }

    /**
     * Calculates the synoptic uncertainty for a given aggregation cell.
     *
     * @param cell The aggregation cell.
     * @param a    The accumulated synoptic uncertainties.
     * @return the synoptic uncertainty.
     */
    public double calculate(AggregationCell cell, double a) {
        return a / eta(cell.getY(), cell.getSampleCount());
    }

    /**
     * Calculates the average synoptic separation distance.
     *
     * @param y The y index of a cell in the target grid (km).
     * @return the average separation distance (km).
     */
    double dxy(int y) {
        final double aPole = 37.2069;
        final double bPole = -0.101691;
        final double dPole = aPole * spatialResolution + bPole;

        final double aEquator = 57.8881;
        final double bEquator = 0.272744;
        final double dEquator = aEquator * spatialResolution + bEquator;

        final double lat = 90.0 - spatialResolution * (y + 0.5);
        final double f = Math.abs(lat) * ONE_DIV_90;

        return dPole * f + dEquator * (1.0 - f);
    }

    /**
     * Returns the average synoptic time separation (days).
     *
     * @return the average time separation (days).
     */
    double dt() {
        if (TemporalResolution.daily.equals(temporalResolution)) {
            return 0.0;
        } else if (TemporalResolution.weekly5d.equals(temporalResolution)) {
            if (spatialResolution <= 1.5) {
                return 2.0;
            } else if (spatialResolution <= 2.5) {
                return 1.0;
            } else {
                return 0.0;
            }
        } else if (TemporalResolution.weekly7d.equals(temporalResolution)) {
            if (spatialResolution <= 1.75) {
                return 2.0;
            } else if (spatialResolution <= 2.5) {
                return 1.0;
            } else {
                return 0.0;
            }
        } else if (TemporalResolution.monthly.equals(temporalResolution)) {
            if (spatialResolution <= 0.5) {
                return 10.0;
            } else if (spatialResolution <= 0.75) {
                return 9.0;
            } else if (spatialResolution <= 0.8) {
                return 8.5;
            } else if (spatialResolution <= 1.0) {
                return 6.0;
            } else if (spatialResolution <= 1.2) {
                return 3.5;
            } else if (spatialResolution <= 1.25) {
                return 3.0;
            } else if (spatialResolution <= 2.00) {
                return 0.5;
            } else if (spatialResolution <= 2.25) {
                return 0.25;
            } else if (spatialResolution <= 2.5) {
                return 0.2;
            } else if (spatialResolution <= 3.0) {
                return 0.1;
            } else {
                return 0.0;
            }
        } else {
            return 0.0;
        }
    }

    double r(int y) {
        return Math.exp(-0.5 * (dxy(y) / LXY + dt()));
    }

    /**
     * Returns the effective number of synoptic areas in a grid cell.
     *
     * @param y           The y index of the grid cell.
     * @param sampleCount The number of samples accumulated.
     * @return the effective number of synoptic areas.
     */
    double eta(int y, long sampleCount) {
        return sampleCount / (1.0 + r(y) * (sampleCount - 1));
    }
}
