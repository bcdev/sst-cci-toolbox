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

import java.util.Arrays;
import java.util.Date;

/**
 * Possible temporal resolutions.
 *
 * @author Norman Fomferra
 */
public enum TemporalResolution {
    daily,
    weekly7d,
    weekly5d,
    monthly,
    seasonal,
    annual;

    private Date date1;

    public void setDate1(Date date1) {
        this.date1 = date1;
    }

    public Date getDate1() {
        return date1;
    }

    public static String valuesForAveraging() {
        final TemporalResolution[] values = values();

        String[] valuesForAveraging = new String[4];
        int i = 0;
        for (TemporalResolution value : values) {
            if (!"weekly5d".equals(value.name()) && !"weekly7d".equals(value.name())){
                valuesForAveraging[i++] = value.name();
            }
        }

        return Arrays.toString(valuesForAveraging);
    }
}
