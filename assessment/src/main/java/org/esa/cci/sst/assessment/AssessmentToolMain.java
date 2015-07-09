package org.esa.cci.sst.assessment;


import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.IOException;

public class AssessmentToolMain {

    public static void main(String[] args) throws IOException, InvalidFormatException {
        final AssessmentTool assessmentTool = new AssessmentTool();

        assessmentTool.run(args);
    }
}
