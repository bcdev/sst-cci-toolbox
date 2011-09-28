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
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.rules.Context;
import ucar.nc2.Variable;

import java.util.Collections;
import java.util.Map;

/**
 * Creates unmodifiable instances of ${@link Context}.
 *
 * @author Thomas Storm
 */
@SuppressWarnings("ReturnOfThis")
class ContextBuilder {

    private Matchup matchup;
    private Reader observationReader;
    private Reader referenceObservationReader;
    private Variable targetVariable;
    private Observation observation;
    private Map<String, Integer> dimensionConfiguration;

    ContextBuilder matchup(Matchup matchup) {
        this.matchup = matchup;
        return this;
    }

    public ContextBuilder targetVariable(Variable targetVariable) {
        this.targetVariable = targetVariable;
        return this;
    }

    public ContextBuilder observationReader(Reader observationReader) {
        this.observationReader = observationReader;
        return this;
    }

    public ContextBuilder referenceObservationReader(Reader referenceObservationReader) {
        this.referenceObservationReader = referenceObservationReader;
        return this;
    }

    public ContextBuilder observation(Observation observation) {
        this.observation = observation;
        return this;
    }

    public ContextBuilder dimensionConfiguration(Map<String,Integer> dimensionConfiguration) {
        this.dimensionConfiguration = Collections.unmodifiableMap(dimensionConfiguration);
        return this;
    }

    @SuppressWarnings({"AccessingNonPublicFieldOfAnotherObject"})
    Context build() {
        final ContextImpl context = new ContextImpl();
        context.matchup = matchup;
        context.observation = observation;
        context.observationReader = observationReader;
        context.referenceObservationReader = referenceObservationReader;
        context.targetVariable = targetVariable;
        context.dimensionConfiguration = dimensionConfiguration;
        return context;
    }

    private static class ContextImpl implements Context {

        private Matchup matchup;
        private Reader observationReader;
        private Reader referenceObservationReader;
        private Observation observation;
        private Variable targetVariable;
        private Map<String, Integer> dimensionConfiguration;

        @Override
        public Matchup getMatchup() {
            return matchup;
        }

        @Override
        public Reader getObservationReader() {
            return observationReader;
        }

        @Override
        public Reader getReferenceObservationReader() {
            return referenceObservationReader;
        }

        @Override
        public Observation getObservation() {
            return observation;
        }

        @Override
        public Variable getTargetVariable() {
            return targetVariable;
        }

        @Override
        public Map<String, Integer> getDimensionConfiguration() {
            return Collections.unmodifiableMap(dimensionConfiguration);
        }
    }
}