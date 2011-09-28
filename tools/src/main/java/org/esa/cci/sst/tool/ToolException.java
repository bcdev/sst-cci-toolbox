package org.esa.cci.sst.tool;

/**
 * An exception that will cause a tool to exit.
 *
 * @author Norman Fomferra
 */
public class ToolException extends Exception {
    private final ExitCode exitCode;

    public ToolException(ExitCode exitCode) {
        this.exitCode = exitCode;
    }

    public ToolException(Throwable cause, ExitCode exitCode) {
        super(cause);
        this.exitCode = exitCode;
    }

    public ToolException(String message, ExitCode exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public ToolException(String message, Throwable cause, ExitCode exitCode) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public ExitCode getExitCode() {
        return exitCode;
    }
}
