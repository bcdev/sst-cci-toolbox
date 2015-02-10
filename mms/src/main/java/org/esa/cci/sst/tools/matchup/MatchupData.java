package org.esa.cci.sst.tools.matchup;

import org.esa.cci.sst.data.Sensor;

import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("UnusedDeclaration")
class MatchupData {

    private List<IO_RefObservation> reo;
    private List<IO_Observation> rlo;
    private List<IO_Observation> iso;
    private List<IO_Observation> glo;
    private List<Sensor> se;
    private List<IO_Matchup> mu;

    MatchupData() {
        reo = new ArrayList<>();
        rlo = new ArrayList<>();
        iso = new ArrayList<>();
        glo = new ArrayList<>();
        se = new ArrayList<>();
        mu = new ArrayList<>();
    }

    public List<IO_RefObservation> getReo() {
        return reo;
    }

    public void setReo(List<IO_RefObservation> ro) {
        this.reo = ro;
    }

    public void add(IO_RefObservation io_refObs) {
        reo.add(io_refObs);
    }

    public List<IO_Observation> getRlo() {
        return rlo;
    }

    public void addRelated(IO_Observation io_oObs) {
        rlo.add(io_oObs);
    }

    public List<IO_Observation> getIso() {
        return iso;
    }

    public void addInsitu(IO_Observation io_oObs) {
        iso.add(io_oObs);
    }

    public void addGlobal(IO_Observation io_oObs) {
        glo.add(io_oObs);
    }

    public List<Sensor> getSe() {
        return se;
    }

    public void add(Sensor sensor) {
        se.add(sensor);
    }

    public List<IO_Matchup> getMu() {
        return mu;
    }

    public void add(IO_Matchup matchup) {
        mu.add(matchup);
    }

    public List<IO_Observation> getGlo() {
        return glo;
    }
}
