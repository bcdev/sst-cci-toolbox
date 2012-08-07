package org.esa.cci.sst.regrid.calculators;

import org.esa.cci.sst.regrid.LUT3;

/**
 * @author Bettina Scholze
 *         Date: 03.08.12 14:11
 */
public class SynopticallyCorrelatedUncertaintyCalculator extends AbstractMeanCalculator {

    @Override
    protected double takeTheMean(double sum) {
        int width = context.getTargetArrayGrid().getWidth();
        int targetCellIndex = context.getTargetCellIndex();
        double d_xy = LUT3.getD_xy(targetCellIndex % width, targetCellIndex / width);
        double d_t = LUT3.getD_t(targetCellIndex % width, targetCellIndex / width);
        double l_xy = 0;
        double l_t = 0;
        double r = Math.exp(-0.5 * (d_xy / l_xy + d_t / l_t));
        double eta = validAggregatedCells / (1 + r * (validAggregatedCells - 1));

        double coverage = validAggregatedCells / Math.pow(numberOfCellsToAggregateInEachDimension, 2);
        if (coverage >= context.getMinCoverage()) {
            return Math.sqrt(sum) / eta;
        } else {
            return Double.NaN;
        }
    }

    //one line in size of the target cell
    @Override
    protected double sumCells1D(double[] source, int startIndex) {
        double sum = 0.0;
        for (int l = startIndex; l < startIndex + numberOfCellsToAggregateInEachDimension; l++) {
            double value = source[l];
            if (Double.isNaN(value)) {
                continue; //skip it
            }
            sum += Math.pow(value, 2);
            this.validAggregatedCells++;
        }
        return sum;
    }
}
