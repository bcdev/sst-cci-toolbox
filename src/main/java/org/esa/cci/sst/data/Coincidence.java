package org.esa.cci.sst.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
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
    float distance;
    double timeDifference;

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

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public double getTimeDifference() {
        return timeDifference;
    }

    public void setTimeDifference(double timeDifference) {
        this.timeDifference = timeDifference;
    }
}
