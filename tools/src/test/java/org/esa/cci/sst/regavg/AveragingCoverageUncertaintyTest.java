package org.esa.cci.sst.regavg;

import org.junit.Assert;
import org.junit.Test;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * @author Norman
 */
public class AveragingCoverageUncertaintyTest {
    @Test
    public void testCell5() throws Exception {
        AveragingCoverageUncertainty provider = create();

        Assert.assertEquals(Double.NaN, provider.calculate(0, 0, 0L, 5.0), 1.0e-10);
        Assert.assertEquals(1.1 * (1.0 - pow(5L / 77500.0, 0.5)), provider.calculate(0, 0, 5L, 5.0), 1.0e-10);
        Assert.assertEquals(1.1 * (1.0 - pow(12L / 77500.0, 0.5)), provider.calculate(0, 0, 12L, 5.0), 1.0e-10);
    }

    @Test
    public void testCell90() throws Exception {
        AveragingCoverageUncertainty provider = create();
        Assert.assertEquals(Double.NaN, provider.calculate(0, 0, 0L, 90.0), 1.0e-10);
        Assert.assertEquals(1.2 / sqrt(5L), provider.calculate(0, 0, 5L, 90.0), 1.0e-10);
        Assert.assertEquals(1.2 / sqrt(12L), provider.calculate(0, 0, 12L, 90.0), 1.0e-10);
    }

    private AveragingCoverageUncertainty create() {
        return new AveragingCoverageUncertainty(0) {
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
