package org.esa.cci.sst.reader;

import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.PGgeometry;
import org.postgis.Point;

import java.io.IOException;
import java.util.Date;

import static org.esa.cci.sst.tools.SensorType.*;

/**
 * Reads records from an (A)ATSR MD NetCDF input file and creates Observations.
 * Defines the variables to access in the NetCDF files and implements the conversion
 * to a "reference observation" with a single point as coordinate. (A)ATSR MDs only
 * serve as reference observation. They never provide a coverage to serve as "common
 * observation" that matches a reference observation.
 *
 * @author Martin Boettcher
 */
class AtsrMdIOHandler extends MdIOHandler {

    AtsrMdIOHandler() {
        super(ATSR_MD.getSensor(), Constants.DIMENSION_NAME_MATCHUP);
    }

    @Override
    public String getSstVariableName() {
        return "atsr.sea_surface_temperature.dual";
    }

    /**
     * Reads record and creates ReferenceObservation for (A)ATSR pixel contained in MD. This observation
     * may serve as reference observation in some matchup.
     *
     * @param recordNo index in observation file, must be between 0 and less than numRecords
     *
     * @return Observation for (A)ATSR pixel
     *
     * @throws IOException if record number is out of range 0 .. numRecords-1 or if file io fails
     */
    @Override
    public ReferenceObservation readObservation(int recordNo) throws IOException {
        final PGgeometry location = new PGgeometry(new Point(getFloat("atsr.longitude", recordNo),
                                                             getFloat("atsr.latitude", recordNo)));
        final ReferenceObservation observation = new ReferenceObservation();
        observation.setName(getString("insitu.callsign", recordNo));
        observation.setSensor(ATSR_MD.getSensor());
        observation.setPoint(location);
        observation.setLocation(location);
        observation.setTime(dateOf(getDouble("atsr.time.julian", recordNo)));
        observation.setDatafile(getDataFile());
        observation.setRecordNo(recordNo);
        try {
            observation.setClassification(getByte("insitu.reference_flag", recordNo));
        } catch (IOException ignore) {
            // ignore, there is no insitu.reference_flag
        }
        observation.setClearSky(getShort("atsr.sea_surface_temperature.dual", recordNo) != getSstFillValue());
        return observation;
    }

    @Override
    public InsituRecord readInsituRecord(int recordNo) throws IOException {
        final InsituRecord insituRecord = new InsituRecord();
        final double julianDate = getDouble("insitu.time.julian", recordNo);
        final double time = TimeUtil.julianDateToSecondsSinceEpoch(julianDate);
        insituRecord.setValue(InsituVariable.TIME, time);
        insituRecord.setValue(InsituVariable.LAT, getFloat("insitu.latitude", recordNo));
        insituRecord.setValue(InsituVariable.LAT, getFloat("insitu.longitude", recordNo));
        final Number sst = getNumberScaled("insitu.sea_surface_temperature", recordNo);
        insituRecord.setValue(InsituVariable.SST, sst.floatValue() + 273.15f);

        return insituRecord;
    }

    private static Date dateOf(double julianDate) {
        return TimeUtil.julianDateToDate(julianDate);
    }
}
