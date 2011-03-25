package org.esa.cci.sst;

/**
 * This exception is thrown by MMM tools.
 */
public class ToolException extends Exception {

    public static final int CONFIGURATION_FILE_NOT_FOUND_ERROR = 2;
    public static final int CONFIGURATION_FILE_IO_ERROR = 3;
    public static final int COMMAND_LINE_ARGUMENTS_PARSE_ERROR = 4;
    public static final int TOOL_CONFIGURATION_ERROR = 11;
    public static final int TOOL_ERROR = 21;

    private final int exitCode;

    public ToolException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public ToolException(String message, Throwable cause, int exitCode) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public final int getExitCode() {
        return exitCode;
    }
}
