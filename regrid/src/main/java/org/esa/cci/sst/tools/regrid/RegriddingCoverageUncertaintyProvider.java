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

package org.esa.cci.sst.tools.regrid;

import org.esa.cci.sst.grid.LUT;
import org.esa.cci.sst.aggregate.CoverageUncertaintyProvider;
import org.esa.cci.sst.aggregate.AggregationCell;
import org.esa.cci.sst.grid.Grid;

import java.util.Date;

/**
 * For calculating coverage uncertainties for regridding (Eq. 1.6)
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
class RegriddingCoverageUncertaintyProvider implements CoverageUncertaintyProvider {

    private final Grid x0space;
    private final Grid x0time;
    private final double xDay;

    @Override
    public double calculate(AggregationCell cell, double variance) {
        final int x = cell.getX();
        final int y = cell.getY();

        final double x0s = x0space.getSampleDouble(x, y);
        final double xKm = x0space.getGridDef().getDiagonal(x, y);
        final double rBarSpace = (x0s / xKm) * (1.0 - Math.exp(-xKm / x0s));

        final double rBarTime;
        if (xDay == 0.0) {
            rBarTime = 1.0;
        } else {
            final double x0t = x0time.getSampleDouble(x, y);
            rBarTime = (x0t / xDay) * (1.0 - Math.exp(-xDay / x0t));
        }
        final double rBar = rBarSpace * rBarTime;

        return Math.sqrt((variance * rBar * (1.0 - rBar)) / (1.0 + (cell.getSampleCount() - 1.0) * rBar));
    }

    RegriddingCoverageUncertaintyProvider(LUT lut2, LUT lut3, Date date1, Date date2) {
        x0space = lut2.getGrid();
        x0time = lut3.getGrid();
        xDay = calculateXDay(date1, date2);
    }

    // package access for testing only tb 2014-11-12
    static double calculateXDay(Date date1, Date date2) {
        return (date2.getTime() - date1.getTime()) / 86400000;
    }
}
