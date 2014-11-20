package org.esa.cci.sst.tools.matchup;

import java.util.ArrayList;
import java.util.List;


class MatchupData {

    private List<IO_RefObservation> referenceObservations;
    private List<IO_Observation> relatedObservations;

    MatchupData() {
        referenceObservations = new ArrayList<>();
        relatedObservations = new ArrayList<>();
    }

    public List<IO_RefObservation> getReferenceObservations() {
        return referenceObservations;
    }

    public void setReferenceObservations(List<IO_RefObservation> referenceObservations) {
        this.referenceObservations = referenceObservations;
    }

    public void add(IO_RefObservation io_refObs) {
        referenceObservations.add(io_refObs);
    }

    public List<IO_Observation> getRelatedObservations() {
        return relatedObservations;
    }

    public void setRelatedObservations(List<IO_Observation> relatedObservations) {
        this.relatedObservations = relatedObservations;
    }
}
