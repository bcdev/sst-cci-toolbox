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

package org.esa.cci.sst.util;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.DataSchema;

import java.io.File;

/**
 * Provides some common utility methods.
 *
 * @author Thomas Storm
 */
public class DataUtil {

    private DataUtil() {
    }

    public static DataSchema createDataSchema(final String schemaName, final String sensorType) {
        final DataSchema dataSchema = new DataSchema();
        dataSchema.setName(schemaName);
        dataSchema.setSensorType(sensorType);
        return dataSchema;
    }

    public static DataFile createDataFile(final File file, final DataSchema dataSchema) {
        final DataFile dataFile = new DataFile();
        dataFile.setPath(file.getPath());
        dataFile.setDataSchema(dataSchema);
        return dataFile;
    }
}
