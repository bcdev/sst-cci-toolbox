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

import static org.esa.cci.sst.SensorName.*;

/**
 * TODO fill out or delete
 *
 * @author Thomas Storm
 */
public class ReaderFactory {

    public static ObservationReader createReader(String schemaName) throws Exception {
        ObservationReader reader;
        if (Constants.DATA_SCHEMA_NAME_AATSR_MD.equalsIgnoreCase(schemaName)) {
            reader = new AatsrMdReader();
        } else if (Constants.DATA_SCHEMA_NAME_METOP_MD.equalsIgnoreCase(schemaName)) {
            reader = new MetopMdReader();
        } else if (Constants.DATA_SCHEMA_NAME_SEVIRI_MD.equalsIgnoreCase(schemaName)) {
            reader = new SeviriMatchupReader();
        } else if (Constants.DATA_SCHEMA_NAME_AMR.equalsIgnoreCase(schemaName)) {
            reader = new ProductObservationReader(SENSOR_NAME_AMSRE.getSensor(), new DefaultGeoBoundaryCalculator());
        } else if (Constants.DATA_SCHEMA_NAME_TMI.equalsIgnoreCase(schemaName)) {
            reader = new ProductObservationReader(SENSOR_NAME_TMI.getSensor(), new DefaultGeoBoundaryCalculator());
        } else if (Constants.DATA_SCHEMA_NAME_ATSR.equalsIgnoreCase(schemaName)) {
            reader = new ProductObservationReader(SENSOR_NAME_AATSR.getSensor(), new DefaultGeoBoundaryCalculator());
        } else if (Constants.DATA_SCHEMA_NAME_AAI.equalsIgnoreCase(schemaName)) {
            reader = new ProductObservationReader(SENSOR_NAME_AAI.getSensor(), new NullGeoBoundaryCalculator());
        } else if (Constants.DATA_SCHEMA_NAME_AVHRR_GAC.equalsIgnoreCase(schemaName)) {
            reader = new ProductObservationReader(SENSOR_NAME_AVHRR.getSensor(), new DefaultGeoBoundaryCalculator());
        } else if (Constants.DATA_SCHEMA_NAME_SEA_ICE.equalsIgnoreCase(schemaName)) {
            reader = new ProductObservationReader(SENSOR_NAME_SEA_ICE.getSensor(), new DefaultGeoBoundaryCalculator());
        } else if (Constants.DATA_SCHEMA_INSITU.equalsIgnoreCase(schemaName)) {
            reader = new InsituHistoryReader();
        } else {
            // todo - sea ice quality and in-situ
            throw new Exception(MessageFormat.format("No appropriate reader for schema {0} found", schemaName));
        }
        return reader;
    }
}
