package org.esa.cci.sst;

public class Constants {

    public static final String DATA_SCHEMA_NAME_AATSR_MD = "aatsr-md";  // AatsrMdObservation
    public static final String DATA_SCHEMA_NAME_METOP_MD = "metop-md";  // Subscene-md-observation
    public static final String DATA_SCHEMA_NAME_SEVIRI_MD = "seviri-md";  // subscene-md-observation

    public static final String DATA_SCHEMA_NAME_AAI = "aai";    // SinglePixelObservation
    public static final String DATA_SCHEMA_NAME_ATSR = "atsr";  // FullOrbitObservation
    public static final String DATA_SCHEMA_NAME_AMR = "amsre";  // FullOrbitObservation
    public static final String DATA_SCHEMA_NAME_AVHRR_GAC = "avhrr_gac";    // FullOrbitObservation
    public static final String DATA_SCHEMA_NAME_SEA_ICE = "seaice"; // FullOrbitObservation
    public static final String DATA_SCHEMA_NAME_TMI = "tmi"; // FullOrbitObservation
    public static final String DATA_SCHEMA_INSITU = "insitu";

    /**
     * Name of persistence unit in META-INF/persistence.xml
     */
    public static final String PERSISTENCE_UNIT_NAME = "matchupdb";

    private Constants() {
    }
}
