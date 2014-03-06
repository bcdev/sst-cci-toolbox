package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.Configuration;

import java.util.logging.Logger;

public class WorkflowContext {

    private long startTime;
    private long stopTime;
    private int halfRevisitTime;
    private Logger logger;
    private Configuration config;
    private PersistenceManager persistenceManager;
    private String sensorName;
    private int sampleCount;
    private int sampleSkip;

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }

    public long getStopTime() {
        return stopTime;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public Configuration getConfig() {
        return config;
    }

    public void setHalfRevisitTime(int halfRevisitTime) {
        this.halfRevisitTime = halfRevisitTime;
    }

    public int getHalfRevisitTime() {
        return halfRevisitTime;
    }

    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    public void setPersistenceManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    public String getSensorName() {
        return sensorName;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleSkip(int sampleSkip) {
        this.sampleSkip = sampleSkip;
    }

    public int getSampleSkip() {
        return sampleSkip;
    }
}
