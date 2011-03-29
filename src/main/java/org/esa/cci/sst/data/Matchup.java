package org.esa.cci.sst.data;

import javax.persistence.Entity;
import javax.persistence.FetchType;
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
@Table(name = "mm_matchup")
public class Matchup {

    int id;
    ReferenceObservation refObs;
    List<Coincidence> coincidences;
    long pattern;

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    public ReferenceObservation getRefObs() {
        return refObs;
    }

    public void setRefObs(ReferenceObservation refObs) {
        this.refObs = refObs;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "matchup")
    public List<Coincidence> getCoincidences() {
        return coincidences;
    }

    public void setCoincidences(List<Coincidence> coincidences) {
        this.coincidences = coincidences;
    }

    public long getPattern() {
        return pattern;
    }

    public void setPattern(long pattern) {
        this.pattern = pattern;
    }
}
