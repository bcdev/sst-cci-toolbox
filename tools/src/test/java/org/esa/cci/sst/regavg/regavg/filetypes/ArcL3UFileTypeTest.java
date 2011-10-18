package org.esa.cci.sst.regavg.regavg.filetypes;

import org.esa.cci.sst.regavg.FileType;
import org.esa.cci.sst.regavg.ProcessingLevel;
import org.esa.cci.sst.regavg.SstDepth;
import org.esa.cci.sst.regavg.filetypes.ArcL3UFileType;
import org.esa.cci.sst.util.UTC;
import org.junit.Test;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
* @author Norman Fomferra
*/
public class ArcL3UFileTypeTest {
    FileType fileType = ArcL3UFileType.INSTANCE;

    @Test
    public void testParseDate() throws Exception {
        DateFormat format = UTC.getDateFormat("yyyy-MM-dd");
        assertEquals(format.parse("2002-01-12"), fileType.parseDate(new File("AT2_AVG_3PAARC20020112_D_dN2b.nc.gz")));  // day/nadir
        assertEquals(format.parse("2002-04-16"), fileType.parseDate(new File("AT1_AVG_3PAARC20020416_D_dN2b.nc.gz")));  // day/dual
        assertEquals(format.parse("2002-01-20"), fileType.parseDate(new File("AT2_AVG_3PAARC20020120_D_nN2b.nc.gz")));  // night/nadir
        assertEquals(format.parse("2002-09-15"), fileType.parseDate(new File("ATS_AVG_3PAARC20020915_D_nD3b.nc.gz")));  // night/dual

        try {
            fileType.parseDate(new File("ATS_AVG_3PAARC_20020915_D_nD3b.nc.gz"));
            fail("ParseException expected.");
        } catch (ParseException e) {
            // ok
        }
    }

    @Test
    public void testOtherProperties() throws Exception {
        assertEquals(ProcessingLevel.L3U, fileType.getProcessingLevel());
        assertEquals("AT[12S]_AVG_3PAARC\\d{8}_[DTEM]_[nd][ND][23][bms][.]nc[.]gz", fileType.getFilenameRegex());
    }
}