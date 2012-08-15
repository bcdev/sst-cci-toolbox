package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.*;
import org.junit.Test;

import java.awt.*;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Norman
 */
public class AggregatorTest {
    @Test
    public void aggregateSources() throws Exception {
        GridDef sourceGridDef = GridDef.createGlobal(0.1);

        SpatialAggregationContext context1 = new SpatialAggregationContext(sourceGridDef,
                                                                       new Grid[]{
                                                                               new ScalarGrid(sourceGridDef, 292.0),
                                                                       },
                                                                       new ScalarGrid(sourceGridDef, 292.5),
                                                                       new ScalarGrid(sourceGridDef, 0.8));
        SpatialAggregationContext context2 = new SpatialAggregationContext(sourceGridDef,
                                                                       new Grid[]{
                                                                               new ScalarGrid(sourceGridDef, 293.0),
                                                                       },
                                                                       new ScalarGrid(sourceGridDef, 291.5),
                                                                       new ScalarGrid(sourceGridDef, 0.8));
        SpatialAggregationContext context3 = new SpatialAggregationContext(sourceGridDef,
                                                                       new Grid[]{
                                                                               new ScalarGrid(sourceGridDef, 291.0),
                                                                       },
                                                                       new ScalarGrid(sourceGridDef, 289.5),
                                                                       new ScalarGrid(sourceGridDef, 0.8));
        SpatialAggregationContext context4 = new SpatialAggregationContext(sourceGridDef,
                                                                       new Grid[]{
                                                                               new ScalarGrid(sourceGridDef, 291.0),
                                                                       },
                                                                       new ScalarGrid(sourceGridDef, 288.5),
                                                                       new ScalarGrid(sourceGridDef, 0.8));

        RegionMask regionMask = RegionMask.create("East_Atlantic", -25, 45, -15, 35);

        CellGrid<MyCell5> cell5Grid = new CellGrid<MyCell5>(GridDef.createGlobal(5.0), new MyCell5Factory());
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
        CellGrid<MyCell5> cell5Grid = new CellGrid<MyCell5>(Aggregator.GRID_DEF_GLOBAL_5, new MyCell5Factory());
        cell5Grid.getCellSafe(34, 1).set(3.0, 1);
        cell5Grid.getCellSafe(35, 1).set(7.0, 1);
        cell5Grid.getCellSafe(36, 1).set(4.0, 1);
        cell5Grid.getCellSafe(37, 1).set(0.0, 0); // empty
        cell5Grid.getCellSafe(38, 1).set(5.0, 1);
        cell5Grid.getCellSafe(39, 1).set(5.2, 1);
        ArrayGrid seaCoverage5Grid = ArrayGrid.createWith2DDoubleArray(Aggregator.GRID_DEF_GLOBAL_5);
        seaCoverage5Grid.setSample(34, 1, 0.8);
        seaCoverage5Grid.setSample(35, 1, 0.5);
        seaCoverage5Grid.setSample(36, 1, 0.7);
        seaCoverage5Grid.setSample(37, 1, 0.6);
        seaCoverage5Grid.setSample(38, 1, 0.5);
        seaCoverage5Grid.setSample(39, 1, 0.7);
        CellGrid<MyCell90> cell90Grid = Aggregator.aggregateCell5GridToCell90Grid(cell5Grid,
                                                                                  seaCoverage5Grid,
                                                                                  new MyCell90Factory());

        List<MyCell90> cell90List = cell90Grid.getCells(CellFilter.NON_EMPTY);
        assertEquals(2, cell90List.size());
        MyCell90 cell90_1 = cell90List.get(0);
        MyCell90 cell90_2 = cell90List.get(1);
        assertEquals(1, cell90_1.getX());
        assertEquals(0, cell90_1.getY());
        assertEquals(2, cell90_1.getSampleCount());
        assertEquals((3.0 * 0.8 + 7.0 * 0.5) / (0.8 + 0.5), cell90_1.getMean(), 1e-6);
        assertEquals(2, cell90_2.getX());
        assertEquals(0, cell90_2.getY());
        assertEquals(3, cell90_2.getSampleCount());
        assertEquals((4.0 * 0.7 + 5.0 * 0.5 + 5.2 * 0.7) / (0.7 + 0.5 + 0.7),
                     cell90_2.getMean(), 1e-6);
    }

