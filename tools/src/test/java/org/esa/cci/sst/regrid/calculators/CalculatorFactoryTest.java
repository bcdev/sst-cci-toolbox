package org.esa.cci.sst.regrid.calculators;

import org.junit.Test;

import static junit.framework.Assert.assertTrue;


/**
 * @author Bettina Scholze
 *         Date: 03.08.12 13:32
 */
public class CalculatorFactoryTest {

    @Test
    public void testCreate() throws Exception {
        assertTrue(CalculatorFactory.create("default") instanceof MeanCalculator);

        assertTrue(CalculatorFactory.create("uncorrelated_uncertainty") instanceof RandomErrorMeanCalculator);

        assertTrue(CalculatorFactory.create("synoptically_correlated_uncertainty") instanceof SynopticallyCorrelatedErrorMeanCalculator);
        assertTrue(CalculatorFactory.create("adjustment_uncertainty") instanceof SynopticallyCorrelatedErrorMeanCalculator);
    }
}
