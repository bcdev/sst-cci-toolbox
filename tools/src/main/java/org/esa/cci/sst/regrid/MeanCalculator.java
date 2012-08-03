package org.esa.cci.sst.regrid;

/**
 * @author Bettina Scholze
 *         Date: 30.07.12 15:38
 */
public class MeanCalculator implements Calculator {

    private int numberOfCellsToAggregateInEachDimension;
    private int validAggregatedCells;
    private double minCoverage;


    @Override
    public double calculate(CellAggregationContext context) {
        this.validAggregatedCells = 0;
        this.numberOfCellsToAggregateInEachDimension = context.getNumberOfCellsToAggregateInEachDimension();
        this.minCoverage = context.getMinCoverage();
        return calculateMean(context.getTargetCellIndex(), context.getSourceDataScaled(), context.getSourceArrayGrid().getWidth());
    }

    private double calculateMean(int targetCellIndex, double[] sourceData, int sourceWidth) {
        double sum = sumCells2D(targetCellIndex, sourceData, sourceWidth);
        return arithmeticMean(sum);
    }

    private double arithmeticMean(double sum) {
        double coverage = validAggregatedCells / Math.pow(numberOfCellsToAggregateInEachDimension, 2);
        if (coverage >= minCoverage){
            return sum / validAggregatedCells;
        }  else {
            return Double.NaN;
        }
    }

    //one target cell is summed up in the source grid
    private double sumCells2D(int targetCellIndex, double[] source, int sourceWidth) {
        double sum = 0.0;
        int targetWidth = sourceWidth / numberOfCellsToAggregateInEachDimension;
        int targetLineIndex = targetCellIndex / targetWidth; //(0,1,2.. - through cast-magic)
        int targetColumnIndex = targetCellIndex % targetWidth;

        int targetLineStep = sourceWidth * numberOfCellsToAggregateInEachDimension;
        int startSourceIndexInTargetLine = targetLineIndex * targetLineStep;
        int sourceStartCellIndex = startSourceIndexInTargetLine + targetColumnIndex * numberOfCellsToAggregateInEachDimension;

        for (int i = sourceStartCellIndex; i < sourceStartCellIndex + targetLineStep; i += sourceWidth) { //step to next line in the source
            sum += sumCells1D(source, i);
        }
        return sum;
    }

    //one line in size of the target cell
    private double sumCells1D(double[] source, int startIndex) {
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
