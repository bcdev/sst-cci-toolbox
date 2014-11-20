package org.esa.cci.sst.tools.matchup;

import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("UnusedDeclaration")
class MatchupData {

    private List<IO_RefObservation> referenceObservations;
    private List<IO_Observation> relatedObservations;
    private List<IO_Observation> insituObservations;

    MatchupData() {
        referenceObservations = new ArrayList<>();
        relatedObservations = new ArrayList<>();
        insituObservations = new ArrayList<>();
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

    public void addRelated(IO_Observation io_oObs) {
        relatedObservations.add(io_oObs);
    }

    public List<IO_Observation> getInsituObservations() {
        return insituObservations;
    }

    public void setInsituObservations(List<IO_Observation> insituObservations) {
        this.insituObservations = insituObservations;
    }

    public void addInsitu(IO_Observation io_oObs) {
        insituObservations.add(io_oObs);
    }
}
