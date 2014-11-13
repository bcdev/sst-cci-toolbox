package org.esa.cci.sst.tools.regavg;

import org.junit.Test;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class AveragingCoverageUncertaintyTest {

    @Test
    public void testCalculate_Cell5() throws Exception {
        final AveragingCoverageUncertaintyProvider provider = create();

        assertEquals(Double.NaN, provider.calculate(0, 0, 0L, 5.0), 1.0e-10);
        assertEquals(1.1 * (1.0 - pow(5L / 77500.0, 0.5)), provider.calculate(0, 0, 5L, 5.0), 1.0e-10);
        assertEquals(1.1 * (1.0 - pow(12L / 77500.0, 0.5)), provider.calculate(0, 0, 12L, 5.0), 1.0e-10);
    }

    @Test
    public void testCalculate_Cell90() throws Exception {
        final AveragingCoverageUncertaintyProvider provider = create();

        assertEquals(Double.NaN, provider.calculate(0, 0, 0L, 90.0), 1.0e-10);
        assertEquals(1.2 / sqrt(5L), provider.calculate(0, 0, 5L, 90.0), 1.0e-10);
        assertEquals(1.2 / sqrt(12L), provider.calculate(0, 0, 12L, 90.0), 1.0e-10);
    }

    @Test
    public void testCalculate_invalidResolution() throws Exception {
        final AveragingCoverageUncertaintyProvider provider = create();

        assertEquals(Double.NaN, provider.calculate(0, 0, 108L, 16.0), 1e-8);
    }

    private AveragingCoverageUncertaintyProvider create() {
        return new AveragingCoverageUncertaintyProvider(0) {
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
