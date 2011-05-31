/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.rules.Context;

/**
 * Creates unmodifiable instances of ${@link Context}.
 *
 * @author Thomas Storm
 */
@SuppressWarnings("ReturnOfThis")
class ContextBuilder {

    private Matchup matchup;
    private byte insituDataset;
    private double time;

    ContextBuilder matchup(Matchup matchup) {
        this.matchup = matchup;
        return this;
    }

    ContextBuilder insituDataset(byte insituDataset) {
        this.insituDataset = insituDataset;
        return this;
    }

    ContextBuilder time(double time) {
        this.time = time;
        return this;
    }

    Context build() {
        return new ContextImpl(matchup, insituDataset, time);
    }


    private static class ContextImpl implements Context {

        private final Matchup matchup;
        private final byte insituDataset;
        private final double time;

        ContextImpl(Matchup matchup, byte insituDataset, double time) {
            this.matchup = matchup;
            this.insituDataset = insituDataset;
            this.time = time;
        }

        @Override
        public Matchup getMatchup() {
            return matchup;
        }

        @Override
        public byte getInsituDataset() {
            return insituDataset;
        }

        @Override
        public double getTime() {
            return time;
        }
    }
}