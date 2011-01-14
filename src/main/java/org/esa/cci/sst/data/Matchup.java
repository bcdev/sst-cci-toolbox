package org.esa.cci.sst.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

/**
 * Data item that represents a multi-sensor matchup with a reference
 * observation and coincidences with other observations.
 *
 * @author Martin Boettcher
 */
@Entity
@Table(name="mm_matchup")
public class Matchup {
    int id;
    Observation refObs;
    List<Coincidence> coincidences;

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

    @OneToMany(mappedBy = "matchup")
    public List<Coincidence> getCoincidences() {
        return coincidences;
    }

    public void setCoincidences(List<Coincidence> coincidences) {
        this.coincidences = coincidences;
    }
}
