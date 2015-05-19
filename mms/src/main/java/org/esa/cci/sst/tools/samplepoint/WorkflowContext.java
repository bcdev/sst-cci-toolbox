package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tool.Configuration;

import java.io.File;
import java.util.logging.Logger;

public class WorkflowContext {

    private long startTime;
    private long stopTime;
    private int searchTimePast;
    private int searchTimeFuture;
    private Logger logger;
    private Configuration config;
    private PersistenceManager persistenceManager;
    private String sensorName1;
    private int sampleCount;
    private int sampleSkip;
    private File archiveRootDir;
    private long insituSensorPattern;
    private String insituSensorName;
    private String sampleGeneratorName;
    private String insituInputPath;
    private String sensorName2;
    private int searchTimePast2;
    private int searchTimeFuture2;
    private boolean landWanted;
    private boolean cloudsWanted;
    private boolean mizOnly;

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

    public void setSearchTimePast(int searchTime) {
        this.searchTimePast = searchTime;
    }

    public int getSearchTimePast() {
        return searchTimePast;
    }

    public void setSearchTimeFuture(int searchTimeFuture) {
        this.searchTimeFuture = searchTimeFuture;
    }

    public int getSearchTimeFuture() {
        return searchTimeFuture;
    }

    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    public void setPersistenceManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    public void setSensorNames(String sensorNames) {
        final String[] parts = sensorNames.split(",", 2);
        this.sensorName1 = parts[0];
        if (parts.length > 1) {
            this.sensorName2 = parts[1];
        } else {
            this.sensorName2 = null;
        }
    }

    public String getSensorName1() {
        return sensorName1;
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
        this.insituSensorPattern = sensorPattern;
    }

    public long getInsituSensorPattern() {
        return insituSensorPattern;
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

    public String getSensorName2() {
        return sensorName2;
    }

    public void setSearchTimePast2(int searchTimePast2) {
        this.searchTimePast2 = searchTimePast2;
    }

    public int getSearchTimePast2() {
        return searchTimePast2;
    }

    public void setSearchTimeFuture2(int searchTimeFuture2) {
        this.searchTimeFuture2 = searchTimeFuture2;
    }

    public int getSearchTimeFuture2() {
        return searchTimeFuture2;
    }

    public boolean isLandWanted() {
        return landWanted;
    }

    public void setLandWanted(boolean landWanted) {
        this.landWanted = landWanted;
    }

    public boolean isCloudsWanted() {
        return cloudsWanted;
    }

    public void setCloudsWanted(boolean cloudsWanted) {
        this.cloudsWanted = cloudsWanted;
    }

    public boolean isMizOnly() {
        return mizOnly;
    }

    public void setMizOnly(boolean mizOnly) {
        this.mizOnly = mizOnly;
    }
}
