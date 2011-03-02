package org.esa.cci.sst;

/**
 * This exception is thrown by MMM tools.
 */
public class ToolException extends Exception {

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
