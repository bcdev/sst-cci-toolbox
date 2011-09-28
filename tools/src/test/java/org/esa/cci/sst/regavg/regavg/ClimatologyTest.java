package org.esa.cci.sst.regavg.regavg;

import org.esa.cci.sst.regavg.Climatology;
import org.esa.cci.sst.regavg.ProcessingLevel;
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

    @Test
    public void testOutputName() throws Exception {
        String filename = getOutputFilename("20000101", "20101231", "Global", ProcessingLevel.L3U, "SSTskin", "PS", "DM", "01.0");
        assertEquals("20000101-20101231-Global_average-ESACCI-L3U_GHRSST-SSTskin-PS-DM-v02.0-fv01.0.nc", filename);
    }

    /**
     * Generates a filename of the form
     * <code>
     * <i>startOfPeriod</i><b>-</b><i>endOfPeriod</i><b>-</b><i>regionName</i><b>_average-ESACCI-</b><i>processingLevel</i><b>_GHRSST-</b><i>sstType</i><b>-</b><i>productString</i><b>-</b><i>additionalSegregator</i><b>-v02.0-fv</b><i>fileVersion</i><b>.nc</b>
     * </code>
     *
     * @param startOfPeriod        Start of period = YYYYMMDD
     * @param endOfPeriod          End of period = YYYYMMDD
     * @param regionName           Region Name or Description
     * @param processingLevel      Processing Level = L3C, L3U or L4
     * @param sstType              SST Type (see Table 4)
     * @param productString        Product String (see Table 5 in PSD)
     * @param additionalSegregator Additional Segregator = LT or DM
     * @param fileVersion          File Version, e.g. 0.10
     * @return The filename.
     */
    public static String getOutputFilename(String startOfPeriod, String endOfPeriod, String regionName, ProcessingLevel processingLevel, String sstType, String productString, String additionalSegregator, String fileVersion) {
        return String.format("%s-%s-%s_average-ESACCI-%s_GHRSST-%s-%s-%s-v02.0-fv%s.nc",
                             startOfPeriod,
                             endOfPeriod,
                             regionName,
                             processingLevel,
                             sstType,
                             productString,
                             additionalSegregator,
                             fileVersion);
    }

}
