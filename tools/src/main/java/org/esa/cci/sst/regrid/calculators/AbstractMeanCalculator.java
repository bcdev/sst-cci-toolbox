package org.esa.cci.sst.regrid.calculators;

import org.esa.cci.sst.regrid.Calculator;
import org.esa.cci.sst.regrid.CellAggregationContext;

/**
 * {@author Bettina Scholze}
 * Date: 03.08.12 15:40
 */
abstract class AbstractMeanCalculator implements Calculator {

    protected int numberOfCellsToAggregateInEachDimension;
    protected int validAggregatedCells;
    protected CellAggregationContext context;


    /**
     * Calculates value for one cell in the target grid.
     *
     * @param context
     * @return
     */
    @Override
    public double calculate(CellAggregationContext context) {
        this.validAggregatedCells = 0;
        this.numberOfCellsToAggregateInEachDimension = context.getNumberOfCellsToAggregateInEachDimension();
        this.context = context;

        double sum = sumCells2D();
        return takeTheMean(sum);
    }

    //one target cell is summed up in the source grid
    protected double sumCells2D() {
        double sum = 0.0;
        double[] source = context.getSourceDataScaled();
        int sourceWidth = context.getSourceArrayGrid().getWidth();

        int targetLineIndex = calculateTargetLineIndex();
        int targetColumnIndex = calculateTargetColumnIndex();

        int targetLineStep = sourceWidth * numberOfCellsToAggregateInEachDimension;
        int startSourceIndexInTargetLine = targetLineIndex * targetLineStep;
        int sourceStartCellIndex = startSourceIndexInTargetLine + targetColumnIndex * numberOfCellsToAggregateInEachDimension;

        for (int i = sourceStartCellIndex; i < sourceStartCellIndex + targetLineStep; i += sourceWidth) { //step to next line in the source
            sum += sumCells1D(source, i);
        }
        return sum;
    }

    protected final int calculateTargetLineIndex() {
        return context.getTargetCellIndex() / context.getTargetArrayGrid().getWidth(); //(0,1,2.. - through cast-magic)
    }

    protected final int calculateTargetColumnIndex() {
        return context.getTargetCellIndex() % context.getTargetArrayGrid().getWidth();
    }

    //one line in size of the target cell
    protected abstract double sumCells1D(double[] source, int startIndex);

    protected abstract double takeTheMean(double sum);
}
