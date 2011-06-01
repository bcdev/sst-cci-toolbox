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

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.rules.Context;
import ucar.nc2.Variable;

/**
 * Creates unmodifiable instances of ${@link Context}.
 *
 * @author Thomas Storm
 */
@SuppressWarnings("ReturnOfThis")
class ContextBuilder {

    private Matchup matchup;
    private Coincidence coincidence;
    private Reader coincidenceReader;
    private Reader referenceObservationReader;
    private Variable targetVariable;

    ContextBuilder matchup(Matchup matchup) {
        this.matchup = matchup;
        return this;
    }

    ContextBuilder coincidence(Coincidence coincidence) {
        this.coincidence = coincidence;
        return this;
    }

    public ContextBuilder targetVariable(Variable targetVariable) {
        this.targetVariable = targetVariable;
        return this;
    }

    public ContextBuilder coincidenceReader(Reader coincidenceReader) {
        this.coincidenceReader = coincidenceReader;
        return this;
    }

    public ContextBuilder referenceObservationReader(Reader referenceObservationReader) {
        this.referenceObservationReader = referenceObservationReader;
        return this;
    }

    @SuppressWarnings({"AccessingNonPublicFieldOfAnotherObject"})
    Context build() {
        final ContextImpl context = new ContextImpl();
        context.matchup = matchup;
        context.coincidence = coincidence;
        context.coincidenceReader = coincidenceReader;
        context.referenceObservationReader = referenceObservationReader;
        context.targetVariable = targetVariable;
        return context;
    }

    private static class ContextImpl implements Context {

        private Matchup matchup;
        private Reader coincidenceReader;
        private Reader referenceObservationReader;
        private Coincidence coincidence;
        private Variable targetVariable;

        @Override
        public Matchup getMatchup() {
            return matchup;
        }

        @Override
        public Reader getCoincidenceReader() {
            return coincidenceReader;
        }

        @Override
        public Reader getReferenceObservationReader() {
            return referenceObservationReader;
        }

        @Override
        public Coincidence getCoincidence() {
            return coincidence;
        }

        @Override
        public Variable getTargetVariable() {
            return targetVariable;
        }
    }
}