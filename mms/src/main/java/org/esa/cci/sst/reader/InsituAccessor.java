package org.esa.cci.sst.reader;


import ucar.ma2.Range;

import java.io.IOException;
import java.util.Date;
import java.util.List;

interface InsituAccessor {

    void readHistoryTimes() throws IOException;

    Date getHistoryStart();

    Date getHistoryEnd();

    Range find12HoursRange(Date date);

    List<Range> createSubsampling(Range range, int maxLength);
}
