package org.esa.cci.sst.regrid.calculators;

import org.esa.cci.sst.regrid.SpatialResolution;
import org.esa.cci.sst.util.ArrayGrid;
import org.esa.cci.sst.util.GridDef;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;


/**
 * @author Bettina Scholze
 *         Date: 03.08.12 13:32
 */
public class CalculatorFactoryTest {

    private final GridDef defaultGridDef = SpatialResolution.DEGREE_0_10.getAssociatedGridDef();

    @Test
    public void testCreate_MeanCalculator() throws Exception {
        ArrayGrid arrayGrid = new ArrayGrid(defaultGridDef, null, null, 0, 0) {
            @Override
            public String getVariable() {
                return "default";
            }
        };
        assertTrue(CalculatorFactory.create(arrayGrid) instanceof MeanCalculator);
    }

    @Test
    public void testCreate_UncorrelatedUncertaintyCalculator() throws Exception {
        ArrayGrid arrayGrid = new ArrayGrid(defaultGridDef, null, null, 0, 0) {
            @Override
            public String getVariable() {
                return "uncorrelated_uncertainty";
            }
        };
        assertTrue(CalculatorFactory.create(arrayGrid) instanceof UncorrelatedUncertaintyCalculator);
    }

    @Test
    public void testCreate_SynopticallyCorrelatedUncertaintyCalculator() throws Exception {
        ArrayGrid arrayGrid = new ArrayGrid(defaultGridDef, null, null, 0, 0) {
            @Override
            public String getVariable() {
                return "synoptically_correlated_uncertainty";
            }
        };
        assertTrue(CalculatorFactory.create(arrayGrid) instanceof SynopticallyCorrelatedUncertaintyCalculator);

        ArrayGrid arrayGrid_2 = new ArrayGrid(defaultGridDef, null, null, 0, 0) {
            @Override
            public String getVariable() {
                return "adjustment_uncertainty";
            }
        };
        assertTrue(CalculatorFactory.create(arrayGrid_2) instanceof SynopticallyCorrelatedUncertaintyCalculator);
    }

    @Test
    public void testCreate_CoverageUncertaintyCalculator() throws Exception {
        ArrayGrid arrayGrid = new ArrayGrid(defaultGridDef, null, null, 0, 0) {
            @Override
            public String getVariable() {
                return "coverage_uncertainty";
            }
        };
        assertTrue(CalculatorFactory.create(arrayGrid) instanceof CoverageUncertaintyCalculator);
    }
}
