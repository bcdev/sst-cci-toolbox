package org.esa.cci.sst.tools.matchup;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class SensorMap {

    private static Map<String, Integer> sensorNameMap;

    /*
    IMPORTANT!!!

    If you need to add new sensors or rename things, please ALWAYS append the changes to the list.

    The name/id mapping implemented here is used heavily during processing - and things may break
    if the existing mapping is changed.

    tb 2014-11-26
     */

    static {
        sensorNameMap = new HashMap<>();
        sensorNameMap.put("aai", 1);
        sensorNameMap.put("seaice", 2);
        sensorNameMap.put("history", 3);
        sensorNameMap.put("atsr_md", 4);
        sensorNameMap.put("metop_md", 5);
        sensorNameMap.put("seviri_md", 6);
        sensorNameMap.put("orb_atsr.1", 7);
        sensorNameMap.put("orb_atsr.2", 8);
        sensorNameMap.put("orb_atsr.3", 9);
        sensorNameMap.put("atsr.1", 10);
        sensorNameMap.put("atsr.2", 11);
        sensorNameMap.put("atsr.3", 12);
        sensorNameMap.put("orb_avhrr.n05", 13);
        sensorNameMap.put("orb_avhrr.n06", 14);
        sensorNameMap.put("orb_avhrr.n07", 15);
        sensorNameMap.put("orb_avhrr.n08", 16);
        sensorNameMap.put("orb_avhrr.n09", 17);
        sensorNameMap.put("orb_avhrr.n10", 18);
        sensorNameMap.put("orb_avhrr.n11", 19);
        sensorNameMap.put("orb_avhrr.n12", 20);
        sensorNameMap.put("orb_avhrr.n13", 21);
        sensorNameMap.put("orb_avhrr.n14", 22);
        sensorNameMap.put("orb_avhrr.n15", 23);
        sensorNameMap.put("orb_avhrr.n16", 24);
        sensorNameMap.put("orb_avhrr.n17", 25);
        sensorNameMap.put("orb_avhrr.n18", 26);
        sensorNameMap.put("orb_avhrr.n19", 27);
        sensorNameMap.put("orb_avhrr.m02", 28);
        sensorNameMap.put("amsre", 29);
        sensorNameMap.put("tmi", 30);
        sensorNameMap.put("avhrr_md", 31);
        sensorNameMap.put("orb_amsr2", 32);
        sensorNameMap.put("orb_avhrr.m01", 33);
        sensorNameMap.put("avhrr.n05", 34);
        sensorNameMap.put("avhrr.n06", 35);
        sensorNameMap.put("avhrr.n07", 36);
        sensorNameMap.put("avhrr.n08", 37);
        sensorNameMap.put("avhrr.n09", 38);
        sensorNameMap.put("avhrr.n10", 39);
        sensorNameMap.put("avhrr.n11", 40);
        sensorNameMap.put("avhrr.n12", 41);
        sensorNameMap.put("avhrr.n13", 42);
        sensorNameMap.put("avhrr.n14", 43);
        sensorNameMap.put("avhrr.n15", 44);
        sensorNameMap.put("avhrr.n16", 45);
        sensorNameMap.put("avhrr.n17", 46);
        sensorNameMap.put("avhrr.n18", 47);
        sensorNameMap.put("avhrr.n19", 48);
        sensorNameMap.put("avhrr.m02", 49);
        sensorNameMap.put("amsr2", 50);
        sensorNameMap.put("avhrr.m01", 51);
        sensorNameMap.put("orb_avhrr_f.m02", 52);
        sensorNameMap.put("orb_avhrr_f.m01", 53);
        sensorNameMap.put("avhrr_f.m02", 54);
        sensorNameMap.put("avhrr_f.m01", 55);
        sensorNameMap.put("iasi.m02", 56);
        sensorNameMap.put("iasi.m01", 57);
        sensorNameMap.put("orb_seviri", 58);
        sensorNameMap.put("seviri", 59);
    }

    static int idForName(String sensorName) {
        final Integer sensorId = sensorNameMap.get(sensorName);
        if (sensorId == null) {
            throw new IllegalArgumentException("Unsupported sensor: " + sensorName);
        }
        return sensorId;
    }

    static String nameForId(int sensorId) {
        final Set<Map.Entry<String, Integer>> entries = sensorNameMap.entrySet();
        for (Map.Entry<String, Integer> entry : entries) {
            if (entry.getValue() == sensorId) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Unsupported sensorId: " + sensorId);
    }
}
