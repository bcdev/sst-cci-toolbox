package org.esa.cci.sst.tools.matchup;

class IO_Coincidence {

    private int id;
    private double td;
    private int oi;
    private boolean is;
    private boolean gl;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getTd() {
        return td;
    }

    public void setTd(double timeDifference) {
        this.td = timeDifference;
    }

    public int getOi() {
        return oi;
    }

    public void setOi(int observationId) {
        this.oi = observationId;
    }

    public boolean isIs() {
        return is;
    }

    public void setIs(boolean insitu) {
        this.is = insitu;
    }

    public void setGl(boolean global) {
        this.gl = global;
    }

    public boolean isGl() {
        return gl;
    }
}
