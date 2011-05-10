/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.Constants;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * A test runner used for unit tests that require JPA persistence.
 *
 * @author Ralf Quast
 */
public class QueriesTestRunner extends BlockJUnit4ClassRunner {

    private boolean canPersist;

    public QueriesTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
        checkPersistenceIsAvailable(klass);
    }


    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        if (canPersist) {
            super.runChild(method, notifier);
        } else {
            notifier.fireTestIgnored(describeChild(method));
        }
    }

    private void checkPersistenceIsAvailable(Class<?> klass) {
        try {
            final Properties configuration = new Properties();
            configuration.load(new FileInputStream("mms-config.properties"));
            @SuppressWarnings({"UnusedDeclaration"})
            final PersistenceManager pm = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME, configuration);
            canPersist = true;
        } catch (Throwable t) {
            System.out.println("Test" + klass.getName() + " suppressed - JPA persistence not available.");
        }
    }
}
