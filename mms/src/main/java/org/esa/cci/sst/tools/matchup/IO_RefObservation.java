package org.esa.cci.sst.tools.matchup;


class IO_RefObservation extends IO_Observation {

    private String pt;
    private byte ds;
    private byte rf;

    public String getPt() {
        return pt;
    }

    public void setPt(String point) {
        this.pt = point;
    }

    public byte getDs() {
        return ds;
    }

    public void setDs(byte dataset) {
        this.ds = dataset;
    }

    public byte getRf() {
        return rf;
    }

    public void setRf(byte referenceFlag) {
        this.rf = referenceFlag;
    }
}
