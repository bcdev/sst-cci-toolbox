package org.esa.cci.sst.tools.matchup;


import java.util.Date;

class IO_Observation {
    private int id;
    private String na;
    private String se;
    private String fp;
    private int si;
    private Date ti;
    private double tr;
    private String lo;
    private int rn;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNa() {
        return na;
    }

    public void setNa(String name) {
        this.na = name;
    }

    public String getSe() {
        return se;
    }

    public void setSe(String sensor) {
        this.se = sensor;
    }

    public String getFp() {
        return fp;
    }

    public void setFp(String filePath) {
        this.fp = filePath;
    }

    public int getSi() {
        return si;
    }

    public void setSi(int sensorId) {
        this.si = sensorId;
    }

    public Date getTi() {
        return ti;
    }

    public void setTi(Date time) {
        this.ti = time;
    }

    public double getTr() {
        return tr;
    }

    public void setTr(double timeRadius) {
        this.tr = timeRadius;
    }

    public String getLo() {
        return lo;
    }

    public void setLo(String location) {
        this.lo = location;
    }

    public int getRn() {
        return rn;
    }

    public void setRn(int recordNo) {
        this.rn = recordNo;
    }
}
