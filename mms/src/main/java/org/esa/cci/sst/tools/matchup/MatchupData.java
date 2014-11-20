package org.esa.cci.sst.tools.matchup;

import java.util.ArrayList;
import java.util.List;


class MatchupData {

    private List<IO_RefObs> referenceObservations;

    MatchupData() {
        referenceObservations = new ArrayList<>();
    }

    public List<IO_RefObs> getReferenceObservations() {
        return referenceObservations;
    }

    public void setReferenceObservations(List<IO_RefObs> referenceObservations) {
        this.referenceObservations = referenceObservations;
    }

    public void add(IO_RefObs io_refObs) {
        referenceObservations.add(io_refObs);
    }
}
