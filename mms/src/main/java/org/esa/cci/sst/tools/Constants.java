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
    public static final int PERSISTENCE_RETRY_COUNT = 7;

    public static final String DIMENSION_NAME_MATCHUP = "matchup";
    public static final String DIMENSION_NAME_CALLSIGN_LENGTH = "callsign_length";
    public static final String DIMENSION_NAME_FILENAME_LENGTH = "filename_length";
    public static final String DIMENSION_NAME_INSITU_NTIME = "insitu.ntime";

    public static final String UNIT_BRIGHTNESS_TEMPERATURE = "K";
    public static final String UNIT_LAT = "degrees_north";
    public static final String UNIT_LON = "degrees_east";
    public static final String UNIT_SEA_SURFACE_TEMPERATURE = "K";
    public static final String UNIT_DTIME = "milliseconds";
    public static final String UNIT_TIME = "seconds since 1978-01-01 00:00:00";
    public static final String UNIT_INSITU_DTIME = "seconds from matchup.time";

    public static final String VARIABLE_NAME_MATCHUP_ID = "matchup.id";
    public static final String VARIABLE_NAME_ARC2_MATCHUP_ID = "matchup_id";

    public static final String SENSOR_NAME_ATSR_MD = "atsr_md";
    public static final String SENSOR_NAME_AVHRR_MD = "avhrr_md";
    public static final String SENSOR_NAME_METOP_MD = "metop";
    public static final String SENSOR_NAME_SEVIRI_MD = "seviri";

    public static final String SENSOR_NAME_DUMMY = "dummy";
    public static final String SENSOR_NAME_HISTORY = "history";
    public static final String SENSOR_NAME_SEAICE = "seaice";

    public static final String SENSOR_NAME_ORB_ATSR_1 = "orb_atsr.1";
    public static final String SENSOR_NAME_ORB_ATSR_2 = "orb_atsr.2";
    public static final String SENSOR_NAME_ORB_ATSR_3 = "orb_atsr.3";
    public static final String SENSOR_NAME_ORB_AVHRR_N10 = "orb_avhrr.n10";
    public static final String SENSOR_NAME_ORB_AVHRR_N11 = "orb_avhrr.n11";
    public static final String SENSOR_NAME_ORB_AVHRR_N12 = "orb_avhrr.n12";
    public static final String SENSOR_NAME_ORB_AVHRR_N13 = "orb_avhrr.n13";
    public static final String SENSOR_NAME_ORB_AVHRR_N14 = "orb_avhrr.n14";
    public static final String SENSOR_NAME_ORB_AVHRR_N15 = "orb_avhrr.n15";
    public static final String SENSOR_NAME_ORB_AVHRR_N16 = "orb_avhrr.n16";
    public static final String SENSOR_NAME_ORB_AVHRR_N17 = "orb_avhrr.n17";
    public static final String SENSOR_NAME_ORB_AVHRR_N18 = "orb_avhrr.n18";
    public static final String SENSOR_NAME_ORB_AVHRR_N19 = "orb_avhrr.n19";
    public static final String SENSOR_NAME_ORB_AVHRR_M01 = "orb_avhrr.m01";
    public static final String SENSOR_NAME_ORB_AVHRR_M02 = "orb_avhrr.m02";
    public static final String SENSOR_NAME_ORB_AVHRR_FRAC_M01 = "orb.avhrr_f.m01";
    public static final String SENSOR_NAME_ORB_AVHRR_FRAC_M02 = "orb.avhrr_f.m02";
    public static final String SENSOR_NAME_ORB_AMSR2 = "orb_amsr2";

    public static final String MASK_NAME_MMS_DIRTY = "mms_dirty";
    public static final byte MATCHUP_REFERENCE_FLAG_INVALID = 0;
    public static final byte MATCHUP_REFERENCE_FLAG_ALGORITHM = 1;
    public static final byte MATCHUP_REFERENCE_FLAG_BLIND = 2;
    public static final byte MATCHUP_REFERENCE_FLAG_VALIDATION = 3;
    public static final byte MATCHUP_REFERENCE_FLAG_UNDEFINED = 4;

    public static final byte MATCHUP_REFERENCE_FLAG_DUPLICATE = 5;
    public static final String MATCHUP_DATASET_ID = "matchup.dataset_id";
    public static final String MATCHUP_ID = "matchup.id";
    public static final String MATCHUP_TIME = "matchup.time";
    public static final String MATCHUP_LONGITUDE = "matchup.longitude";
    public static final String MATCHUP_LATITUDE = "matchup.latitude";
    public static final String MATCHUP_INSITU_CALLSIGN = "matchup.insitu.callsign";
    public static final String MATCHUP_INSITU_DATASET = "matchup.insitu.dataset";
    public static final String MATCHUP_REFERENCE_FLAG = "matchup.reference_flag";

    private Constants() {
    }
}
