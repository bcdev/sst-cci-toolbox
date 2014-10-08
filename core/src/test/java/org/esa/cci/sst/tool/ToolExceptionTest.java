package org.esa.cci.sst.tool;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("ThrowableInstanceNeverThrown")
public class ToolExceptionTest {

    @Test
    public void testCreateWithCauseAndExitCode() {
        final String message = "we test this";
        final Throwable throwable = new Throwable(message);

        final ToolException toolException = new ToolException(throwable, ToolException.TOOL_ERROR);
        assertEquals(ToolException.TOOL_ERROR, toolException.getExitCode());
        final Throwable cause = toolException.getCause();
        assertNotNull(cause);
        assertEquals(message, cause.getMessage());
    }
}
