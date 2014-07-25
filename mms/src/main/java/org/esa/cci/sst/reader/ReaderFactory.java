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

package org.esa.cci.sst.reader;

import com.bc.ceres.core.Assert;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.tools.Configuration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Factory providing a static method for getting the correct io reader, according to given schema name.
 *
 * @author Thomas Storm
 */
public class ReaderFactory {

    public static final String DEFAULT_READER_SPEC = "GunzipDecorator,ProductReader";
    public static final String DEFAULT_MASK_SPEC = "";

    private static final String READER_PACKAGE_NAME = ReaderFactory.class.getPackage().getName();


    private ReaderFactory() {
    }

    /**
     * Factory method for creating an {@link Reader}, which is initialized with the
     * data file supplied as argument.
     *
     * @param datafile      The data file.
     * @param configuration The tool configuration.
     * @return a new {@link Reader} instance, which is initialized with the data file
     * supplied as argument.
     * @throws IOException if the {@link Reader} could not be initialized.
     */
    public static Reader open(DataFile datafile, Configuration configuration) throws IOException {
        final String sensorName = datafile.getSensor().getName();
        final String readerSpec = configuration.getStringValue("mms.reader." + sensorName, DEFAULT_READER_SPEC);
        final String dirtyMaskExpression = configuration.getDirtyMaskExpression(sensorName);
        final String archiveRootPath = configuration.getStringValue(Configuration.KEY_MMS_ARCHIVE_ROOT, ".");
        final File archiveRoot = new File(archiveRootPath);

        final Reader reader = createReader(readerSpec, sensorName, dirtyMaskExpression);
        reader.open(datafile, archiveRoot);

        return reader;
    }

    /**
     * Factory method for getting the correct reader, according to given schema and sensor names.
     *
     * @param readerSpec The reader specification, in the form <code>Reader2,Reader1</code>,
     *                   where <code>Reader1</code> is constructor argument for <code>Reader2</code>.
     * @param sensorName The sensor name.
     * @return a new instance of <code>Reader</code>.
     * @throws IllegalArgumentException when the reader specification is incorrect.
     */
    @SuppressWarnings({"unchecked"})
    public static Reader createReader(String readerSpec, String sensorName) {
        Assert.argument(readerSpec != null, "readerSpec == null");
        Assert.argument(sensorName != null, "sensorName == null");
        return createReader(readerSpec, sensorName, null);
    }

    // package public for testing only
    static Reader createReader(String readerSpec, String sensorName, String dirtyMaskExpression) {
        Assert.argument(readerSpec != null, "readerSpec == null");
        Assert.argument(sensorName != null, "sensorName == null");
        final String[] readerClassNames = readerSpec.split(",");
        Reader reader = null;
        try {
            for (int i = readerClassNames.length - 1; i >= 0; i--) {
                final Class<? extends Reader> readerClass = (Class<? extends Reader>) Class.forName(READER_PACKAGE_NAME + '.' + readerClassNames[i]);
                if (reader == null) {
                    Constructor<? extends Reader> constructor;
                    try {
                        constructor = readerClass.getDeclaredConstructor(String.class, String.class);
                        reader = constructor.newInstance(sensorName, dirtyMaskExpression);
                    } catch (NoSuchMethodException e) {
                        constructor = readerClass.getDeclaredConstructor(String.class);
                        reader = constructor.newInstance(sensorName);
                    }
                } else {
                    final Constructor<? extends Reader> constructor = readerClass.getDeclaredConstructor(Reader.class);
                    reader = constructor.newInstance(reader);
                }
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        return reader;
    }
}
