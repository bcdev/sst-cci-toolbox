package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.Grid;
import org.esa.cci.sst.util.GridCell;
import org.esa.cci.sst.util.GridDef;
import org.esa.cci.sst.util.UTC;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// todo - make it a parameterised algorithm object

/**
 * Utility that performs the regional averaging.
 *
 * @author Norman Fomferra
 */
public class RegionalAveraging {
    private static final GridDef GLOBAL_5_DEG_GRID_DEF = GridDef.createGlobalGrid(5.0);
    private static final GridDef GLOBAL_90_DEG_GRID_DEF = GridDef.createGlobalGrid(90.0);

    public static class OutputTimeStep {
        final Date date1;
        final Date date2;
        final List<GridCell> regionalAverages;

        public OutputTimeStep(Date date1, Date date2, List<GridCell> regionalAverages) {
            this.date1 = date1;
            this.date2 = date2;
            this.regionalAverages = regionalAverages;
        }
    }

    public static List<OutputTimeStep> computeOutputTimeSteps(ProductStore productStore,
                                                              Climatology climatology,
                                                              Date startDate, Date endDate,
                                                              TemporalResolution temporalResolution,
                                                              RegionMaskList regionMaskList)
            throws IOException {

        List<OutputTimeStep> results = new ArrayList<OutputTimeStep>();
        Calendar calendar = UTC.createCalendar(startDate);
        while (calendar.getTime().before(endDate)) {
            Date date1 = calendar.getTime();
            List<GridCell> result;
            if (temporalResolution == TemporalResolution.daily) {
                calendar.add(Calendar.DATE, 1);
                result = computeOutputTimeStep(productStore, climatology, date1, calendar.getTime(), temporalResolution, regionMaskList);
            } else if (temporalResolution == TemporalResolution.monthly) {
                calendar.add(Calendar.MONTH, 1);
                result = computeOutputTimeStep(productStore, climatology, date1, calendar.getTime(), temporalResolution, regionMaskList);
            } else if (temporalResolution == TemporalResolution.seasonal) {
                calendar.add(Calendar.MONTH, 3);
                result = computeMonthlyOutputTimeStep(productStore, climatology, regionMaskList, date1, calendar.getTime());
            } else /*if (temporalResolution == TemporalResolution.anual)*/ {
                calendar.add(Calendar.YEAR, 1);
                result = computeMonthlyOutputTimeStep(productStore, climatology, regionMaskList, date1, calendar.getTime());
            }
            results.add(new OutputTimeStep(date1, calendar.getTime(), result));
        }
        return results;
    }

    private static List<GridCell> computeMonthlyOutputTimeStep(ProductStore productStore, Climatology climatology, RegionMaskList regionMaskList, Date date1, Date date2) throws IOException {
        List<OutputTimeStep> intermediateResults = computeOutputTimeSteps(productStore, climatology, date1, date2, TemporalResolution.monthly, regionMaskList);
        return aggregateMultiMonthsResults(intermediateResults);
    }

    private static List<GridCell> aggregateMultiMonthsResults(List<OutputTimeStep> monthlyResults) {
        // todo - aggregate results according to Nick's equations
        return null;
    }

