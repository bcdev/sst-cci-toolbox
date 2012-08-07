package org.esa.cci.sst.regrid.calculators;

import org.esa.cci.sst.regrid.Calculator;
import org.esa.cci.sst.regrid.LUT1;
import org.esa.cci.sst.util.ArrayGrid;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bettina Scholze
 *         Date: 03.08.12 13:12
 */
public class CalculatorFactory {

    private static Map<String, Calculator> repository = new HashMap<String, Calculator>();

    public static Calculator create(ArrayGrid arrayGrid) {
        String variable = arrayGrid.getVariable();

        if ("uncorrelated_uncertainty".equals(variable)) {
            return fetch("uncorrelatedUncertaintyCalculator", new UncorrelatedUncertaintyCalculator());
        } else if ("synoptically_correlated_uncertainty".equals(variable) || "adjustment_uncertainty".equals(variable)) {
            return fetch("synopticallyCorrelatedUncertaintyCalculator", new SynopticallyCorrelatedUncertaintyCalculator());
        } else if ("coverage_uncertainty".equals(variable)) {
            LUT1 lut1 = LUT1.read(arrayGrid.getGridDef());
            return fetch("coverageUncertaintyCalculator", new CoverageUncertaintyCalculator(lut1));
        } else {
            //geophysical variables, large_scale_correlated_uncertainties
            return fetch("meanCalculator", new MeanCalculator());
        }
    }

    private static Calculator fetch(String key, Calculator calculator) {
        if (repository.get(key) == null) {
            repository.put(key, calculator);
        }
        return repository.get(key);
    }
}
