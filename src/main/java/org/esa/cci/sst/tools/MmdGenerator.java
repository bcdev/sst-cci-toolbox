/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools;

import ucar.nc2.NetcdfFileWriteable;

/**
* Interface for MmdGenerators.
*
* @author Thomas Storm
*/
interface MmdGenerator {

    static final String COUNT_MATCHUPS_QUERY =
            "select count( m ) "
            + " from Matchup m";

    static final String ALL_MATCHUPS_QUERY =
            "select m"
            + " from Matchup m"
            + " order by m.id";

    static final String TIME_CONSTRAINED_MATCHUPS_QUERY =
            "select m.id"
            + " from mm_matchup m, mm_observation o"
            + " where o.id = m.refobs_id"
            + " and o.time > TIMESTAMP WITH TIME ZONE '%s'"
            + " and o.time < TIMESTAMP WITH TIME ZONE '%s'"
            + " order by o.time";

    /**
     * Creates the netcdf structure for the given file, that is, variables, dimensions, and (global) attributes.
     *
     * @param file The target MMD file.
     *
     * @throws Exception If something goes wrong.
     */
    void createMmdStructure(NetcdfFileWriteable file) throws Exception;

    /**
     * Writes the matchups from the database into the MMD file.
     *
     * @param file The target MMD file.
     *
     * @throws Exception If something goes wrong.
     */
    void writeMatchups(NetcdfFileWriteable file) throws Exception;

    /**
     * Closes the generator and performs some cleanup.
     */
    void close();
}
