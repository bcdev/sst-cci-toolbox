package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.ArrayInt;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * A reader for AMSR-E NetCDF files.
 *
 * @author Martin Boettcher
 * @author Norman Fomferra
 */
public class AmsreObservationReader implements ObservationReader {
    private DataFile dataFileEntry;
    private NetcdfFile netcdf;
    private int numRecords;


    @Override
    public void init(File observationFile, DataFile dataFileEntry) throws IOException {

        this.dataFileEntry = dataFileEntry;

         this.dataFileEntry = dataFileEntry;

        // open match-up file
        netcdf = NetcdfFile.open(observationFile.getPath());
        if (netcdf == null) {
            throw new IOException(MessageFormat.format("Can''t find NetCDF IOServiceProvider for file {0}", observationFile));
        }
        // read number of records value
        final Dimension dimension = netcdf.findDimension("time");
        if (dimension == null) {
            throw new IOException(MessageFormat.format("Can''t find dimension ''{0}'' in file {1}", "time", observationFile));
        }
        numRecords = dimension.getLength();
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public int getNumRecords() {
        return numRecords;
    }

    @Override
    public long getTime(int recordNo) throws IOException, InvalidRangeException {
        final Variable variable = netcdf.findVariable("time");
        final Array timeData = variable.read();
        final int secondsSince1981 = ((ArrayInt.D1) timeData).get(recordNo);
        return TimeUtil.MILLISECONDS_1981 + secondsSince1981 * 1000;
    }

    @Override
    public Observation readObservation(int recordNo) throws IOException, InvalidRangeException {
        final Observation observation = new Observation();
        observation.setDatafile(dataFileEntry);
        observation.setTime(new Date(getTime(0)));
        return observation;
    }

    @Override
    public Observation readRefObs(int recordNo) throws IOException, InvalidRangeException {
        return null;  // todo
    }
}
