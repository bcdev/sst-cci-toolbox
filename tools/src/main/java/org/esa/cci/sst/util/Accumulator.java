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

package org.esa.cci.sst.util;

/**
 * An accumulator for numbers.
 *
 * @author Norman Fomferra
 */
public abstract class Accumulator {

    protected Accumulator() {
    }

    public void accumulate(double sample, double weight) {
        if (!Double.isNaN(sample) && !Double.isNaN(weight)) {
            accumulateSample(sample, weight);
        }
    }

    public abstract long getSampleCount();

    public abstract double computeAverage();

    protected abstract void accumulateSample(double sample, double weight);
}
