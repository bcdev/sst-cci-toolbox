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

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Observation;
import ucar.ma2.Array;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

/**
 * A decorator for readers that deflates a gzip-compressed input file into
 * a temporary directory but otherwise forwards operations to the decorated
 * reader.
 * <p/>
 * Compressed input file are recognized due to the ".gz" file extension. The
 * decorator gracefully handles non-compressed files by simply delegating to
 * the decorated reader.
 *
 * @author Martin Boettcher
 */
class GunzipDecorator implements Reader {

    private final Reader delegate;
    private File tmpFile;

    GunzipDecorator(Reader delegate) {
        this.delegate = delegate;
    }

    /**
     * Maybe deflates gz files, initialises reader.
     *
     * @param dataFile The file to be read.
     *
     * @throws IOException if decompressing or opening the file with the decorated reader fails.
     */
    @Override
    public final void init(DataFile dataFile) throws IOException {
        if (dataFile.getPath().endsWith(".gz")) {
            // deflate product to tmp file in tmp dir
            tmpFile = tmpFileFor(dataFile.getPath(), true);
            decompress(new File(dataFile.getPath()), tmpFile);

            // temporarily read from tmp path
            final String origPath = dataFile.getPath();
            try {
                dataFile.setPath(tmpFile.getPath());
                delegate.init(dataFile);
            } finally {
                dataFile.setPath(origPath);
            }
        } else {
            tmpFile = null;
            delegate.init(dataFile);
        }
    }

    /**
     * Closes the product and deletes the tmp file.
     */
    @Override
    public final void close() {
        delegate.close();
        if (tmpFile != null && tmpFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tmpFile.delete();
        }
        tmpFile = null;
    }

    /**
     * Delegates to decorated reader.
     */
    @Override
    public final int getNumRecords() {
        return delegate.getNumRecords();
    }

    /**
     * Delegates to decorated reader.
     */
    @Override
    public final Observation readObservation(int recordNo) throws IOException {
        return delegate.readObservation(recordNo);
    }

    @Override
    public final Array read(String role, ExtractDefinition extractDefinition) throws IOException {
        return delegate.read(role, extractDefinition);
    }

    @Override
    public final Item getColumn(String role) {
        return delegate.getColumn(role);
    }

    /**
     * Delegates to decorated reader.
     */
    @Override
    public final Item[] getColumns() {
        return delegate.getColumns();
    }


    /**
     * Delegates to decorated reader.
     */
    @Override
    public final DataFile getDatafile() {
        return delegate.getDatafile();
    }

    @Override
    public GeoCoding getGeoCoding(int recordNo) throws IOException {
        return delegate.getGeoCoding(recordNo);
    }

    @Override
    public double getDTime(int recordNo, int scanLine) throws IOException {
        return delegate.getDTime(recordNo, scanLine);
    }

    @Override
    public long getTime(int recordNo, int scanLine) throws IOException {
        return delegate.getTime(recordNo, scanLine);
    }

    @Override
    public int getLineSkip() {
        return delegate.getLineSkip();
    }

    @Override
    public InsituSource getInsituSource() {
        return delegate.getInsituSource();
    }

    @Override
    public int getScanLineCount() {
        return delegate.getScanLineCount();
    }

    @Override
    public int getElementCount() {
        return delegate.getScanLineCount();
    }

    /**
     * Constructs File with suffix of original file without "dotgz" in tmp dir.
     * The tmp dir can be configured with property java.io.tmpdir.
     * Else, it is a system default.
     *
     * @param dataFilePath path of the .gz file
     * @param deleteOnExit
     *
     * @return File in tmp dir
     *
     * @throws IOException if the tmp file could not be created
     */
    private static File tmpFileFor(String dataFilePath, boolean deleteOnExit) throws IOException {
        // chop of path before filename and ".gz" suffix to determine filename
        final int slashPosition = dataFilePath.lastIndexOf(File.separator);
        final int dotGzPosition = dataFilePath.length() - ".gz".length();
        final String fileName = dataFilePath.substring(slashPosition + 1, dotGzPosition);
        // use filename without suffix as prefix and "." + suffix as suffix
        final int dotPosition = fileName.lastIndexOf('.');
        final String prefix = (dotPosition > -1) ? fileName.substring(0, dotPosition) : fileName;
        final String suffix = (dotPosition > -1) ? fileName.substring(dotPosition) : null;
        // create temporary file in tmp dir, either property java.io.tmpdir or system default
        final File tempFile = File.createTempFile(prefix, suffix);
        if (deleteOnExit) {
            tempFile.deleteOnExit();
        }
        return tempFile;
    }

    /**
     * Uncompresses a file in gzip format to a tmp file.
     *
     * @param gzipFile existing file in gzip format
     * @param tmpFile  new file for the uncompressed content
     *
     * @throws IOException if reading the input, decompression, or writing the output fails
     */
    private static void decompress(File gzipFile, File tmpFile) throws IOException {
        final byte[] buffer = new byte[8192];

        InputStream in = null;
        OutputStream out = null;
        try {
            in = new GZIPInputStream(new FileInputStream(gzipFile), 8192);
            out = new BufferedOutputStream(new FileOutputStream(tmpFile));
            int noOfBytesRead;
            while ((noOfBytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, noOfBytesRead);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
