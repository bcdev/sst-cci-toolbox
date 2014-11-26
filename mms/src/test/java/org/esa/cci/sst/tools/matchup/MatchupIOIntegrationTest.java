package org.esa.cci.sst.tools.matchup;

import org.esa.cci.sst.IoTestRunner;
import org.esa.cci.sst.TestHelper;
import org.esa.cci.sst.data.Matchup;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@RunWith(IoTestRunner.class)
public class MatchupIOIntegrationTest {

    @Test
    public void testReadFromFile() throws IOException {
        final String resourcePath = TestHelper.getResourcePath(MatchupIOIntegrationTest.class, "test_matchups.json");
        final FileInputStream inputStream = new FileInputStream(new File(resourcePath));

        final List<Matchup> matchups = MatchupIO.read(inputStream);
    }

    @Test
    public void testWriteToFile() {
        // @todo 2 tb/tb implement 2014-11-26
    }
}
