package org.esa.cci.sst.common;

import org.esa.cci.sst.common.CoverageUncertaintyProvider;
import org.junit.Assert;
import org.junit.Test;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * @author Norman
 */
public class CoverageUncertaintyProviderTest {
    @Test
    public void testCell5() throws Exception {
        CoverageUncertaintyProvider provider = create();
        Assert.assertEquals(Double.NaN, provider.getCoverageUncertainty5(0, 0, 0L), 1.0e-10);
        Assert.assertEquals(1.1 * (1.0 - pow(5L / 77500.0, 0.5)), provider.getCoverageUncertainty5(0, 0, 5L), 1.0e-10);
        Assert.assertEquals(1.1 * (1.0 - pow(12L / 77500.0, 0.5)), provider.getCoverageUncertainty5(0, 0, 12L), 1.0e-10);
    }

    @Test
    public void testCell90() throws Exception {
        CoverageUncertaintyProvider provider = create();
        Assert.assertEquals(Double.NaN, provider.getCoverageUncertainty90(0, 0, 0L), 1.0e-10);
        Assert.assertEquals(1.2 / sqrt(5L), provider.getCoverageUncertainty90(0, 0, 5L), 1.0e-10);
        Assert.assertEquals(1.2 / sqrt(12L), provider.getCoverageUncertainty90(0, 0, 12L), 1.0e-10);
    }

    private CoverageUncertaintyProvider create() {
        return new CoverageUncertaintyProvider(0) {
            @Override
            protected double getMagnitude5(int cellX, int cellY) {
                return 1.1;
            }

            @Override
            protected double getExponent5(int cellX, int cellY) {
                return 0.5;
            }

            @Override
            protected double getMagnitude90(int cellX, int cellY, int month) {
                return 1.2;
            }
        };
    }
}
