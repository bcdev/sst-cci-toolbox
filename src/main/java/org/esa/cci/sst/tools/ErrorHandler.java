package org.esa.cci.sst.tools;

/**
 * Error handler interface.
 */
public interface ErrorHandler {

    /**
     * Handles the occurrence of a {@link ToolException}, which eventually shall
     * always terminate the executable.
     *
     * @param e The tool exception.
     */
    public void terminate(ToolException e);

    /**
     * Treats the occurrence of a {@link Throwable} as a warning, which must not
     * terminate the executable.
     *
     * @param t       The throwable.
     * @param message The warning message.
     */
    public void warn(Throwable t, String message);
}
