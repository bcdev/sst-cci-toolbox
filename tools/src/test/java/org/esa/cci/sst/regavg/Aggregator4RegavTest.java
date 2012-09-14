package org.esa.cci.sst.regavg;

import org.esa.cci.sst.common.SpatialAggregationContext;
import org.esa.cci.sst.common.cell.AbstractCell;
import org.esa.cci.sst.common.cell.AggregationCell;
import org.esa.cci.sst.common.cell.CellFactory;
import org.esa.cci.sst.common.cell.SpatialAggregationCell;
import org.esa.cci.sst.common.cellgrid.ArrayGrid;
import org.esa.cci.sst.common.cellgrid.CellGrid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.RegionMask;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class Aggregator4RegavTest {
    static final GridDef GRID_DEF_GLOBAL_5 = GridDef.createGlobal(5.0); //for tests only

    @Test
    public void testAggregateCell5OrCell90Grid() throws Exception {
        CellGrid<MyCell5> cell5Grid = new CellGrid<MyCell5>(GRID_DEF_GLOBAL_5, new MyCell5Factory());
        cell5Grid.getCellSafe(34, 1).set(3.0, 1);
        cell5Grid.getCellSafe(35, 1).set(7.0, 1);
        cell5Grid.getCellSafe(36, 1).set(4.0, 1);
        cell5Grid.getCellSafe(37, 1).set(0.0, 0); // empty
        cell5Grid.getCellSafe(38, 1).set(5.0, 1);
        cell5Grid.getCellSafe(39, 1).set(5.2, 1);
        ArrayGrid seaCoverage5Grid = ArrayGrid.createWith2DDoubleArray(GRID_DEF_GLOBAL_5);
        seaCoverage5Grid.setSample(34, 1, 0.8);
        seaCoverage5Grid.setSample(35, 1, 0.5);
        seaCoverage5Grid.setSample(36, 1, 0.7);
        seaCoverage5Grid.setSample(37, 1, 0.6);
        seaCoverage5Grid.setSample(38, 1, 0.5);
        seaCoverage5Grid.setSample(39, 1, 0.7);

        MyCellSameMonthAggregation aggregation = new MyCellSameMonthAggregation();
        //execution
        Aggregator4Regav.aggregateCell5OrCell90Grid(cell5Grid, seaCoverage5Grid, aggregation);

        assertEquals(5, aggregation.getSampleCount());
        assertEquals((3.0 * 0.8 + 7.0 * 0.5 + 4.0 * 0.7 + 5.0 * 0.5 + 5.2 * 0.7) / (0.8 + 0.5 + 0.7 + 0.5 + 0.7),
                aggregation.getMean(), 1e-6);
    }

    @Test
    public void testAggregateRegions() throws Exception {
        // todo - write test
        // Aggregator.aggregateRegions(null, null, null, null, null, null);
    }

    @Test
    public void testAggregateSameMonthAggregationsToMultiMonthAggregation() throws Exception {
        // todo - write test
        // Aggregator.aggregateSameMonthAggregationsToMultiMonthAggregation(null, 0, null);
    }

    @Test
    public void testGetCell5GridForRegion() throws Exception {
        // todo - write test
        // Aggregator.getCell5GridForRegion(null, null);
    }

    @Test
    public void testMustAggregateTo90() throws Exception {
        assertEquals(true, Aggregator4Regav.mustAggregateTo90(RegionMask.create("G", -180, 90, 180, -90)));
        assertEquals(true, Aggregator4Regav.mustAggregateTo90(RegionMask.create("NH", -180, 90, 180, 0)));
        assertEquals(true, Aggregator4Regav.mustAggregateTo90(RegionMask.create("SH", -180, 0, 180, -90)));
        assertEquals(false, Aggregator4Regav.mustAggregateTo90(RegionMask.create("X", -180, 90, 180, -85)));
        assertEquals(false, Aggregator4Regav.mustAggregateTo90(RegionMask.create("X", 0, 10, 10, 0)));
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

    private static class MyCellSameMonthAggregation extends MyCell implements SameMonthAggregation<MyCell> {

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
