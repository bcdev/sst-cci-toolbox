package org.esa.cci.sst.tools.samplepoint;

import java.util.logging.Logger;

class TestLogger extends Logger {

    private String warning;

    TestLogger() {
        super("we", null);
    }

    @Override
    public void warning(String msg) {
        warning = msg;
    }

    String getWarning() {
        return warning;
    }
}
