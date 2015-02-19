package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.ReferenceObservation;

import java.util.Comparator;
import java.util.Date;

class MatchupComparator implements Comparator<Matchup> {

    @Override
    public int compare(Matchup m1, Matchup m2) {
        final ReferenceObservation refObs_1 = m1.getRefObs();
        final ReferenceObservation refObs_2 = m2.getRefObs();
        final String path_1 = refObs_1.getDatafile().getPath();
        final String path_2 = refObs_2.getDatafile().getPath();

        int result = path_1.compareTo(path_2);
        if (result != 0) {
            return result;
        }

        final Date time_1 = refObs_1.getTime();
        final Date time_2 = refObs_2.getTime();
        result = time_1.compareTo(time_2);
        if (result != 0) {
            return result;
        }

        final int id_1 = refObs_1.getId();
        final int id_2 = refObs_2.getId();
        return Integer.compare(id_1, id_2);
    }
}