    private static List<GridCell> computeOutputTimeStep(ProductStore productStore,
                                                        Climatology climatology,
                                                        Date date1,
                                                        Date date2,
                                                        TemporalResolution temporalResolution,
                                                        RegionMaskList regionMaskList) throws IOException {
        DateFormat isoDateFormat = UTC.getIsoFormat();

        List<File> files = productStore.getFiles(date1, date2);

        System.out.printf("Computing output time step from %s to %s. %d file(s) found.%n",
                          isoDateFormat.format(date1), isoDateFormat.format(date2), files.size());

        RegionMask combinedRegionMask = RegionMask.or(regionMaskList);

        Grid combined5DGrid = new Grid(GLOBAL_5_DEG_GRID_DEF);

        // todo - Aggregate all variables of all netcdfFile into 72x36 5deg grid boxes
        for (File file : files) {
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            long t0 = System.currentTimeMillis();
            System.out.printf("Aggregating file %s\n", netcdfFile.getLocation());
            try {
                // todo - generalise code: the following code is for ARC L2U

                float time = netcdfFile.findTopVariable("time").readScalarFloat();
                // todo - get climatology for time
                Variable sstVar = netcdfFile.findTopVariable("sst_skin");
                Variable uncertaintyVar = netcdfFile.findTopVariable("uncertainty");
                Variable maskVar = netcdfFile.findTopVariable("mask");
                GridDef arcGridDef = GridDef.createGlobalGrid(3600, 1800);
                Reader reader = new ArcL3Reader();
                Aggregator aggregator = new ArcL3Aggregator();
                // todo - check all variables exist and have the expected grid sizes

                // note: the following loop may be inefficient for Global or Hemispheric coverage
                for (int cellY = 0; cellY < combinedRegionMask.getHeight(); cellY++) {
                    for (int cellX = 0; cellX < combinedRegionMask.getWidth(); cellX++) {
                        if (combinedRegionMask.getSampleForCell(cellX, cellY)) {
                            Rectangle2D lonLatRectangle = combinedRegionMask.getGrid().getLonLatRectangle(cellX, cellY);
                            Rectangle gridRectangle = arcGridDef.getGridRectangle(lonLatRectangle);

                            Array sstData = reader.read(sstVar, gridRectangle);
                            Array maskData = reader.read(maskVar, gridRectangle);
                            // System.out.println("sstData = " + sstData);

                            GridCell combined5DGridCell = combined5DGrid.getGridCellSafe(cellX, cellY);
                            aggregator.aggregate(sstData, maskData, getFillValue(sstVar), combined5DGridCell);
                            // todo - for each uncertainty variables A-G: create a grid comprising max. 72 x 36 aggregation results.
                        }
                    }
                }
            } finally {
                netcdfFile.close();
            }

            long t1 = System.currentTimeMillis();

            long dt = t1 - t0;
            System.out.println("reading took " + dt + " ms");
        }

        List<GridCell> regionalGridCells = new ArrayList<GridCell>();
        for (RegionMask regionMask : regionMaskList) {
            // if we have multiple masks, split the aggregation results we've got to each region
            Grid regional5DGrid = new Grid(combined5DGrid.getGridDef());
            for (int cellY = 0; cellY < regionMask.getHeight(); cellY++) {
                for (int cellX = 0; cellX < regionMask.getWidth(); cellX++) {
                    if (combinedRegionMask.getSampleForCell(cellX, cellY)) {
                        regional5DGrid.setGridCell(cellX, cellY, combined5DGrid.getGridCellSafe(cellX, cellY));
                    }
                }
            }
            // check if region is Globe or Hemisphere (actually: check if we have fully covered 90 deg grid boxes),
            // if so apply special averaging for all 90 deg grid boxes.
            if (regionMask.getCoverage() == RegionMask.Coverage.Globe
                    || regionMask.getCoverage() == RegionMask.Coverage.N_Hemisphere
                    || regionMask.getCoverage() == RegionMask.Coverage.S_Hemisphere) {
                int width90D = GLOBAL_90_DEG_GRID_DEF.getWidth();
                int height90D = GLOBAL_90_DEG_GRID_DEF.getHeight();
                Grid regional90DGrid = new Grid(GLOBAL_90_DEG_GRID_DEF);

                for (int cellY = 0; cellY < regionMask.getHeight(); cellY++) {
                    for (int cellX = 0; cellX < regionMask.getWidth(); cellX++) {
                        if (combinedRegionMask.getSampleForCell(cellX, cellY)) {
                            GridCell regional5DGridCell = regional5DGrid.getGridCell(cellX, cellY);
                            if (regional5DGridCell != null) {
                                int cellX90D = (cellX * width90D) / regional5DGrid.getGridDef().getWidth();
                                int cellY90D = (cellY * height90D) / regional5DGrid.getGridDef().getHeight();
                                GridCell regional90DGridCell = regional90DGrid.getGridCellSafe(cellX90D, cellY90D);
                                regional90DGridCell.aggregate(regional5DGridCell);
                            }
                        }
                    }
                }

                regionalGridCells.add(regional90DGrid.combine());
            } else {
                regionalGridCells.add(regional5DGrid.combine());
            }
        }

        return regionalGridCells;
    }

    private static Number getFillValue(Variable sstVar) {
        Attribute attribute = sstVar.findAttribute("_FillValue");
        if (attribute == null) {
            return null;
        }
        return attribute.getNumericValue();
    }

    public interface Reader {
        Array read(Variable variable, Rectangle gridRectangle) throws IOException;
    }

    public static class ArcL3Reader implements Reader {
        @Override
        public Array read(Variable variable, Rectangle gridRectangle) throws IOException {
            //System.out.println("reading sstVar rect " + gridRectangle);
            Array array;
            try {
                array = variable.read(new int[]{0, gridRectangle.y, gridRectangle.x},
                                      new int[]{1, gridRectangle.height, gridRectangle.width});
            } catch (InvalidRangeException e) {
                throw new IllegalStateException(e);
            }
            return array;
        }
    }

    public interface Aggregator {
        void aggregate(Array sstData, Array maskData, Number fillValue, GridCell gridCell);
    }

    public static class ArcL3Aggregator implements Aggregator {
        @Override
        public void aggregate(Array sstData, Array maskData, Number fillValue, GridCell gridCell) {
            int size = (int) sstData.getSize();
            for (int i = 0; i < size; i++) {
                double sstSample = sstData.getDouble(i);
                int maskSample = maskData.getInt(i);

                boolean isQualityLevel5 = (maskSample & 0x10) != 0;
               //System.out.println("isQualityLevel5 = " + isQualityLevel5);
                boolean isNumber = !Double.isNaN(sstSample);
                boolean hasData = fillValue == null || sstSample != fillValue.doubleValue();

                isQualityLevel5 = true;
                if (isQualityLevel5 && isNumber && hasData) {
                    gridCell.aggregate(sstSample, 1);
                }
            }
        }
    }

}
