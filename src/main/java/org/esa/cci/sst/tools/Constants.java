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

package org.esa.cci.sst.tools;

public class Constants {

    /**
     * Name of persistence unit in META-INF/persistence.xml
     */
    public static final String PERSISTENCE_UNIT_NAME = "matchupdb";

    public static final String DIMENSION_NAME_MATCHUP = "matchup";
    public static final String DIMENSION_NAME_CALLSIGN_LENGTH = "callsign_length";
    public static final String DIMENSION_NAME_FILENAME_LENGTH = "filename_length";

    public static final String UNIT_BRIGHTNESS_TEMPERATURE = "K";
    public static final String UNIT_LAT = "degrees_north";
    public static final String UNIT_LON = "degrees_east";
    public static final String UNIT_SEA_SURFACE_TEMPERATURE = "K";
    public static final String UNIT_DTIME = "milliseconds";
    public static final String UNIT_TIME = "seconds since 1978-01-01 00:00:00";
    public static final String UNIT_INSITU_TIME = "seconds from matchup.time";

    public static final String VARIABLE_NAME_MATCHUP_ID = "matchup.id";
    public static final String VARIABLE_NAME_ARC2_MATCHUP_ID = "matchup_id";
    public static final String VARIABLE_NAME_OBSERVATION_TIME = "observation_time";

    public static final String PROPERTY_LOCATIONFILE = "mms.pixelpos.locationfile";
    public static final String PROPERTY_LATLONFILE = "mms.pixelpos.latlonfile";

    public static final String PROPERTY_MATCHUP_START_TIME = "mms.matchup.startTime";
    public static final String PROPERTY_MATCHUP_STOP_TIME = "mms.matchup.stopTime";

    public static final String PROPERTY_SOURCE_START_TIME = "mms.source.startTime";
    public static final String PROPERTY_SOURCE_END_TIME = "mms.source.endTime";

    public static final String PROPERTY_OUTPUT_START_TIME = "mms.arc1x2.startTime";
    public static final String PROPERTY_OUTPUT_END_TIME = "mms.arc1x2.endTime";
    public static final String PROPERTY_OUTPUT_TMPDIR = "mms.arc1x2.tmpdir";
    public static final String PROPERTY_OUTPUT_DESTDIR = "mms.arc1x2.destdir";

    public static final String PROPERTY_MMS_REINGESTION_FILENAME = "mms.reingestion.filename";
    public static final String PROPERTY_MMS_REINGESTION_LOCATED = "mms.reingestion.located";

    public static final String PROPERTY_MMS_NWP_ARC3_SENSOR = "mms.nwp_arc3.sensor";
    public static final String PROPERTY_MMS_NWP_ARC3_INPUT_PATTERN = "mms.nwp_arc3.pattern";
    public static final String PROPERTY_MMS_NWP_PATTERN = "mms.nwp.pattern";
    public static final String PROPERTY_MMS_NWP_SOURCEFILE = "mms.nwp.sourcefile";
    public static final String PROPERTY_MMS_NWP_SOURCEDIR = "mms.nwp.sourceDir";
    public static final String PROPERTY_MMS_NWP_TARGETFILE = "mms.nwp.targetfile";

    public static final String PROPERTY_MMS_ARC3_OUTPUT = "mms.arc3.targetfile";
    public static final String PROPERTY_MMS_ARC3_PATTERN = "mms.arc3.pattern";
    public static final String PROPERTY_MMS_ARC3_HOME = "mms.arc3.home";
    public static final String PROPERTY_MMS_ARC3_CONFIG_FILE = "mms.arc3.configFile";

    public static final String PROPERTY_TARGET_START_TIME = "mms.target.startTime";
    public static final String PROPERTY_TARGET_STOP_TIME = "mms.target.stopTime";

    private Constants() {
    }
}
