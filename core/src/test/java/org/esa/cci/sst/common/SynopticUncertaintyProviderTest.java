/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.common;

import org.esa.cci.sst.aggregate.AggregationCell;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class SynopticUncertaintyProviderTest {

    @Test
    public void testDxy() throws Exception {
        final SynopticUncertaintyProvider separations = new SynopticUncertaintyProvider(SpatialResolution.DEGREE_5_00, null);

        assertEquals(188.815598, separations.dxy(0), 0.000001); // almost north pole
        assertEquals(286.830454, separations.dxy(18), 0.000001); // almost equator
        assertEquals(188.815598, separations.dxy(35), 0.000001); // almost south pole
        assertEquals(240.705816, separations.dxy(9), 0.000001);
        assertEquals(234.940236, separations.dxy(27), 0.000001);
    }

    @Test
    public void testDt_weekly5days() throws Exception {
        for (final SpatialResolution spatialResolution : SpatialResolution.values()) {
            final SynopticUncertaintyProvider separations = new SynopticUncertaintyProvider(spatialResolution,
                    TemporalResolution.weekly5d);

            switch (spatialResolution) {
                case DEGREE_0_05:
                case DEGREE_0_10:
                case DEGREE_0_15:
                case DEGREE_0_20:
                case DEGREE_0_25:
                case DEGREE_0_30:
                case DEGREE_0_40:
                case DEGREE_0_50:
                case DEGREE_0_60:
                case DEGREE_0_75:
                case DEGREE_0_80:
                case DEGREE_1_00:
                case DEGREE_1_20:
                case DEGREE_1_25: {
                    assertEquals(2.0, separations.dt());
                    break;
                }
                case DEGREE_2_00:
                case DEGREE_2_25:
                case DEGREE_2_40:
                case DEGREE_2_50: {
                    assertEquals(1.0, separations.dt());
                    break;
                }
                case DEGREE_3_00:
                case DEGREE_3_75:
                case DEGREE_4_00:
                case DEGREE_4_50:
                case DEGREE_5_00:
                default: {
                    assertEquals(0.0, separations.dt());
                }
            }
        }
    }

    @Test
    public void testDt_weekly7days() throws Exception {
        for (final SpatialResolution spatialResolution : SpatialResolution.values()) {
            final SynopticUncertaintyProvider separations = new SynopticUncertaintyProvider(spatialResolution,
                    TemporalResolution.weekly7d);

            switch (spatialResolution) {
                case DEGREE_0_05:
                case DEGREE_0_10:
                case DEGREE_0_15:
                case DEGREE_0_20:
                case DEGREE_0_25:
                case DEGREE_0_30:
                case DEGREE_0_40:
                case DEGREE_0_50:
                case DEGREE_0_60:
                case DEGREE_0_75:
                case DEGREE_0_80:
                case DEGREE_1_00:
                case DEGREE_1_20:
                case DEGREE_1_25: {
                    assertEquals(2.0, separations.dt());
                    break;
                }
                case DEGREE_2_00:
                case DEGREE_2_25:
                case DEGREE_2_40:
                case DEGREE_2_50: {
                    assertEquals(1.0, separations.dt());
                    break;
                }
                case DEGREE_3_00:
                case DEGREE_3_75:
                case DEGREE_4_00:
                case DEGREE_4_50:
                case DEGREE_5_00:
                default: {
                    assertEquals(0.0, separations.dt());
                }
            }
        }
    }

    @Test
    public void testDt_monthly() throws Exception {
        for (final SpatialResolution spatialResolution : SpatialResolution.values()) {
            final SynopticUncertaintyProvider separations = new SynopticUncertaintyProvider(spatialResolution,
                    TemporalResolution.monthly);

            switch (spatialResolution) {
                case DEGREE_0_05:
                case DEGREE_0_10:
                case DEGREE_0_15:
                case DEGREE_0_20:
                case DEGREE_0_25:
                case DEGREE_0_30:
                case DEGREE_0_40:
                case DEGREE_0_50: {
                    assertEquals(10.0, separations.dt());
                    break;
                }
                case DEGREE_0_60:
                case DEGREE_0_75: {
                    assertEquals(9.0, separations.dt());
                    break;
                }
                case DEGREE_0_80: {
                    //This value estimated Bettina roughly for the eps graphic (mail from 17.12.12 from Nick Rayner)
                    assertEquals(8.5, separations.dt());
                    break;
                }
                case DEGREE_1_00: {
                    assertEquals(6.0, separations.dt());
                    break;
                }
                case DEGREE_1_20: {
                    //This value estimated Bettina roughly for the eps graphic (mail from 17.12.12 from Nick Rayner)
                    assertEquals(3.5, separations.dt());
                    break;
                }
                case DEGREE_1_25: {
                    assertEquals(3.0, separations.dt());
                    break;
                }
                case DEGREE_2_00: {
                    assertEquals(0.5, separations.dt());
                    break;
                }
                case DEGREE_2_25: {
                    assertEquals(0.25, separations.dt());
                    break;
                }
                case DEGREE_2_40:
                case DEGREE_2_50: {
                    //This value estimated Bettina roughly for the eps graphic (mail from 17.12.12 from Nick Rayner)
                    assertEquals(0.2, separations.dt());
                    break;
                }
                case DEGREE_3_00: {
                    //This value estimated Bettina roughly for the eps graphic (mail from 17.12.12 from Nick Rayner)
                    assertEquals(0.1, separations.dt());
                    break;
                }
                case DEGREE_3_75:
                case DEGREE_4_00:
                case DEGREE_4_50:
                case DEGREE_5_00:
                default: {
                    assertEquals(0.0, separations.dt());
                }
            }
        }
    }

    @Test
    public void testDt_daily() throws Exception {
        for (final SpatialResolution spatialResolution : SpatialResolution.values()) {
            final SynopticUncertaintyProvider separations = new SynopticUncertaintyProvider(spatialResolution, TemporalResolution.daily);
            assertEquals(0.0, separations.dt());
        }
    }

    @Test
    public void testDt_seasonalAndAnnual() throws Exception {
        for (final SpatialResolution spatialResolution : SpatialResolution.values()) {
            final SynopticUncertaintyProvider separations = new SynopticUncertaintyProvider(spatialResolution,
                    TemporalResolution.seasonal);
            assertEquals(0.0, separations.dt());
        }

        for (SpatialResolution spatialResolution : SpatialResolution.values()) {
            final SynopticUncertaintyProvider separations = new SynopticUncertaintyProvider(spatialResolution, TemporalResolution.annual);
            assertEquals(0.0, separations.dt());
        }
    }

    @Test
    public void test_r() {
        SynopticUncertaintyProvider sup = new SynopticUncertaintyProvider(SpatialResolution.DEGREE_0_05, TemporalResolution.weekly5d);
        assertEquals(0.3646409252417634, sup.r(12), 1e-8);

        sup = new SynopticUncertaintyProvider(SpatialResolution.DEGREE_0_15, TemporalResolution.weekly5d);
        assertEquals(0.35780795774357266, sup.r(12), 1e-8);

    }

    @Test
    public void test_eta() {
        SynopticUncertaintyProvider sup = new SynopticUncertaintyProvider(SpatialResolution.DEGREE_0_05, TemporalResolution.weekly5d);
        assertEquals(2.7121519513732206, sup.eta(14, 156), 1e-8);

        sup = new SynopticUncertaintyProvider(SpatialResolution.DEGREE_0_15, TemporalResolution.weekly5d);
        assertEquals(2.771316127976776, sup.eta(16, 209), 1e-8);
    }

    @Test
    public void test_calculate() {
        final AggregationCell testCell = new TestCell(19, 211);
        SynopticUncertaintyProvider sup = new SynopticUncertaintyProvider(SpatialResolution.DEGREE_0_60, TemporalResolution.weekly7d);
        assertEquals(0.8571179770138263, sup.calculate(testCell, 2.6), 1e-8);

        sup = new SynopticUncertaintyProvider(SpatialResolution.DEGREE_0_75, TemporalResolution.weekly5d);
        assertEquals(1.0216628434543982, sup.calculate(testCell, 3.2), 1e-8);
    }

    private class TestCell implements AggregationCell {

        private final int y;
        private final int sampleCount;

        private TestCell(int y, int sampleCount) {
            this.y = y;
            this.sampleCount = sampleCount;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public int getX() {
            return 0;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public long getSampleCount() {
            return sampleCount;
        }

        @Override
        public Number[] getResults() {
            return new Number[0];
        }

        @Override
        public double getSeaSurfaceTemperature() {
            return 0;
        }

        @Override
        public double getSeaSurfaceTemperatureAnomaly() {
            return 0;
        }

        @Override
        public double getRandomUncertainty() {
            return 0;
        }

        @Override
        public double getLargeScaleUncertainty() {
            return 0;
        }

        @Override
        public double getCoverageUncertainty() {
            return 0;
        }

        @Override
        public double getAdjustmentUncertainty() {
            return 0;
        }

        @Override
        public double getSynopticUncertainty() {
            return 0;
        }

        @Override
        public double getSeaIceFraction() {
            return 0;
        }
    }
}
