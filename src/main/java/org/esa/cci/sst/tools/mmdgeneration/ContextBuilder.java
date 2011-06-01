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
import ucar.ma2.Array;

/**
 * Creates unmodifiable instances of ${@link Context}.
 *
 * @author Thomas Storm
 */
@SuppressWarnings("ReturnOfThis")
class ContextBuilder {

    private Matchup matchup;
    private byte insituDataset;
    private double matchupTime;
    private double metopTime;
    private Array metopDTimes;

    ContextBuilder matchup(Matchup matchup) {
        this.matchup = matchup;
        return this;
    }

    ContextBuilder insituDataset(byte insituDataset) {
        this.insituDataset = insituDataset;
        return this;
    }

    ContextBuilder matchupTime(double time) {
        this.matchupTime = time;
        return this;
    }

    ContextBuilder metopTime(double metopTime) {
        this.metopTime = metopTime;
        return this;
    }

    ContextBuilder metopDTimes(Array metopDTimes) {
        this.metopDTimes = metopDTimes;
        return this;
    }

    @SuppressWarnings({"AccessingNonPublicFieldOfAnotherObject"})
    Context build() {
        final ContextImpl context = new ContextImpl();
        context.matchup = matchup;
        context.insituDataset = insituDataset;
        context.matchupTime = matchupTime;
        context.metopTime = metopTime;
        context.metopDTimes = metopDTimes;
        return context;
    }


    private static class ContextImpl implements Context {

        private Matchup matchup;
        private byte insituDataset;
        private double matchupTime;
        private double metopTime;
        private Array metopDTimes;

        @Override
        public Matchup getMatchup() {
            return matchup;
        }

        @Override
        public byte getInsituDataset() {
            return insituDataset;
        }

        @Override
        public double getMatchupTime() {
            return matchupTime;
        }

        @Override
        public double getMetopTime() {
            return metopTime;
        }

        @Override
        public Array getMetopDTimes() {
            return metopDTimes;
        }
    }
}