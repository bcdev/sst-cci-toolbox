package org.esa.cci.sst.tools.matchup;

class IO_Coincidence {

    private int id;
    private double timeDifference;
    private int observationId;
    private boolean isInsitu;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getTimeDifference() {
        return timeDifference;
    }

    public void setTimeDifference(double timeDifference) {
        this.timeDifference = timeDifference;
    }

    public int getObservationId() {
        return observationId;
    }

    public void setObservationId(int observationId) {
        this.observationId = observationId;
    }

    public boolean isInsitu() {
        return isInsitu;
    }

    public void setInsitu(boolean isInsitu) {
        this.isInsitu = isInsitu;
    }
}
