package org.esa.cci.sst.util;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class StopWatchTest {

    @Test
    public void testStartStopGetElapsedMillis() {
        final StopWatch stopWatch = new StopWatch();

        stopWatch.start();

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }

        stopWatch.stop();

        assertTrue(stopWatch.getElapsedMillis() > 40);
    }
}
