/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.rules;

import org.esa.cci.sst.util.TimeUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class DetectorTemperatureProviderTest {

    private MyDetectorTemperatureProvider provider;

    @Before
    public void setUp() throws Exception {
        provider = new MyDetectorTemperatureProvider("test_detector_temperature.dat");
    }

    @Test
    public void testReadMetaInfo() throws Exception {
        assertEquals(2 * 365 * 24 * 60 * 60 + 366 * 24 * 60 * 60 + 333849750, provider.startTime);
        assertEquals(300, provider.step);
        assertEquals(36, provider.temperatures.length);
    }

    @Test
    public void testReadTemperatures() throws Exception {
        assertEquals(257.02, provider.temperatures[0], 0.001);
        assertEquals(255.81, provider.temperatures[11], 0.001);
        assertEquals(256.18, provider.temperatures[23], 0.001);
        assertEquals(255.82, provider.temperatures[35], 0.001);
    }

    @Test
    public void testGetTemperature() throws Exception {
        long secondsSince1981 = 333849750;
        long milliSecondsSince1981 = secondsSince1981 * 1000;
        long milliSecondsSince1970 = milliSecondsSince1981 + TimeUtil.MILLIS_1981;
        final Date firstDate = new Date(milliSecondsSince1970);
        final Date secondDate = new Date(milliSecondsSince1970 + 300000);
        final Date thirdDate = new Date(milliSecondsSince1970 + 600000);
        final Date betweenThirdAndFourthDateLow = new Date(milliSecondsSince1970 + 749999);
        final Date betweenThirdAndFourthDateHigh = new Date(milliSecondsSince1970 + 750000);

        assertEquals(257.02, provider.getDetectorTemperature(firstDate), 0.001);
        assertEquals(256.95, provider.getDetectorTemperature(secondDate), 0.001);
        assertEquals(256.36, provider.getDetectorTemperature(thirdDate), 0.001);
        assertEquals(256.36, provider.getDetectorTemperature(betweenThirdAndFourthDateLow), 0.001);
        assertEquals(256.13, provider.getDetectorTemperature(betweenThirdAndFourthDateHigh), 0.001);
    }

    private static class MyDetectorTemperatureProvider extends DetectorTemperatureProvider {

        MyDetectorTemperatureProvider(String detectorTemperatureFile) {
            super(detectorTemperatureFile);
        }
    }
}
