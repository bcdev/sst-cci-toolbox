package org.esa.cci.sst.common;

import org.esa.cci.sst.common.CoverageUncertaintyProvider;
import org.junit.Ignore;

/**
 * @author Norman
 */
@Ignore
public class ScalarCoverageUncertaintyProvider extends CoverageUncertaintyProvider {

    private final double magnitude90;
    private final double magnitude5;
    private final double exponent5;

     public ScalarCoverageUncertaintyProvider(double magnitude90, double magnitude5, double exponent5) {
        super(0);
        this.magnitude90 = magnitude90;
        this.magnitude5 = magnitude5;
        this.exponent5 = exponent5;
    }

    @Override
    protected double getMagnitude90(int cellX, int cellY, int month) {
        return magnitude90;
    }

    @Override
    protected double getMagnitude5(int cellX, int cellY) {
        return magnitude5;
    }

    @Override
    protected double getExponent5(int cellX, int cellY) {
        return exponent5;
    }

}
