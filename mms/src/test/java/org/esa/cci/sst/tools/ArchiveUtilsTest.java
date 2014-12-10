package org.esa.cci.sst.tools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArchiveUtilsTest {

    @Test
    public void testCreateCleanFilePath() {
        assertEquals("/archive/root/clean/avhrr_orb.n10,history/2008/avhrr_orb.n10,history-clean-2008-08.json",
                ArchiveUtils.createCleanFilePath("/archive/root", new String[]{"avhrr_orb.n10", "history"}, 2008, 8));


        assertEquals("/archive/root/clean/orb_avhrr.n18/1995/orb_avhrr.n18-clean-1995-11.json",
                ArchiveUtils.createCleanFilePath("/archive/root", new String[]{"orb_avhrr.n18"}, 1995, 11));

        assertEquals("/archive/root/clean/amsre,avhrr.m01,history/2006/amsre,avhrr.m01,history-clean-2006-02.json",
                ArchiveUtils.createCleanFilePath("/archive/root", new String[]{"amsre", "avhrr.m01", "history"}, 2006, 2));
    }

    @Test
    public void testCreateCleanEnvFilePath() {
        assertEquals("/archive/root/clean-env/avhrr_orb.n10,history/2008/avhrr_orb.n10,history-clean-env-2008-08.json",
                ArchiveUtils.createCleanEnvFilePath("/archive/root", new String[]{"avhrr_orb.n10", "history"}, 2008, 8));


        assertEquals("/archive/root/clean-env/orb_avhrr.n18/1995/orb_avhrr.n18-clean-env-1995-11.json",
                ArchiveUtils.createCleanEnvFilePath("/archive/root", new String[]{"orb_avhrr.n18"}, 1995, 11));

        assertEquals("/archive/root/clean-env/amsre,avhrr.m01,history/2006/amsre,avhrr.m01,history-clean-env-2006-02.json",
                ArchiveUtils.createCleanEnvFilePath("/archive/root", new String[]{"amsre", "avhrr.m01", "history"}, 2006, 2));
    }
}
