/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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

    private int id;
    private ReferenceObservation refObs;
    private List<Coincidence> coincidences;
    private long pattern;
    private boolean invalid;

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    // important: this is not public API
    @Deprecated
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

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }
}
