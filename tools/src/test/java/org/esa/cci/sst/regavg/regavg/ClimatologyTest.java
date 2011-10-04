package org.esa.cci.sst.regavg.regavg;

import org.esa.cci.sst.regavg.Climatology;
import org.esa.cci.sst.regavg.ProcessingLevel;
import org.esa.cci.sst.regavg.RegionalAverageTool;
import org.esa.cci.sst.util.UTC;
import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class ClimatologyTest {

    public static final DateFormat DATE_FORMAT = UTC.getDateFormat("yyyy-MM-dd HH:mm:ss");

    @Test
    public void testGetRange() throws Exception {
        Assert.assertArrayEquals(new int[]{1, 2, 0}, Climatology.getDayOfYearRange(DATE_FORMAT.parse("2002-01-01 00:00:00")));
        assertArrayEquals(new int[]{125, 126, 12 * 60 * 60 * 1000}, Climatology.getDayOfYearRange(DATE_FORMAT.parse("2002-05-05 12:00:00")));
        assertArrayEquals(new int[]{365, 1, 15 * 60 * 60 * 1000}, Climatology.getDayOfYearRange(DATE_FORMAT.parse("2010-12-31 15:00:00")));
        assertArrayEquals(new int[]{365, 1, 15 * 60 * 60 * 1000}, Climatology.getDayOfYearRange(DATE_FORMAT.parse("2004-12-31 15:00:00"))); // leap year
    }


}
