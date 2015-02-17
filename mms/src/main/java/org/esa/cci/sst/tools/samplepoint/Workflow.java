package org.esa.cci.sst.tools.samplepoint;

import org.esa.cci.sst.util.SamplingPoint;

import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public abstract class Workflow {

    protected final Logger logger;
    protected final WorkflowContext workflowContext;

    public Workflow(WorkflowContext context) {
        logger = context.getLogger();
        this.workflowContext = context;
    }

    public abstract void execute(List<SamplingPoint> samplingPoints) throws IOException;

    public abstract List<SamplingPoint> execute() throws IOException, ParseException;

    protected void logInfo(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }
}
