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

import org.esa.cci.sst.common.LUT;
import org.esa.cci.sst.common.SpatialResolution;
import org.esa.cci.sst.common.TemporalResolution;
import org.esa.cci.sst.common.calculator.CoverageUncertainty;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cellgrid.GridDef;

import java.util.Calendar;
import java.util.Date;

/**
 * Calculates sampling/coverage uncertainties for regridding (Eq. 1.6)
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
class RegriddingCoverageUncertainty implements CoverageUncertainty {

    private final LUT lut2;
    private final LUT lut3;
    private final GridDef gridDef;
    private final double xDay;

    RegriddingCoverageUncertainty(LUT lut2, LUT lut3,
                                  SpatialResolution spatialResolution,
                                  TemporalResolution temporalResolution,
                                  Date date) {
        this.lut2 = lut2;
        this.lut3 = lut3;
        this.gridDef = GridDef.createGlobal(spatialResolution.getResolution());
        this.xDay = calculateXDay(temporalResolution, date);
    }

    static double calculateXDay(TemporalResolution temporalResolution, Date date) {
        if (TemporalResolution.daily.equals(temporalResolution)) {
            return 0.0;
        } else if (TemporalResolution.monthly.equals(temporalResolution)) {
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        } else {
            throw new IllegalArgumentException("TemporalResolution must be 'daily' or 'monthly'.");
        }
    }

    @Override
    public double calculate(AggregationCell cell, double a) {
        final int x = cell.getX();
        final int y = cell.getY();

        final double x0Space = lut2.getGrid().getSampleDouble(x, y);
        final double xKm = gridDef.getDiagonal(x, y);
        final double rBarSpace = (x0Space / xKm) * (1.0 - Math.exp(-xKm / x0Space));

        final double rBarTime;
        if (xDay == 0.0) {
            rBarTime = 1.0;
        } else {
            final double x0Time = lut3.getGrid().getSampleDouble(x, y);
            rBarTime = (x0Time / xDay) * (1.0 - Math.exp(-xDay / x0Time));
        }

        final double rBar = rBarSpace * rBarTime;

        return Math.sqrt((a * rBar * (1.0 - rBar)) / (1.0 + (cell.getSampleCount() - 1.0) * rBar));
    }
}
