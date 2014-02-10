package org.esa.cci.sst.reader;


import ucar.ma2.Range;

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

    Range find12HoursRange(Date date);

    List<Range> createSubsampling(Range range, int maxLength);
}
