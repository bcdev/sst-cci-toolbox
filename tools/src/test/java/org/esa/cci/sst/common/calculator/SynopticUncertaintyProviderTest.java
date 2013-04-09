package org.esa.cci.sst.common.calculator;

import org.esa.cci.sst.common.SpatialResolution;
import org.esa.cci.sst.common.TemporalResolution;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class SynopticUncertaintyProviderTest {

    @Test
    public void testDxy() throws Exception {
        final SynopticUncertaintyProvider separations = new SynopticUncertaintyProvider(SpatialResolution.DEGREE_5_00, null);
        assertEquals(36, GridDef.createGlobal(SpatialResolution.DEGREE_5_00.getResolution()).getHeight());
        assertEquals(72, GridDef.createGlobal(SpatialResolution.DEGREE_5_00.getResolution()).getWidth());

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
}
