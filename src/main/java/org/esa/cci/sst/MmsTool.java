package org.esa.cci.sst;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

public class MmsTool {

    private Properties configuration;
    private boolean verbose;
    private boolean debug;

    public MmsTool() {
        configuration = new Properties();
    }

    public Properties getConfiguration() {
        return configuration;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isVerbose() {
         return verbose;
     }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void addConfigurationProperties(Properties properties) {
        for (Map.Entry entry : properties.entrySet()) {
            configuration.put(entry.getKey(), entry.getValue());
        }
    }

    public void addConfigurationProperties(File configurationFile) throws ToolException {
        try {
            FileReader reader = new FileReader(configurationFile);
            try {
                Properties configuration = new Properties();
                configuration.load(reader);
                addConfigurationProperties(configuration);
            } finally {
                reader.close();
            }
        } catch (FileNotFoundException e) {
            throw new ToolException(MessageFormat.format("File not found {0}", configurationFile), 2, e);
        } catch (IOException e) {
            throw new ToolException(MessageFormat.format("Failed to read from {0}", configurationFile), 3, e);
        }
        printInfo(MessageFormat.format("Using configuration read from {0}", configurationFile));
    }

    protected void printInfo(String msg) {
        if (isVerbose()) {
            System.out.println(msg);
        }
    }

    public static class ToolException extends Exception {

        int exitCode;

        public ToolException(String message, int exitCode) {
            super(message);
            this.exitCode = exitCode;
        }

        public ToolException(String message, int exitCode, Throwable cause) {
            super(message, cause);
            this.exitCode = exitCode;
        }

        public int getExitCode() {
            return exitCode;
        }
    }
}
