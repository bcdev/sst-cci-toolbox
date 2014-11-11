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

package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tool.Configuration;
import ucar.nc2.Variable;

import java.util.Map;

/**
 * Context which provides access to data needed to write mmd file.
 *
 * @author Thomas Storm
 */
public interface Context {

    Matchup getMatchup();

    Reader getObservationReader();

    Reader getReferenceObservationReader();

    Observation getObservation();

    Variable getTargetVariable();

    Map<String, Integer> getDimensionConfiguration();

    Configuration getConfiguration();
}
