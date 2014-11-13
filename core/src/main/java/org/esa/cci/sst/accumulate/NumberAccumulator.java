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

package org.esa.cci.sst.accumulate;

/**
 * An accumulator whose samples are single, weighted numbers.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public abstract class NumberAccumulator implements Accumulator {

    protected NumberAccumulator() {
    }

    public final void accumulate(double sample) {
        if (!Double.isNaN(sample)) {
            accumulateSample(sample, 1.0);
        }
    }

    public final void accumulate(double sample, double weight) {
        if (!Double.isNaN(sample) && !Double.isNaN(weight) && weight != 0.0) {
            accumulateSample(sample, weight);
        }
    }

    public abstract double combine();

    protected abstract void accumulateSample(double sample, double weight);
}
