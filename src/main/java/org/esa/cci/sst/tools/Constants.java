package org.esa.cci.sst.tools;

public class Constants {

    public static final String DATA_SCHEMA_NAME_ATSR_MD = "atsr_md";
    public static final String DATA_SCHEMA_NAME_METOP_MD = "metop_md";
    public static final String DATA_SCHEMA_NAME_SEVIRI_MD = "seviri_md";

    public static final String DATA_SCHEMA_NAME_AAI = "aai";
    public static final String DATA_SCHEMA_NAME_ATSR_L1B = "atsr_l1b";
    public static final String DATA_SCHEMA_NAME_AMSRE = "amsre";
    public static final String DATA_SCHEMA_NAME_AVHRR_GAC = "avhrr_gac";
    public static final String DATA_SCHEMA_NAME_SEAICE = "seaice";
    public static final String DATA_SCHEMA_NAME_TMI = "tmi";
    public static final String DATA_SCHEMA_INSITU_HISTORY = "history";

    /**
     * Name of persistence unit in META-INF/persistence.xml
     */
    public static final String PERSISTENCE_UNIT_NAME = "matchupdb";

    public static final String DIMENSION_NAME_MATCHUP = "matchup";

    public static final String DIMENSION_ROLE_LENGTH = "length";

    public static final String DIMENSION_ROLE_MATCHUP = "matchup";

    public static final int ATSR_MD_CS_LENGTH = 8;
    public static final int ATSR_MD_UI_LENGTH = 30;
    public static final int ATSR_MD_LENGTH = 65;

    public static final int METOP_LENGTH = 21;
    public static final int METOP_LEN_ID = 11;
    public static final int METOP_LEN_FILENAME = 65;

    public static final int SEVIRI_LENGTH = 5;
    public static final int SEVIRI_LEN_ID = 11;
    public static final int SEVIRI_LEN_FILENAME = 65;

    public static final int ATSR_SUBSCENE_HEIGHT = 101;
    public static final int ATSR_SUBSCENE_WIDTH = 101;

    public static final int AVHRR_SUBSCENE_HEIGHT = 31;
    public static final int AVHRR_SUBSCENE_WIDTH = 25;

    public static final int AMSRE_SUBSCENE_HEIGHT = 11;
    public static final int AMSRE_SUBSCENE_WIDTH = 11;

    public static final int TMI_SUBSCENE_HEIGHT = 11;
    public static final int TMI_SUBSCENE_WIDTH = 11;

    public static final int AAI_SUBSCENE_HEIGHT = 1;
    public static final int AAI_SUBSCENE_WIDTH = 1;

    public static final int SEA_ICE_SUBSCENE_HEIGHT = 15;
    public static final int SEA_ICE_SUBSCENE_WIDTH = 15;

    public static final int INSITU_HISTORY_LENGTH = 24;
    public static final int INSITU_HISTORY_QC_LENGTH = 2;

    public static final String VARIABLE_NAME_TIME = "matchup.time";
    public static final String VARIABLE_NAME_LON = "matchup.longitude";
    public static final String VARIABLE_NAME_LAT = "matchup.latitude";
    public static final String VARIABLE_NAME_MATCHUP_ID = "matchup.id";
    public static final String VARIABLE_NAME_MATCHUP_ID_ALTERNATIVE = "matchup_id";

    public static final String LOCATIONFILE_PROPERTY = "mms.arcprocessing.locationfile";
    public static final String LATLONFILE_PROPERTY = "mms.arcprocessing.latlonfile";
    public static final String PIXEL_POS_SUFFIX = "_pixelPos";
    public static final String DATA_SCHEMA_NAME_ATSR_SUB = "atsr_sub";

    private Constants() {
    }
}
