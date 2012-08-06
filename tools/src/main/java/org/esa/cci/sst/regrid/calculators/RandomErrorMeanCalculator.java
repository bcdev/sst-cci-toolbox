package org.esa.cci.sst.regrid.calculators;

/**
 * @author Bettina Scholze
 *         Date: 03.08.12 13:18
 */
public class RandomErrorMeanCalculator extends AbstractMeanCalculator {

    @Override
    protected double takeTheMean(double sum) {
        double coverage = validAggregatedCells / Math.pow(numberOfCellsToAggregateInEachDimension, 2);
        if (coverage >= context.getMinCoverage()){
            return Math.sqrt(sum) / validAggregatedCells;
        }  else {
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
