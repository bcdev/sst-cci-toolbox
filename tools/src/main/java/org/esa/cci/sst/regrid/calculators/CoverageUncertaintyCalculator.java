package org.esa.cci.sst.regrid.calculators;


import org.esa.cci.sst.regrid.LUT1;
import org.esa.cci.sst.util.ArrayGrid;

/**
 * {@author Bettina Scholze}
 * Date: 06.08.12 09:32
 */
public class CoverageUncertaintyCalculator extends AbstractMeanCalculator {

    private final ArrayGrid exponentGrid;
    private final ArrayGrid magnitudeGrid;

    public CoverageUncertaintyCalculator(LUT1 lut1) {
        super();
        exponentGrid = (ArrayGrid) lut1.getExponentGrid();
        magnitudeGrid = (ArrayGrid) lut1.getMagnitudeGrid();
    }

    @Override
    protected double sumCells1D(double[] source, int startIndex) {
        double sumOfValidSourceCells = 0.0;
        for (int l = startIndex; l < startIndex + numberOfCellsToAggregateInEachDimension; l++) {
            double value = source[l];
            if (Double.isNaN(value)) {
                continue; //skip it
            }
            this.validAggregatedCells++;
            sumOfValidSourceCells++;
        }
        return sumOfValidSourceCells;
    }

    @Override
    protected double takeTheMean(double sum) {
        double n_total = Math.pow(this.numberOfCellsToAggregateInEachDimension, 2);
        double f = sum / n_total;

        double p = exponentGrid.getSampleDouble(calculateTargetColumnIndex(), calculateTargetLineIndex());
        double s_0 = magnitudeGrid.getSampleDouble(calculateTargetColumnIndex(), calculateTargetLineIndex());

        return s_0 * (1 - Math.pow(f, p));
    }
}
