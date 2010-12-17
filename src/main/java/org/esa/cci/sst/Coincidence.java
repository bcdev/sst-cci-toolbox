package org.esa.cci.sst;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * TODO add API doc
 *
 * @author Martin Boettcher
 */
@Entity
public class Coincidence {
    int id;
    Observation refObs;
    Observation observation;

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Observation getRefObs() {
        return refObs;
    }

    public void setRefObs(Observation refObs) {
        this.refObs = refObs;
    }

    public Observation getObservation() {
        return observation;
    }

    public void setObservation(Observation observation) {
        this.observation = observation;
    }
}
