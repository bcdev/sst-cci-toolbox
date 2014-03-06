package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.tools.Configuration;

import java.util.logging.Logger;

public class WorkflowContext {

    private long startTime;
    private long stopTime;
    private Logger logger;
    private Configuration config;

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
}
