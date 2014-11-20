package org.esa.cci.sst.tools.matchup;


class IO_RefObservation extends IO_Observation {

    private String point;
    private byte dataset;
    private byte referenceFlag;

    public String getPoint() {
        return point;
    }

    public void setPoint(String point) {
        this.point = point;
    }

    public byte getDataset() {
        return dataset;
    }

    public void setDataset(byte dataset) {
        this.dataset = dataset;
    }

    public byte getReferenceFlag() {
        return referenceFlag;
    }

    public void setReferenceFlag(byte referenceFlag) {
        this.referenceFlag = referenceFlag;
    }
}
