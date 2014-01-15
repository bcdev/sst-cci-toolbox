package org.esa.cci.sst.util;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.core.Assert;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.reader.ReaderFactory;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ReaderCache {

    private final Cache<String, Reader> readerCache;
    private final Properties configuration;
    private final Logger logger;

    private Reader cachedReader;


    public ReaderCache(int capacity) {
        this(capacity, new Properties(), null);
    }

    public ReaderCache(int capacity, Properties configuration, Logger logger) {
        Assert.notNull(configuration, "configuration == null");

        this.readerCache = new Cache<>(capacity);
        this.configuration = configuration;
        this.logger = logger;
    }

    public Reader getReader(DataFile datafile, boolean useCache) throws IOException {
        final String path = datafile.getPath();
        if (readerCache.contains(path)) {
            return readerCache.get(path);
        } else if (cachedReader != null && cachedReader.getDatafile().getPath().equals(path)) {
            return cachedReader;
        } else {
            if (useCache) {
                final Reader reader;
                try {
                    if (logger != null && logger.isLoggable(Level.INFO)) {
                        final String message = MessageFormat.format("Opening input file ''{0}''.", path);
                        logger.info(message);
                    }
                    reader = ReaderFactory.open(datafile, configuration);
                } catch (Exception e) {
                    throw new IOException(MessageFormat.format("Unable to open file ''{0}''.", path), e);
                }
                final Reader removedReader = readerCache.add(path, reader);
                if (removedReader != null) {
                    removedReader.close();
                }
                return reader;
            } else {
                if (cachedReader != null) {
                    cachedReader.close();
                }
                if (logger != null && logger.isLoggable(Level.INFO)) {
                    final String message = MessageFormat.format("Opening input file ''{0}''.", path);
                    logger.info(message);
                }
                cachedReader = ReaderFactory.open(datafile, configuration);
                return cachedReader;
            }
        }
    }

    public void closeReader(DataFile datafile) {
        final String path = datafile.getPath();
        if (readerCache.contains(path)) {
            final Reader removedReader = readerCache.remove(path);
            if (removedReader != null) {
                removedReader.close();
            }
        } else if (cachedReader != null && cachedReader.getDatafile().getPath().equals(path)) {
            cachedReader.close();
            cachedReader = null;
        }
    }

    public void clear() {
        final Collection<Reader> removedReaders = readerCache.clear();
        for (final Reader reader : removedReaders) {
            reader.close();
        }
    }
}
