package org.esa.cci.sst.tools;

/**
 * Error handler interface.
 */
public interface ErrorHandler {

    /**
     * Treats the occurrence of a {@link Throwable} as an error, which terminates
     * the executable.
     *
     * @param t        The throwable.
     * @param message  The error message.
     * @param exitCode The exit code.
     */
    public void handleError(Throwable t, String message, int exitCode);

    /**
     * Treats the occurrence of a {@link Throwable} as a warning, which must not
     * terminate the executable.
     *
     * @param t       The throwable.
     * @param message The warning message.
     */
    public void handleWarning(Throwable t, String message);
}
