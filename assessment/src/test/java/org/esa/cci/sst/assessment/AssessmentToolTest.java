package org.esa.cci.sst.assessment;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AssessmentToolTest {

    @Test
    public void testMakeWordVariable() {
        assertEquals("${yeah}", AssessmentTool.makeWordVariable("yeah"));
        assertEquals("${Oh.yeah}", AssessmentTool.makeWordVariable("Oh.yeah"));
    }

    @Test
    public void testCreateOptions() {
        final Options options = AssessmentTool.createOptions();
        assertNotNull(options);
        assertEquals(4, options.getOptions().size());

        final Option templateOption = options.getOption("t");
        assertNotNull(templateOption);
        assertEquals("template", templateOption.getLongOpt());
        assertTrue(templateOption.isRequired());
        assertTrue(templateOption.hasArg());
        assertEquals("The word template file-path to use", templateOption.getDescription());

        final Option propertiesOption = options.getOption("p");
        assertNotNull(propertiesOption);
        assertEquals("properties", propertiesOption.getLongOpt());
        assertTrue(propertiesOption.isRequired());
        assertTrue(propertiesOption.hasArg());
        assertEquals("The properties file-path containing the variables", propertiesOption.getDescription());

        final Option outputOption = options.getOption("o");
        assertNotNull(outputOption);
        assertEquals("output", outputOption.getLongOpt());
        assertTrue(outputOption.isRequired());
        assertTrue(outputOption.hasArg());
        assertEquals("The output file name/path", outputOption.getDescription());

        final Option replaceOption = options.getOption("r");
        assertNotNull(replaceOption);
        assertEquals("replace", replaceOption.getLongOpt());
        assertFalse(replaceOption.isRequired());
        assertFalse(replaceOption.hasArg());
        assertEquals("Replace output file if existing", replaceOption.getDescription());
    }
}
