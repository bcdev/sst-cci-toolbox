package org.esa.cci.sst;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class IoTestRunner extends BlockJUnit4ClassRunner {

    private static final String PROPERTYNAME_EXECUTE_IO_TESTS = "org.esa.cci.io.tests.execute";

    private final Class<?> clazz;
    private final boolean executeIoTests;

    public IoTestRunner(Class<?> klass) throws InitializationError {
        super(klass);

        this.clazz = klass;

        executeIoTests = Boolean.getBoolean(PROPERTYNAME_EXECUTE_IO_TESTS);
    }

    @Override
    public Description getDescription() {
        return Description.createSuiteDescription("SST-CCI IO Test Runner");
    }

    @Override
    public void run(RunNotifier runNotifier) {
        if (executeIoTests) {
            System.out.println("Executing IOTests in class " + clazz);
            super.run(runNotifier);
        } else {
            final Description description = Description.createTestDescription(clazz, "allMethods. SST-CCI IO tests disabled. " +
                    "Set VM param -D" + PROPERTYNAME_EXECUTE_IO_TESTS + "=true to enable.");
            runNotifier.fireTestIgnored(description);
        }
    }
}
