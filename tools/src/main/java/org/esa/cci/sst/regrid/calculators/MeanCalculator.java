package org.esa.cci.sst.regrid.calculators;

/**
 * @author Bettina Scholze
 *         Date: 30.07.12 15:38
 */
public class MeanCalculator extends AbstractMeanCalculator{

    @Override
    protected double takeTheMean(double sum) {
        double coverage = validAggregatedCells / Math.pow(numberOfCellsToAggregateInEachDimension, 2);
        if (coverage >= context.getMinCoverage()){
            return sum / validAggregatedCells;
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
            sum += value;
            this.validAggregatedCells++;
        }
        return sum;
    }
}
