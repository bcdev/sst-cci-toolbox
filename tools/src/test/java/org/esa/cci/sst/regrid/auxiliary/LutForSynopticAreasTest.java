package org.esa.cci.sst.regrid.auxiliary;

import org.esa.cci.sst.common.TemporalResolution;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.SpatialResolution;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * {@author Bettina Scholze}
 * Date: 17.12.12 13:27
 */
public class LutForSynopticAreasTest {

    @Test
    public void testDistanceSep_smallestOutputResolution() throws Exception {
        LutForSynopticAreas lut = new LutForSynopticAreas(null, SpatialResolution.DEGREE_5_00);
        assertEquals(36, GridDef.createGlobal(SpatialResolution.DEGREE_5_00.getResolution()).getHeight());
        assertEquals(72, GridDef.createGlobal(SpatialResolution.DEGREE_5_00.getResolution()).getWidth());

        assertEquals(188.815598, lut.getDxy(0), 0.000001); // almost north pole
        assertEquals(286.830454, lut.getDxy(18), 0.000001); // almost equator
        assertEquals(188.815598, lut.getDxy(35), 0.000001); // almost south pole
        assertEquals(240.705816, lut.getDxy(9), 0.000001);
        assertEquals(234.940236, lut.getDxy(27), 0.000001);
    }

    @Test
    public void testTimeSep_weekly5days() throws Exception {
        for (SpatialResolution spatialResolution : SpatialResolution.values()) {
            LutForSynopticAreas lut = new LutForSynopticAreas(TemporalResolution.weekly5d, spatialResolution);

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
                    assertEquals(2.0, lut.getDt());
                    break;
                }
                case DEGREE_2_00:
                case DEGREE_2_25:
                case DEGREE_2_40:
                case DEGREE_2_50: {
                    assertEquals(1.0, lut.getDt());
                    break;
                }
                case DEGREE_3_00:
                case DEGREE_3_75:
                case DEGREE_4_00:
                case DEGREE_4_50:
                case DEGREE_5_00:
                default: {
                    assertEquals(0.0, lut.getDt());
                }
            }
        }
    }

    @Test
    public void testTimeSep_weekly7days() throws Exception {
        for (SpatialResolution spatialResolution : SpatialResolution.values()) {
            LutForSynopticAreas lut = new LutForSynopticAreas(TemporalResolution.weekly7d, spatialResolution);

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
                    assertEquals(2.0, lut.getDt());
                    break;
                }
                case DEGREE_2_00:
                case DEGREE_2_25:
                case DEGREE_2_40:
                case DEGREE_2_50: {
                    assertEquals(1.0, lut.getDt());
                    break;
                }
                case DEGREE_3_00:
                case DEGREE_3_75:
                case DEGREE_4_00:
                case DEGREE_4_50:
                case DEGREE_5_00:
                default: {
                    assertEquals(0.0, lut.getDt());
                }
            }
        }
    }

    @Test
    public void testTimeSep_monthly() throws Exception {

        for (SpatialResolution spatialResolution : SpatialResolution.values()) {
            LutForSynopticAreas lut = new LutForSynopticAreas(TemporalResolution.monthly, spatialResolution);

            switch (spatialResolution) {
                case DEGREE_0_05:
                case DEGREE_0_10:
                case DEGREE_0_15:
                case DEGREE_0_20:
                case DEGREE_0_25:
                case DEGREE_0_30:
                case DEGREE_0_40:
                case DEGREE_0_50: {
                    assertEquals(10.0, lut.getDt());
                    break;
                }
                case DEGREE_0_60:
                case DEGREE_0_75: {
                    assertEquals(9.0, lut.getDt());
                    break;
                }
                case DEGREE_0_80: {
                    //This value estimated Bettina roughly for the eps graphic (mail from 17.12.12 from Nick Rayner)
                    assertEquals(8.5, lut.getDt());
                    break;
                }
                case DEGREE_1_00: {
                    assertEquals(6.0, lut.getDt());
                    break;
                }
                case DEGREE_1_20: {
                    //This value estimated Bettina roughly for the eps graphic (mail from 17.12.12 from Nick Rayner)
                    assertEquals(3.5, lut.getDt());
                    break;
                }
                case DEGREE_1_25: {
                    assertEquals(3.0, lut.getDt());
                    break;
                }
                case DEGREE_2_00: {
                    assertEquals(0.5, lut.getDt());
                    break;
                }
                case DEGREE_2_25: {
                    assertEquals(0.25, lut.getDt());
                    break;
                }
                case DEGREE_2_40:
                case DEGREE_2_50: {
                    //This value estimated Bettina roughly for the eps graphic (mail from 17.12.12 from Nick Rayner)
                    assertEquals(0.2, lut.getDt());
                    break;
                }
                case DEGREE_3_00: {
                    //This value estimated Bettina roughly for the eps graphic (mail from 17.12.12 from Nick Rayner)
                    assertEquals(0.1, lut.getDt());
                    break;
                }
                case DEGREE_3_75:
                case DEGREE_4_00:
                case DEGREE_4_50:
                case DEGREE_5_00:
                default: {
                    assertEquals(0.0, lut.getDt());
                }
            }
        }
    }

    @Test
    public void testTimeSep_daily() throws Exception {

        for (SpatialResolution spatialResolution : SpatialResolution.values()) {
            LutForSynopticAreas lut = new LutForSynopticAreas(TemporalResolution.daily, spatialResolution);
            assertEquals(0.0, lut.getDt());
        }
    }

    @Test
    public void testTimeSep_seasonalAndAnnual() throws Exception {

        for (SpatialResolution spatialResolution : SpatialResolution.values()) {
            LutForSynopticAreas lut = new LutForSynopticAreas(TemporalResolution.seasonal, spatialResolution);
            assertEquals(0.0, lut.getDt());
        }

        for (SpatialResolution spatialResolution : SpatialResolution.values()) {
            LutForSynopticAreas lut = new LutForSynopticAreas(TemporalResolution.annual, spatialResolution);
            assertEquals(0.0, lut.getDt());
        }
    }


}
