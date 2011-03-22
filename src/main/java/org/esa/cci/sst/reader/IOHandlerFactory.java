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

package org.esa.cci.sst.reader;

import org.esa.cci.sst.Constants;

import java.text.MessageFormat;

/**
 * Factory providing a static method for getting the correct io handler, according to given schema name.
 *
 * @author Thomas Storm
 */
public class IOHandlerFactory {

    /**
     * Factory method for getting the correct io handler, according to given schema and sensor names.
     *
     * @param schemaName The schema name.
     * @param sensorName The sensor name.
     *
     * @return a new instance of <code>IOHandler</code>.
     */
    public static IOHandler createHandler(String schemaName, String sensorName) {
        if (Constants.DATA_SCHEMA_NAME_AATSR_MD.equalsIgnoreCase(schemaName)) {
            return new AatsrMdIOHandler();
        }
        if (Constants.DATA_SCHEMA_NAME_METOP_MD.equalsIgnoreCase(schemaName)) {
            return new MetopMdIOHandler();
        }
        if (Constants.DATA_SCHEMA_NAME_SEVIRI_MD.equalsIgnoreCase(schemaName)) {
            return new SeviriMatchupIOHandler();
        }
        if (Constants.DATA_SCHEMA_NAME_AMR.equalsIgnoreCase(schemaName)) {
            return new ProductIOHandler(sensorName, new DefaultGeoBoundaryCalculator());
        }
        if (Constants.DATA_SCHEMA_NAME_TMI.equalsIgnoreCase(schemaName)) {
            return new ProductIOHandler(sensorName, new DefaultGeoBoundaryCalculator());
        }
        if (Constants.DATA_SCHEMA_NAME_ATSR.equalsIgnoreCase(schemaName)) {
            return new ProductIOHandler(sensorName, new DefaultGeoBoundaryCalculator());
        }
        if (Constants.DATA_SCHEMA_NAME_AAI.equalsIgnoreCase(schemaName)) {
            return new ProductIOHandler(sensorName, new NullGeoBoundaryCalculator());
        }
        if (Constants.DATA_SCHEMA_NAME_AVHRR_GAC.equalsIgnoreCase(schemaName)) {
            return new ProductIOHandler(sensorName, new DefaultGeoBoundaryCalculator());
        }
        if (Constants.DATA_SCHEMA_NAME_SEA_ICE.equalsIgnoreCase(schemaName)) {
            return new ProductIOHandler(sensorName, new DefaultGeoBoundaryCalculator());
        }
        if (Constants.DATA_SCHEMA_INSITU.equalsIgnoreCase(schemaName)) {
            return new InsituHistoryIOHandler();
        }
        throw new IllegalArgumentException(
                MessageFormat.format("No appropriate IO handler for schema {0} found.", schemaName));
    }
}
