package org.esa.cci.sst.regrid;

/**
 * @author Bettina Scholze
 *         Date: 30.07.12 15:38
 */
public class MeanCalculator implements Calculator {

    @Override
    public double calculate(int targetCellIndex, double[] sourceData, int numberOfCellsToAggregateInEachDimension, int sourceWidth) {
        int numberOfCells2D = numberOfCellsToAggregateInEachDimension * numberOfCellsToAggregateInEachDimension;
        double sum = sumCells2D(targetCellIndex, sourceData, numberOfCellsToAggregateInEachDimension, sourceWidth);
        return arithmeticMean(numberOfCells2D, sum);

    }

    private double arithmeticMean(int numberOfCellsToAggregate, double sum) {
        return sum / numberOfCellsToAggregate;
    }

    //one target cell is summed up in the source grid
    private double sumCells2D(int targetCellIndex, double[] source, int numberOfCellsToAggregateIn1Dimension, int sourceWidth) {
        double sum = 0.0;
        int targetWidth = sourceWidth / numberOfCellsToAggregateIn1Dimension;
        int targetLineIndex = targetCellIndex / targetWidth; //(0,1,2.. - through cast-magic)
        int targetColumnIndex = targetCellIndex % targetWidth;

        int targetLineStep = sourceWidth * numberOfCellsToAggregateIn1Dimension;
        int startSourceIndexInTargetLine = targetLineIndex * targetLineStep;
        int sourceStartCellIndex = startSourceIndexInTargetLine + targetColumnIndex * numberOfCellsToAggregateIn1Dimension;

        for (int i = sourceStartCellIndex; i < sourceStartCellIndex + targetLineStep; i += sourceWidth) { //step to next line in the source
            sum += sumCells1D(source, i, numberOfCellsToAggregateIn1Dimension);
        }
        return sum;
    }

    //one line in size of the target cell
    private double sumCells1D(double[] source, int startIndex, double howMany) {
        double sum = 0.0;
        for (int l = startIndex; l < startIndex + howMany; l++) {
            double value = source[l];
            sum += value;
        }
        return sum;
    }
}
