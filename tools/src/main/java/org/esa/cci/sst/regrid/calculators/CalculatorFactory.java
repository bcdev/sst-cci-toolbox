package org.esa.cci.sst.regrid.calculators;

import org.esa.cci.sst.regrid.Calculator;

/**
 * @author Bettina Scholze
 *         Date: 03.08.12 13:12
 */
public class CalculatorFactory {

    public static Calculator create(String variable) {

        if ("uncorrelated_uncertainty".equals(variable)) {
            return new RandomErrorMeanCalculator();
        } else if ("synoptically_correlated_uncertainty".equals(variable) || "adjustment_uncertainty".equals(variable)) {
            return new SynopticallyCorrelatedErrorMeanCalculator();
        } else {
            //geophysical variables, large_scale_correlated_uncertainties
            return new MeanCalculator();
        }
    }
}
