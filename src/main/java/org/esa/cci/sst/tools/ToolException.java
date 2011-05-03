package org.esa.cci.sst.tools;

/**
 * This runtime exception shall thrown by MMS tools when an error has
 * occurred from which a recovery is not possible.
 *
 * @author Ralf Quast
 */
public class ToolException extends RuntimeException {

    public static final int UNKNOWN_ERROR = 1;
    public static final int CONFIGURATION_FILE_NOT_FOUND_ERROR = 2;
    public static final int CONFIGURATION_FILE_IO_ERROR = 3;
    public static final int COMMAND_LINE_ARGUMENTS_PARSE_ERROR = 4;
    public static final int TOOL_CONFIGURATION_ERROR = 11;
    public static final int TOOL_ERROR = 21;

    private final int exitCode;

    /**
     * Constructs a ne instance of this class.
     *
     * @param message  The error message.
     * @param exitCode The exit code which shall be returned to the system.
     */
    public ToolException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    /**
     * Constructs a ne instance of this class.
     *
     * @param message  The error message.
     * @param cause    The cause of the error.
     * @param exitCode The exit code which shall be returned to the system.
     */
    public ToolException(String message, Throwable cause, int exitCode) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    /**
     * Returns the exit code, which shall be returned to the system.
     *
     * @return the exit code.
     */
    public final int getExitCode() {
        return exitCode;
    }
}
