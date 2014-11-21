package org.esa.cci.sst.tools.matchup;

import java.util.ArrayList;
import java.util.List;

class IO_Matchup {

    private int id;
    private int refObsId;
    private List<IO_Coincidence> coincidences;
    private long pattern;
    private boolean invalid;

    IO_Matchup() {
        coincidences = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRefObsId() {
        return refObsId;
    }

    public void setRefObsId(int refObsId) {
        this.refObsId = refObsId;
    }

    public List<IO_Coincidence> getCoincidences() {
        return coincidences;
    }

    public void setCoincidences(List<IO_Coincidence> coincidences) {
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

    public void add(IO_Coincidence coincidence) {
        coincidences.add(coincidence);
    }
}
