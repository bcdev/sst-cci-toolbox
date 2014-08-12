package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.Predicate;

import java.util.HashSet;
import java.util.Set;

/**
 * Predicate used for filtering target variables in MMD variable configuration files.
 *
 * @author Ralf Quast
 */
final class SensorPredicate implements Predicate {

    private final Set<String> prefixSet;

    public SensorPredicate(String[] sensorNames) {
        prefixSet = new HashSet<>(5);
        if (sensorNames.length > 0) {
            prefixSet.add("matchup.");
            prefixSet.add("aai.");
            prefixSet.add("seaice.");
            prefixSet.add("insitu.");
            for (final String sensorName : sensorNames) {
                prefixSet.add(sensorName + ".");
            }
        }
    }

    @Override
    public boolean test(String s) {
        if (prefixSet.isEmpty()) {
            return true;
        }
        for (final String prefix : prefixSet) {
            if (s.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