    @Test
    public void aggregateCell5OrCell90Grid() throws Exception {
        CellGrid<MyCell5> cell5Grid = new CellGrid<MyCell5>(Aggregator.GRID_DEF_GLOBAL_5, new MyCell5Factory());
        cell5Grid.getCellSafe(34, 1).set(3.0, 1);
        cell5Grid.getCellSafe(35, 1).set(7.0, 1);
        cell5Grid.getCellSafe(36, 1).set(4.0, 1);
        cell5Grid.getCellSafe(37, 1).set(0.0, 0); // empty
        cell5Grid.getCellSafe(38, 1).set(5.0, 1);
        cell5Grid.getCellSafe(39, 1).set(5.2, 1);
        ArrayGrid seaCoverage5Grid = ArrayGrid.createWith2DDoubleArray(Aggregator.GRID_DEF_GLOBAL_5);
        seaCoverage5Grid.setSample(34, 1, 0.8);
        seaCoverage5Grid.setSample(35, 1, 0.5);
        seaCoverage5Grid.setSample(36, 1, 0.7);
        seaCoverage5Grid.setSample(37, 1, 0.6);
        seaCoverage5Grid.setSample(38, 1, 0.5);
        seaCoverage5Grid.setSample(39, 1, 0.7);

        MyCellSameMonthAggregation aggregation = new MyCellSameMonthAggregation();
        Aggregator.aggregateCell5OrCell90Grid(cell5Grid, seaCoverage5Grid, aggregation);

        assertEquals(5, aggregation.getSampleCount());
        assertEquals((3.0 * 0.8 + 7.0 * 0.5 + 4.0 * 0.7 + 5.0 * 0.5 + 5.2 * 0.7) / (0.8 + 0.5 + 0.7 + 0.5 + 0.7),
                     aggregation.getMean(), 1e-6);
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
            return new MyCell5(x, y);
        }
    }

    private static class MyCell extends AbstractCell implements AggregationCell {
        double sumX;
        double sumW;
        int sampleCount;

        protected MyCell(int x, int y) {
            super(x, y);
        }

        double getMean() {
            return sumX / sumW;
        }

        public void set(double sumX, int sampleCount) {
            set(sumX, sampleCount, sampleCount);
        }

        public void set(double sumX, double sumW, int sampleCount) {
            this.sumX = sumX;
            this.sumW = sumW;
            this.sampleCount = sampleCount;
        }

        @Override
        public long getSampleCount() {
            return sampleCount;
        }

        @Override
        public Number[] getResults() {
            return new Number[]{getMean()};
        }

        @Override
        public boolean isEmpty() {
            return sampleCount == 0;
        }
    }

    private static class MyCell5 extends MyCell implements SpatialAggregationCell {

        private MyCell5(int x, int y) {
            super(x, y);
        }

        @Override
        public void accumulate(SpatialAggregationContext spatialAggregationContext, Rectangle rect) {
            sumX += spatialAggregationContext.getSourceGrids()[0].getSampleDouble(0, 0)
                    - spatialAggregationContext.getAnalysedSstGrid().getSampleDouble(0, 0);
            sumW++;
            sampleCount++;
        }

    }

    private static class MyCell90Factory implements CellFactory<MyCell90> {
        @Override
        public MyCell90 createCell(int x, int y) {
            return new MyCell90(x, y);
        }
    }

    private static class MyCell90 extends MyCell implements AggregationCell90<MyCell5> {

        private MyCell90(int x, int y) {
            super(x, y);
        }

        @Override
        public void accumulate(MyCell5 cell, double w) {
            double x = w * cell.getMean();
            sumX += x;
            sumW += w;
            sampleCount++;
        }
    }

    private static class MyCellSameMonthAggregation extends  MyCell implements SameMonthAggregation<MyCell> {

        private MyCellSameMonthAggregation() {
            super(0, 0);
        }

        @Override
        public void accumulate(MyCell cell, double w) {
            double x = w * cell.getMean();
            sumX += x;
            sumW += w;
            sampleCount++;
        }
    }
}
