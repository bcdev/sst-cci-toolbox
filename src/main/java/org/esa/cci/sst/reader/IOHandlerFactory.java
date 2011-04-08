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

import org.esa.cci.sst.tools.Constants;

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
        if (Constants.DATA_SCHEMA_NAME_ATSR_MD.equalsIgnoreCase(schemaName)) {
            return new AtsrMdIOHandler();
        }
        if (Constants.DATA_SCHEMA_NAME_METOP_MD.equalsIgnoreCase(schemaName)) {
            return new MetopIOHandler();
        }
        if (Constants.DATA_SCHEMA_NAME_SEVIRI_MD.equalsIgnoreCase(schemaName)) {
            return new SeviriIOHandler();
        }
        if (Constants.DATA_SCHEMA_NAME_AMSRE.equalsIgnoreCase(schemaName)) {
            return new ProductIOHandler(sensorName, new DefaultBoundaryCalculator());
        }
        if (Constants.DATA_SCHEMA_NAME_TMI.equalsIgnoreCase(schemaName)) {
            return new ProductIOHandler(sensorName, new DefaultBoundaryCalculator());
        }
        if (Constants.DATA_SCHEMA_NAME_ATSR_L1B.equalsIgnoreCase(schemaName)) {
            return new ProductIOHandler(sensorName, new DefaultBoundaryCalculator());
        }
        if (Constants.DATA_SCHEMA_NAME_AAI.equalsIgnoreCase(schemaName)) {
            return new ProductIOHandler(sensorName, null);
        }
        if (Constants.DATA_SCHEMA_NAME_AVHRR_GAC.equalsIgnoreCase(schemaName)) {
            return new ProductIOHandler(sensorName, new DefaultBoundaryCalculator());
        }
        if (Constants.DATA_SCHEMA_NAME_SEAICE.equalsIgnoreCase(schemaName)) {
            return new ProductIOHandler(sensorName, new DefaultBoundaryCalculator());
        }
        if (Constants.DATA_SCHEMA_INSITU_HISTORY.equalsIgnoreCase(schemaName)) {
            return new InsituIOHandler();
        }
        throw new IllegalArgumentException(
                MessageFormat.format("No appropriate IO handler for schema {0} found.", schemaName));
    }
}
