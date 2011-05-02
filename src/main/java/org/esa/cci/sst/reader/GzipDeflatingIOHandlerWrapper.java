package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Descriptor;
import org.esa.cci.sst.data.Observation;
import org.postgis.PGgeometry;
import ucar.nc2.NetcdfFileWriteable;

import javax.naming.OperationNotSupportedException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.zip.GZIPInputStream;

/**
 * GlobalProductIOHandler that temporarily deflates a gz file to a tmp dir for the reader.
 * Distinguishes gz files by their extension ".gz". Gracefully handles non-gz files
 * like the normal GlobalProductIOHandler.
 */
public class GzipDeflatingIOHandlerWrapper implements IOHandler {

    private final IOHandler delegate;
    private File tmpFile = null;

    public GzipDeflatingIOHandlerWrapper(IOHandler delegate) {
        this.delegate = delegate;
    }

    /**
     * Maybe deflates gz files, initialises reader
     *
     * @param dataFile the file to be ingested
     *
     * @throws IOException if temporary decompression or opening the file with the product reader fails
     */
    @Override
    public final void init(DataFile dataFile) throws IOException {
        if (dataFile.getPath().endsWith(".gz")) {
            // deflate product to tmp file in tmp dir
            tmpFile = tmpFileFor(dataFile.getPath());
            decompressToTmpFile(new File(dataFile.getPath()), tmpFile);

            // temporarily read from tmp path
            String origPath = dataFile.getPath();
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
     * Closes the product and deletes the tmp file
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
     * Delegates to implementation IOHandler
     */
    @Override
    public int getNumRecords() {
        return delegate.getNumRecords();
    }

    /**
     * Delegates to implementation IOHandler
     */
    @Override
    public Observation readObservation(int recordNo) throws IOException {
        return delegate.readObservation(recordNo);
    }

    /**
     * Delegates to implementation IOHandler
     */
    @Override
    public Descriptor[] getVariableDescriptors() throws IOException {
        return delegate.getVariableDescriptors();
    }

    /**
     * Delegates to implementation IOHandler
     */
    @Override
    public void write(NetcdfFileWriteable targetFile, Observation sourceObservation, String sourceVariableName,
                      String targetVariableName, int targetRecordNumber, PGgeometry refPoint, Date refTime) throws
                                                                                                            IOException {
        delegate.write(targetFile, sourceObservation, sourceVariableName, targetVariableName, targetRecordNumber,
                       refPoint, refTime);
    }

    @Override
    public InsituRecord readInsituRecord(int recordNo) throws IOException, OperationNotSupportedException {
        return delegate.readInsituRecord(recordNo);
    }

    @Override
    public DataFile getDataFile() {
        return delegate.getDataFile();
    }

    /**
     * Constructs File with suffix of original file without "dotgz" in tmp dir.
     * The tmp dir can be configured with property java.io.tmpdir.
     * Else, it is a system default.
     *
     * @param dataFilePath path of the .gz file
     *
     * @return File in tmp dir
     *
     * @throws IOException if the tmp file could not be created
     */
    private static File tmpFileFor(String dataFilePath) throws IOException {
        // chop of path before filename and ".gz" suffix to determine filename
        final int slashPosition = dataFilePath.lastIndexOf(File.separator);
        final int dotGzPosition = dataFilePath.length() - ".gz".length();
        String fileName = dataFilePath.substring(slashPosition + 1, dotGzPosition);
        // use filename without suffix as prefix and "." + suffix as suffix
        final int dotPosition = fileName.lastIndexOf('.');
        String prefix = (dotPosition > -1) ? fileName.substring(0, dotPosition) : fileName;
        String suffix = (dotPosition > -1) ? fileName.substring(dotPosition) : null;
        // create temporary file in tmp dir, either property java.io.tmpdir or system default
        return File.createTempFile(prefix, suffix);
    }

    /**
     * Uncompresses a file in gzip format to a tmp file.
     *
     * @param gzipFile existing file in gzip format
     * @param tmpFile  new file for the uncompressed content
     *
     * @throws IOException if reading the input, decompression, or writing the output fails
     */
    private static void decompressToTmpFile(File gzipFile, File tmpFile) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new GZIPInputStream(new FileInputStream(gzipFile), 8192);
            out = new BufferedOutputStream(new FileOutputStream(tmpFile));
            byte[] buffer = new byte[8192];
            int noOfBytesRead;
            while ((noOfBytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, noOfBytesRead);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}
