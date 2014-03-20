package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.Configuration;

import java.io.File;
import java.util.logging.Logger;

public class WorkflowContext {

    private long startTime;
    private long stopTime;
    private int searchTime;
    private Logger logger;
    private Configuration config;
    private PersistenceManager persistenceManager;
    private String sensorName;
    private int sampleCount;
    private int sampleSkip;
    private File archiveRootDir;
    private long inistuSensorPattern;
    private String insituSensorName;
    private String sampleGeneratorName;
    private String insituInputPath;

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

    public void setSearchtTime(int searchtTime) {
        this.searchTime = searchtTime;
    }

    public int getSearchTime() {
        return searchTime;
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

    public void setArchiveRootDir(File archiveRootDir) {
        this.archiveRootDir = archiveRootDir;
    }

    public File getArchiveRootDir() {
        return archiveRootDir;
    }

    public void setInsituSensorPattern(long sensorPattern) {
        this.inistuSensorPattern = sensorPattern;
    }

    public long getInsituSensorPattern() {
        return inistuSensorPattern;
    }

    public void setInsituSensorName(String insituSensorName) {
        this.insituSensorName = insituSensorName;
    }

    public String getInsituSensorName() {
        return insituSensorName;
    }

    public void setSampleGeneratorName(String sampleGeneratorName) {
        this.sampleGeneratorName = sampleGeneratorName;
    }

    public String getSampleGeneratorName() {
        return sampleGeneratorName;
    }

    public void setInsituInputPath(String insituInputPath) {
        this.insituInputPath = insituInputPath;
    }

    public String getInsituInputPath() {
        return insituInputPath;
    }
}
