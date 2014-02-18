package org.esa.cci.sst.tools.samplepoint;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SamplingPointUtilTest {

    @Test
    public void testCreateOutputPath() {
        final String archiveRoot = "/archive";

        // # mms/archive/mms2/smp/atsr.3/2003/atsr.3-smp-2003-01-b.json
        String path = SamplingPointUtil.createPath(archiveRoot, "atsr.2", 2008, 5, 'a');
        assertEquals("/archive/smp/atsr.2/2008/atsr.2-smp-2008-05-a.json", path);

        path = SamplingPointUtil.createPath(archiveRoot, "atsr.3", 2010, 11, 'b');
        assertEquals("/archive/smp/atsr.3/2010/atsr.3-smp-2010-11-b.json", path);
    }
}
