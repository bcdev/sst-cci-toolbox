package org.esa.cci.sst.regrid;

import org.esa.cci.sst.util.ArrayGrid;
import org.esa.cci.sst.util.GridDef;
import ucar.ma2.Array;

import java.util.Map;

/**
 * @author Bettina Scholze
 *         Date: 26.07.12 15:48
 */
public class GridAggregation {
    private final Map<String, ArrayGrid> sourceGrids;
    private final Map<String, ArrayGrid> targetGrids;
    private Calculator calculator;

    public GridAggregation(Map<String, ArrayGrid> sourceGrids, Map<String, ArrayGrid> targetGrids, Calculator calculator) {
        this.sourceGrids = sourceGrids;
        this.targetGrids = targetGrids;
        this.calculator = calculator;
    }

    public void aggregateGrids(double minCoverage) {
        for (String variable : targetGrids.keySet()) {
            ArrayGrid sourceArrayGrid = sourceGrids.get(variable);
            ArrayGrid targetArrayGrid = targetGrids.get(variable);

            double[] sourceDataScaled = fetchDataAsScaledDoubles(sourceArrayGrid);
            CellAggregationContext context = new CellAggregationContext(variable, sourceDataScaled, sourceArrayGrid, targetArrayGrid);
            context.setMinCoverage(minCoverage);
            double[] targetDataScaled = aggregateData(context);
            fillTargetStorage(targetArrayGrid, targetDataScaled);
        }
    }

    double[] fetchDataAsScaledDoubles(ArrayGrid sourceArrayGrid) {
        final Array array = sourceArrayGrid.getArray();
        final Object sourceStorageObject = array.getStorage();
        final double scaling = sourceArrayGrid.getScaling();
        final double offset = sourceArrayGrid.getOffset();
        final Number fillValue = sourceArrayGrid.getFillValue();
        final GridDef sourceGridDef = sourceArrayGrid.getGridDef();
        final int length = sourceGridDef.getNumberOfCells();
        final double[] doubles = new double[length];
        final String type = array.getElementType().getName();

        if ("short".equals(type)) {
            short[] storage = (short[]) sourceStorageObject;
            assert storage.length == doubles.length;
            for (int i = 0; i < length; i++) {
                short rawValue = storage[i];
                if (fillValue.shortValue() == rawValue) {
                    doubles[i] = Double.NaN;
                } else {
                    double value = rawValue * scaling + offset;
                    doubles[i] = value;
                }
            }
        } else if ("byte".equals(type)) {
            byte[] storage = (byte[]) sourceStorageObject;
            assert storage.length == doubles.length;
            for (int i = 0; i < length; i++) {
                byte rawValue = storage[i];
                if (fillValue.byteValue() == rawValue) {
                    doubles[i] = Double.NaN;
                } else {
                    double value = rawValue * scaling + offset;
                    doubles[i] = value;
                }
            }
        } else if ("int".equals(type)) {
            int[] storage = (int[]) sourceStorageObject;
            assert storage.length == doubles.length;
            for (int i = 0; i < length; i++) {
                int rawValue = storage[i];
                if (fillValue.intValue() == rawValue) {
                    doubles[i] = Double.NaN;
                } else {
                    double value = rawValue * scaling + offset;
                    doubles[i] = value;
                }
            }
        } else {
            throw new RuntimeException("byte[], short[] or int[] expected, but found " + type + "[].");
        }
        return doubles;
    }

    double[] aggregateData(CellAggregationContext context) {
        final GridDef sourceGridDef = context.getSourceArrayGrid().getGridDef();
        final GridDef targetGridDef = context.getTargetArrayGrid().getGridDef();
        assert (sourceGridDef.getTime() == 1);
        assert (targetGridDef.getTime() == 1);
        context.setNumberOfCellsToAggregateInEachDimension(calculateResolution(sourceGridDef, targetGridDef));

        int targetArrayLength = targetGridDef.getNumberOfCells();
        double[] targetData = new double[targetArrayLength];
        for (int targetCellIndex = 0; targetCellIndex < targetArrayLength; targetCellIndex++) {
            context.setTargetCellIndex(targetCellIndex);
            final double mean = calculator.calculate(context);
            targetData[targetCellIndex] = mean;
        }
        return targetData;
    }

    private void fillTargetStorage(ArrayGrid targetArrayGrid, double[] targetDataScaled) {
        final Object targetStorageObj = targetArrayGrid.getArray().getStorage();
        double scaling = targetArrayGrid.getScaling();
        double offset = targetArrayGrid.getOffset();
        Number fillValue = targetArrayGrid.getFillValue();
        String type = targetArrayGrid.getArray().getElementType().getName();

        if ("short".equals(type)) {
            short[] store = (short[]) targetStorageObj;
            for (int i = 0; i < targetDataScaled.length; i++) {
                double scaledValue = targetDataScaled[i];
                if (Double.isNaN(scaledValue)) {
                    store[i] = fillValue.shortValue();
                } else {
                    long value = Math.round((scaledValue - offset) / scaling);
                    store[i] = (short) value;
                }
            }
        } else if ("byte".equals(type)) {
            byte[] store = (byte[]) targetStorageObj;
            for (int i = 0; i < targetDataScaled.length; i++) {
                double scaledValue = targetDataScaled[i];
                if (Double.isNaN(scaledValue)) {
                    store[i] = fillValue.byteValue();
                } else {
                    long value = Math.round((scaledValue - offset) / scaling);
                    store[i] = (byte) value;
                }
            }
        } else if ("int".equals(type)) {
            int[] store = (int[]) targetStorageObj;
            for (int i = 0; i < targetDataScaled.length; i++) {
                double scaledValue = targetDataScaled[i];
                if (Double.isNaN(scaledValue)) {
                    store[i] = fillValue.intValue();
                } else {
                    long value = Math.round((scaledValue - offset) / scaling);
                    store[i] = (int) value;
                }
            }
        } else {
            throw new RuntimeException("byte[], short[] or int[] expected, but found " + type + "[].");
        }
    }

    //Number of cell to be put in one target cell (return > 1 -> coarser grid)
    private int calculateResolution(GridDef sourceGridDef, GridDef targetGridDef) {
        return (int) (targetGridDef.getResolutionX() / sourceGridDef.getResolutionX());
    }
}
