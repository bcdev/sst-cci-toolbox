package org.esa.cci.sst.tools.matchup;

import java.util.ArrayList;
import java.util.List;

class IO_Matchup {

    private long id;
    private int ri;
    private List<IO_Coincidence> ci;
    private long pa;
    private boolean iv;

    IO_Matchup() {
        ci = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getRi() {
        return ri;
    }

    public void setRi(int refObsId) {
        this.ri = refObsId;
    }

    public List<IO_Coincidence> getCi() {
        return ci;
    }

    public void setCi(List<IO_Coincidence> coincidences) {
        this.ci = coincidences;
    }

    public long getPa() {
        return pa;
    }

    public void setPa(long pattern) {
        this.pa = pattern;
    }

    public boolean isIv() {
        return iv;
    }

    public void setIv(boolean invalid) {
        this.iv = invalid;
    }

    public void add(IO_Coincidence coincidence) {
        ci.add(coincidence);
    }
}
