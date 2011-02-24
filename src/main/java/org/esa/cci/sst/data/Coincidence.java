package org.esa.cci.sst.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Data item that represents the coincidence between a matchup's reference
 * observation and other observations.
 *
 * @author Martin Boettcher
 */
@Entity
@Table(name="mm_coincidence")
public class Coincidence {
    int id;
    Matchup matchup;
    GlobalObservation observation;
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
    public Matchup getMatchup() {
        return matchup;
    }

    public void setMatchup(Matchup matchup) {
        this.matchup = matchup;
    }

    @ManyToOne
    public GlobalObservation getObservation() {
        return observation;
    }

    public void setObservation(GlobalObservation observation) {
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
