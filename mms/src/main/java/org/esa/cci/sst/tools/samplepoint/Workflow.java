package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.SamplingPoint;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public abstract class Workflow {
    protected final Logger logger;

    public Workflow(WorkflowContext context) {
        logger = context.getLogger();
    }

    public abstract void execute(List<SamplingPoint> samplingPoints) throws IOException;

    protected void logInfo(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }
}
