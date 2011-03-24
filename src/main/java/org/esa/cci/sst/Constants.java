package org.esa.cci.sst;

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

    public static final String DIMENSION_NAME_MATCHUP = "match_up";

    public static final String DIMENSION_ROLE_LENGTH = "length";

    public static final String DIMENSION_ROLE_MATCHUP = "match_up";

    public static final int AATSR_MD_CS_LENGTH = 8;
    public static final int AATSR_MD_UI_LENGTH = 30;
    public static final int AATSR_MD_LENGTH = 65;
    public static final int METOP_LENGTH = 21;
    public static final int METOP_LEN_ID = 11;
    public static final int METOP_LEN_FILENAME = 65;
    public static final int SEVIRI_LENGTH = 5;
    public static final int SEVIRI_LEN_ID = 11;
    public static final int SEVIRI_LEN_FILENAME = 65;
    public static final int AATSR_LENGTH = 101;
    public static final int AVHRR_WIDTH = 25;
    public static final int AVHRR_HEIGHT = 31;
    public static final int AMSRE_LENGTH = 11;
    public static final int TMI_LENGTH = 11;
    public static final int AAI_LENGTH = 1;
    public static final int SEA_ICE_LENGTH = 15;

    private Constants() {
    }
}
