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
    /**
     * Name of the matchup dimension in an MMD file.
     */
    public static final String DIMENSION_NAME_MATCHUP = "matchup";

    @Deprecated
    public static final int ATSR_MD_CS_LENGTH = 8;
    @Deprecated
    public static final int ATSR_MD_UI_LENGTH = 30;
    @Deprecated
    public static final int ATSR_MD_LENGTH = 65;

    @Deprecated
    public static final int METOP_LENGTH = 21;
    @Deprecated
    public static final int METOP_LEN_ID = 11;
    @Deprecated
    public static final int METOP_LEN_FILENAME = 65;

    @Deprecated
    public static final int SEVIRI_LENGTH = 5;
    @Deprecated
    public static final int SEVIRI_LEN_ID = 11;
    @Deprecated
    public static final int SEVIRI_LEN_FILENAME = 65;

    @Deprecated
    public static final int ATSR_SUBSCENE_HEIGHT = 101;
    @Deprecated
    public static final int ATSR_SUBSCENE_WIDTH = 101;

    @Deprecated
    public static final int AVHRR_SUBSCENE_HEIGHT = 31;
    @Deprecated
    public static final int AVHRR_SUBSCENE_WIDTH = 25;

    @Deprecated
    public static final int AMSRE_SUBSCENE_HEIGHT = 11;
    @Deprecated
    public static final int AMSRE_SUBSCENE_WIDTH = 11;

    @Deprecated
    public static final int TMI_SUBSCENE_HEIGHT = 11;
    @Deprecated
    public static final int TMI_SUBSCENE_WIDTH = 11;

    @Deprecated
    public static final int AAI_SUBSCENE_HEIGHT = 1;
    @Deprecated
    public static final int AAI_SUBSCENE_WIDTH = 1;

    @Deprecated
    public static final int SEA_ICE_SUBSCENE_HEIGHT = 15;
    @Deprecated
    public static final int SEA_ICE_SUBSCENE_WIDTH = 15;

    @Deprecated
    public static final int INSITU_HISTORY_LENGTH = 24;
    @Deprecated
    public static final int INSITU_HISTORY_QC_LENGTH = 2;

    public static final String COLUMN_NAME_MATCHUP_ID = "matchup.id";
    public static final String COLUMN_NAME_MATCHUP_TIME = "matchup.time";
    public static final String COLUMN_NAME_MATCHUP_LON = "matchup.longitude";
    public static final String COLUMN_NAME_MATCHUP_LAT = "matchup.latitude";

    public static final String COLUMN_NAME_MATCHUP_INSITU_CALLSIGN = "matchup.insitu.callsign";
    public static final String COLUMN_NAME_MATCHUP_INSITU_DATASET = "matchup.insitu.dataset";
    public static final String COLUMN_NAME_MATCHUP_REFERENCE_FLAG = "matchup.reference_flag";
    public static final String COLUMN_NAME_MATCHUP_VALID = "matchup.valid";
    public static final String COLUMN_NAME_MATCHUP_PRIMARY_SENSOR = "matchup.primary_sensor";
    public static final String COLUMN_NAME_MATCHUP_PRIMARY_FILENAME = "matchup.primary_filename";
    public static final String COLUMN_NAME_MATCHUP_SENSOR_LIST = "matchup.sensor_list";

    public static final String COLUMN_NAME_MATCHUP_NWP_SEA_ICE_CONCENTRATION = "matchup.nwp.sea_ice_concentration";
    public static final String COLUMN_NAME_MATCHUP_NWP_SST = "matchup.nwp.sea_surface_temperature";
    public static final String COLUMN_NAME_MATCHUP_NWP_SURFACE_SENSIBLE_HEAT_FLUX = "matchup.nwp.surface_sensible_heat_flux";
    public static final String COLUMN_NAME_MATCHUP_NWP_SURFACE_LATENT_HEAT_FLUX = "matchup.nwp.surface_latent_heat_flux";
    public static final String COLUMN_NAME_MATCHUP_NWP_MEAN_SEA_LEVEL_PRESSURE = "matchup.nwp.mean_sea_level_pressure";
    public static final String COLUMN_NAME_MATCHUP_NWP_BOUNDARY_LAYER_HEIGHT = "matchup.nwp.boundary_layer_height";
    public static final String COLUMN_NAME_MATCHUP_NWP_10M_EAST_WIND_COMPONENT = "matchup.nwp.10m_east_wind_component";
    public static final String COLUMN_NAME_MATCHUP_NWP_10M_NORTH_WIND_COMPONENT = "matchup.nwp.10m_north_wind_component";
    public static final String COLUMN_NAME_MATCHUP_NWP_2M_TEMPERATURE = "matchup.nwp.2m_temperature";
    public static final String COLUMN_NAME_MATCHUP_NWP_2M_DEW_POINT = "matchup.nwp.2m_dew_point";
    public static final String COLUMN_NAME_MATCHUP_NWP_DOWNWARD_SURFACE_SOLAR_RADIATION = "matchup.nwp.downward_surface_solar_radiation";
    public static final String COLUMN_NAME_MATCHUP_NWP_DOWNWARD_SURFACE_THERMAL_RADIATION = "matchup.nwp.downward_surface_thermal_radiation";
    public static final String COLUMN_NAME_MATCHUP_NWP_SURFACE_SOLAR_RADIATION = "matchup.nwp.surface_solar_radiation";
    public static final String COLUMN_NAME_MATCHUP_NWP_SURFACE_THERMAL_RADIATION = "matchup.nwp.surface_thermal_radiation";
    public static final String COLUMN_NAME_MATCHUP_NWP_TURBULENT_STRESS_EAST = "matchup.nwp.turbulent_stress_east";
    public static final String COLUMN_NAME_MATCHUP_NWP_TURBULENT_STRESS_NORTH = "matchup.nwp.turbulent_stress_north";
    public static final String COLUMN_NAME_MATCHUP_NWP_EVAPORATION = "matchup.nwp.evaporation";
    public static final String COLUMN_NAME_MATCHUP_NWP_TOTAL_PRECIPITATION = "matchup.nwp.total_precipitation";

    /**
     * @deprecated replaced by #COLUMN_NAME_MATCHUP_TIME.
     */
    @Deprecated
    public static final String VARIABLE_OBSERVATION_TIME = "observation_time";

    public static final String VARIABLE_NAME_MATCHUP_ID_ALTERNATIVE = "matchup_id";

    public static final String LOCATIONFILE_PROPERTY = "mms.pixelpos.locationfile";
    public static final String LATLONFILE_PROPERTY = "mms.pixelpos.latlonfile";

    public static final String PROPERTY_SOURCE_START_TIME = "mms.source.startTime";
    public static final String PROPERTY_OUTPUT_START_TIME = "mms.output.startTime";
    public static final String PROPERTY_SOURCE_END_TIME = "mms.source.endTime";
    public static final String PROPERTY_OUTPUT_END_TIME = "mms.output.endTime";
    public static final String PROPERTY_OUTPUT_TMPDIR = "mms.output.tmpdir";
    public static final String PROPERTY_OUTPUT_DESTDIR = "mms.output.destdir";
    public static final String PROPERTY_MMS_REINGESTION_FILENAME = "mms.reingestion.filename";
    public static final String PROPERTY_MMS_REINGESTION_LOCATED = "mms.reingestion.located";
    public static final String PROPERTY_MMS_ARC3_EXECUTABLE = "mms.arc3.executable";
    public static final String PROPERTY_MMS_ARC3_SOURCEFILE = "mms.arc3.sourcefile";
    public static final String PROPERTY_MMS_ARC3_CUT_SUBSCENES = "mms.arc3.cutSubscenes";
    public static final String PROPERTY_MMS_ARC3_TARGETFILE = "mms.arc3.targetfile";
    public static final String PROPERTY_MMS_ARC3_NWPFILE = "mms.arc3.nwpfile";
    public static final String PROPERTY_MMS_ARC3_PATTERN = "mms.arc3.pattern";

    private Constants() {
    }
}
