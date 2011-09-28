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

package org.esa.cci.sst.reader;

import java.io.IOException;

/**
 * Interface for a source of in-situ data.
 *
 * @author Ralf Quast
 */
public interface InsituSource {

    /**
     * Returns the in-situ longitude (degrees east)
     *
     * @param recordNo The record number.
     *
     * @return the in-situ longitude (degrees east).
     *
     * @throws java.io.IOException when an I/O error occurred.
     */
    double readInsituLon(int recordNo) throws IOException;

    /**
     * Returns the in-situ latitude (degrees north)
     *
     * @param recordNo The record number.
     *
     * @return the in-situ latitude (degrees north).
     *
     * @throws java.io.IOException when an I/O error occurred.
     */
    double readInsituLat(int recordNo) throws IOException;

    /**
     * Returns the in-situ time (seconds since 01-01-1978 00:00:00)
     *
     * @param recordNo The record number.
     *
     * @return the in-situ time (seconds since 01-01-1978 00:00:00).
     *
     * @throws java.io.IOException when an I/O error occurred.
     */
    double readInsituTime(int recordNo) throws IOException;

    /**
     * Returns the in-situ SST (K)
     *
     * @param recordNo The record number.
     *
     * @return the in-situ SST (K).
     *
     * @throws java.io.IOException when an I/O error occurred.
     */
    double readInsituSst(int recordNo) throws IOException;
}
