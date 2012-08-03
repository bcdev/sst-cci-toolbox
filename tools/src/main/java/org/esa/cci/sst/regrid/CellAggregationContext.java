package org.esa.cci.sst.regrid;

import org.esa.cci.sst.util.ArrayGrid;

/**
 * @author Bettina Scholze
 *         Date: 25.07.12 11:55
 */
public class CellAggregationContext {

    private String variable;
    private int targetCellIndex;
    private ArrayGrid sourceArrayGrid;
    private double[] sourceDataScaled;
    private ArrayGrid targetArrayGrid;
    private int numberOfCellsToAggregateInEachDimension;
    private SpatialResolution targetResolution;

    public CellAggregationContext(String variable, double[] sourceDataScaled, ArrayGrid sourceArrayGrid, ArrayGrid targetArrayGrid) {
        this.variable = variable;
        this.sourceDataScaled = sourceDataScaled;
        this.sourceArrayGrid = sourceArrayGrid;
        this.targetArrayGrid = targetArrayGrid;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public int getNumberOfCellsToAggregateInEachDimension() {
        return numberOfCellsToAggregateInEachDimension;
    }

    public void setNumberOfCellsToAggregateInEachDimension(int numberOfCellsToAggregateInEachDimension) {
        this.numberOfCellsToAggregateInEachDimension = numberOfCellsToAggregateInEachDimension;
    }

    public ArrayGrid getSourceArrayGrid() {
        return sourceArrayGrid;
    }

    public void setSourceArrayGrid(ArrayGrid sourceArrayGrid) {
        this.sourceArrayGrid = sourceArrayGrid;
    }

    public double[] getSourceDataScaled() {
        return sourceDataScaled;
    }

    public int getTargetCellIndex() {
        return targetCellIndex;
    }

    public void setSourceDataScaled(double[] sourceDataScaled) {
        this.sourceDataScaled = sourceDataScaled;
    }

    public void setTargetCellIndex(int targetCellIndex) {
        this.targetCellIndex = targetCellIndex;
    }

    public ArrayGrid getTargetArrayGrid() {
        return targetArrayGrid;
    }

    public void setTargetArrayGrid(ArrayGrid targetArrayGrid) {
        this.targetArrayGrid = targetArrayGrid;
    }
}
