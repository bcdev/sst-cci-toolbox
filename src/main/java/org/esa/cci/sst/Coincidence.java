package org.esa.cci.sst;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * TODO add API doc
 *
 * @author Martin Boettcher
 */
@Entity
@Table(name="mm_coincidence")
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

    @ManyToOne
    public Observation getRefObs() {
        return refObs;
    }

    public void setRefObs(Observation refObs) {
        this.refObs = refObs;
    }

    @ManyToOne
    public Observation getObservation() {
        return observation;
    }

    public void setObservation(Observation observation) {
        this.observation = observation;
    }
}
