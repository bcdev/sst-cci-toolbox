package org.esa.cci.sst;

public class Constants {

    public static final String DATA_SCHEMA_NAME_AATSR_MD = "aatsr-md";
    public static final String DATA_SCHEMA_NAME_METOP_MD = "metop-md";
    public static final String DATA_SCHEMA_NAME_SEVIRI_MD = "seviri-md";

    public static final String DATA_SCHEMA_NAME_AAI = "aai";
    public static final String DATA_SCHEMA_NAME_ATSR = "atsr";
    public static final String DATA_SCHEMA_NAME_AMR = "amsre";
    public static final String DATA_SCHEMA_NAME_AVHRR_GAC = "avhrr_gac";
    public static final String DATA_SCHEMA_NAME_SEA_ICE = "seaice";
    public static final String DATA_SCHEMA_NAME_TMI = "tmi";

    public static final String SENSOR_NAME_AATSR_MD = "aatsr-md";
    public static final String SENSOR_NAME_AATSR = "aatsr";
    public static final String SENSOR_NAME_METOP = "metop";
    public static final String SENSOR_NAME_SEVIRI = "seviri";
    public static final String SENSOR_NAME_AVHRR = "avhrr";
    public static final String SENSOR_NAME_AMSRE = "amsre";
    public static final String SENSOR_NAME_TMI = "tmi";
    public static final String SENSOR_NAME_AAI = "aai";
    public static final String SENSOR_NAME_SEA_ICE = "seaice";
    /**
     * Name of persistence unit in META-INF/persistence.xml
     */
    public static final String PERSISTENCE_UNIT_NAME = "matchupdb";

    public static final String[] SENSOR_NAMES = {
            SENSOR_NAME_AATSR,
            SENSOR_NAME_METOP,
            SENSOR_NAME_SEVIRI,
            SENSOR_NAME_AVHRR,
            SENSOR_NAME_AMSRE,
            SENSOR_NAME_TMI,
            SENSOR_NAME_SEA_ICE
    };

    private Constants() {
    }
}
