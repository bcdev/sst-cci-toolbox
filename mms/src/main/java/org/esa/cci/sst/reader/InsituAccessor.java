package org.esa.cci.sst.reader;


import org.esa.cci.sst.util.SamplingPoint;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Date;
import java.util.List;

interface InsituAccessor {

    void readHistoryTimes() throws IOException;

    Date getHistoryStart();

    Date getHistoryEnd();

    String getObservationName();

    double getStartLon() throws IOException;

    double getEndLon() throws IOException;

    double getStartLat() throws IOException;

    double getEndLat() throws IOException;

    // @ todo 1 tb/tb make time range configurable tb 2014-03-12
    Range find12HoursRange(Date date);

    List<Range> createSubsampling(Range range, int maxLength);

    List<SamplingPoint> readSamplingPoints() throws IOException;

    Variable getVariable(String role);

    void scaleTime(Array timeArray);
}
