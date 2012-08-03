package org.esa.cci.sst.regrid;

/**
 * @author Bettina Scholze
 *         Date: 30.07.12 15:38
 */
public class MeanCalculator implements Calculator {

    private int getNumberOfCellsToAggregateInEachDimension;
    private int validAggregatedCells;


    @Override
    public double calculate(CellAggregationContext context) {
        this.validAggregatedCells = 0;
        this.getNumberOfCellsToAggregateInEachDimension = context.getNumberOfCellsToAggregateInEachDimension();
        return calculateMean(context.getTargetCellIndex(), context.getSourceDataScaled(), context.getSourceArrayGrid().getWidth());
    }

    private double calculateMean(int targetCellIndex, double[] sourceData, int sourceWidth) {
        double sum = sumCells2D(targetCellIndex, sourceData, sourceWidth);
        return arithmeticMean(sum);
    }

    private double arithmeticMean(double sum) {
        return sum / validAggregatedCells;
    }

    //one target cell is summed up in the source grid
    private double sumCells2D(int targetCellIndex, double[] source, int sourceWidth) {
        double sum = 0.0;
        int targetWidth = sourceWidth / getNumberOfCellsToAggregateInEachDimension;
        int targetLineIndex = targetCellIndex / targetWidth; //(0,1,2.. - through cast-magic)
        int targetColumnIndex = targetCellIndex % targetWidth;

        int targetLineStep = sourceWidth * getNumberOfCellsToAggregateInEachDimension;
        int startSourceIndexInTargetLine = targetLineIndex * targetLineStep;
        int sourceStartCellIndex = startSourceIndexInTargetLine + targetColumnIndex * getNumberOfCellsToAggregateInEachDimension;

        for (int i = sourceStartCellIndex; i < sourceStartCellIndex + targetLineStep; i += sourceWidth) { //step to next line in the source
            sum += sumCells1D(source, i);
        }
        return sum;
    }

    //one line in size of the target cell
    private double sumCells1D(double[] source, int startIndex) {
        double sum = 0.0;
        for (int l = startIndex; l < startIndex + getNumberOfCellsToAggregateInEachDimension; l++) {
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
