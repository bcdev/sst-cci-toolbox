/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.reader;

import org.esa.cci.sst.tools.MmsTool;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Factory providing a static method for getting the correct io handler, according to given schema name.
 *
 * @author Thomas Storm
 */
public class IOHandlerFactory {

    private IOHandlerFactory() {
    }

    /**
     * Factory method for getting the correct io handler, according to given schema and sensor names.
     *
     * @param readerSpec The reader specification, in the form <code>Reader1,Reader2</code>,
     *                   where <code>Reader2</code> is constructor argument for <code>Reader1</code>.
     * @param sensorName The sensor name.
     *
     * @return a new instance of <code>IOHandler</code>.
     */
    public static IOHandler createHandler(String readerSpec, String sensorName) throws ClassNotFoundException,
                                                                                       NoSuchMethodException,
                                                                                       InvocationTargetException,
                                                                                       IllegalAccessException,
                                                                                       InstantiationException {
        final String packageName = IOHandlerFactory.class.getPackage().getName();
        final String[] handlerClassNames = readerSpec.split(",");
        IOHandler handler = null;
        for (int i = handlerClassNames.length - 1; i >= 0; i--) {
            final Class<?> handlerClass = Class.forName(packageName + '.' + handlerClassNames[i]);
            if (handler == null) {
                final Constructor<?> constructor = handlerClass.getDeclaredConstructor(String.class);
                handler = (IOHandler) constructor.newInstance(sensorName);
            } else {
                final Constructor<?> constructor = handlerClass.getDeclaredConstructor(IOHandler.class);
                handler = (IOHandler) constructor.newInstance(handler);
            }
        }

        return handler;
    }

    public static IOHandler createMmdIOHandler(final MmsTool tool, String sensorName) {
        return new MmdIOHandler(tool, sensorName);
    }

}
