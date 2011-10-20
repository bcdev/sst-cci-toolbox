package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.*;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

/**
 * @author Norman
 */
public class AggregatorTest {
    @Test
    public void aggregateSources() throws Exception {
        GridDef sourceGridDef = GridDef.createGlobal(0.1);

        AggregationCell5Context context1 = new AggregationCell5Context(sourceGridDef,
                                                                      new Grid[]{
                                                                              new ScalarGrid(sourceGridDef, 292.0),
                                                                      },
                                                                      new ScalarGrid(sourceGridDef, 292.5),
                                                                      new ScalarGrid(sourceGridDef, 0.8));
        AggregationCell5Context context2 = new AggregationCell5Context(sourceGridDef,
                                                                      new Grid[]{
                                                                              new ScalarGrid(sourceGridDef, 293.0),
                                                                      },
                                                                      new ScalarGrid(sourceGridDef, 291.5),
                                                                      new ScalarGrid(sourceGridDef, 0.8));
        AggregationCell5Context context3 = new AggregationCell5Context(sourceGridDef,
                                                                      new Grid[]{
                                                                              new ScalarGrid(sourceGridDef, 291.0),
                                                                      },
                                                                      new ScalarGrid(sourceGridDef, 289.5),
                                                                      new ScalarGrid(sourceGridDef, 0.8));
        AggregationCell5Context context4 = new AggregationCell5Context(sourceGridDef,
                                                                      new Grid[]{
                                                                              new ScalarGrid(sourceGridDef, 291.0),
                                                                      },
                                                                      new ScalarGrid(sourceGridDef, 288.5),
                                                                      new ScalarGrid(sourceGridDef, 0.8));

        RegionMask regionMask = RegionMask.create("East_Atlantic", -25, 45, -15, 35);

        CellGrid<MyCell5> cell5Grid = new CellGrid<MyCell5> (GridDef.createGlobal(5.0), new MyCell5Factory());
        Aggregator.aggregateSources(context1, regionMask, cell5Grid);
        Aggregator.aggregateSources(context2, regionMask, cell5Grid);
        Aggregator.aggregateSources(context3, regionMask, cell5Grid);
        Aggregator.aggregateSources(context4, regionMask, cell5Grid);

        assertNull(cell5Grid.getCell(30, 8));
        assertNull(cell5Grid.getCell(31, 8));
        assertNull(cell5Grid.getCell(32, 8));
        assertNull(cell5Grid.getCell(33, 8));

        assertNull(cell5Grid.getCell(30, 9));
        assertNotNull(cell5Grid.getCell(31, 9));
        assertNotNull(cell5Grid.getCell(32, 9));
        assertNull(cell5Grid.getCell(33, 9));

        assertNull(cell5Grid.getCell(30, 10));
        assertNotNull(cell5Grid.getCell(31, 10));
        assertNotNull(cell5Grid.getCell(32, 10));
        assertNull(cell5Grid.getCell(33, 10));

        assertNull(cell5Grid.getCell(30, 11));
        assertNull(cell5Grid.getCell(31, 11));
        assertNull(cell5Grid.getCell(32, 11));
        assertNull(cell5Grid.getCell(33, 11));

        MyCell5 cell5 = cell5Grid.getCell(31, 9);
        assertEquals(4L, cell5.getSampleCount());
        assertEquals(1.25, cell5.getMean(), 1.e-10);
    }

    @Test
    public void aggregateCell5GridToCell90Grid() throws Exception {
        // todo - write test
        // Aggregator.aggregateCell5GridToCell90Grid(null, null, null);
    }

    @Test
    public void aggregateCell5OrCell90Grid() throws Exception {
        // todo - write test
        // Aggregator.aggregateCellGrid(null, null, null);
    }

    @Test
    public void aggregateRegions() throws Exception {
        // todo - write test
        // Aggregator.aggregateRegions(null, null, null, null, null, null);
    }

    @Test
    public void aggregateSameMonthAggregationsToMultiMonthAggregation() throws Exception {
        // todo - write test
        // Aggregator.aggregateSameMonthAggregationsToMultiMonthAggregation(null, 0, null);
    }

    @Test
    public void getCell5GridForRegion() throws Exception {
        // todo - write test
        // Aggregator.getCell5GridForRegion(null, null);
    }

    @Test
    public void mustAggregateTo90() throws Exception {
        assertEquals(true, Aggregator.mustAggregateTo90(RegionMask.create("G", -180, 90, 180, -90)));
        assertEquals(true, Aggregator.mustAggregateTo90(RegionMask.create("NH", -180, 90, 180, 0)));
        assertEquals(true, Aggregator.mustAggregateTo90(RegionMask.create("SH", -180, 0, 180, -90)));
        assertEquals(false, Aggregator.mustAggregateTo90(RegionMask.create("X", -180, 90, 180, -85)));
        assertEquals(false, Aggregator.mustAggregateTo90(RegionMask.create("X", 0, 10, 10, 0)));
    }

    private static class MyCell5Factory implements CellFactory<MyCell5> {
        @Override
        public MyCell5 createCell(int x, int y) {
            return new MyCell5(x,y);
        }

    }
    private static class MyCell5 extends AbstractCell implements AggregationCell5 {
        double anomalySum;
        int sampleCount;

        private MyCell5(int x, int y) {
            super(x, y);
        }

        @Override
        public void accumulate(AggregationCell5Context aggregationCell5Context, Rectangle rect) {
            anomalySum += aggregationCell5Context.getSourceGrids()[0].getSampleDouble(0, 0)
                   - aggregationCell5Context.getAnalysedSstGrid().getSampleDouble(0,0);
            sampleCount++;
        }

        @Override
        public long getSampleCount() {
            return sampleCount;
        }

        @Override
        public Number[] getResults() {
            return new Number[] {getMean()};
        }

        double getMean() {
            return anomalySum / sampleCount;
        }

        @Override
        public boolean isEmpty() {
            return sampleCount == 0;
        }


    }
}
