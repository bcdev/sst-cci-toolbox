/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
