package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.Cell;
import org.esa.cci.sst.util.CellGrid;
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
        final List<Cell> regionalAverages;

        public OutputTimeStep(Date date1, Date date2, List<Cell> regionalAverages) {
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
            List<Cell> result;
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

    private static List<Cell> computeMonthlyOutputTimeStep(ProductStore productStore, Climatology climatology, RegionMaskList regionMaskList, Date date1, Date date2) throws IOException {
        List<OutputTimeStep> intermediateResults = computeOutputTimeSteps(productStore, climatology, date1, date2, TemporalResolution.monthly, regionMaskList);
        return aggregateMultiMonthsResults(intermediateResults);
    }

    private static List<Cell> aggregateMultiMonthsResults(List<OutputTimeStep> monthlyResults) {
        // todo - aggregate results according to Nick's equations
        return null;
    }

    private static List<Cell> computeOutputTimeStep(ProductStore productStore,
                                                        Climatology climatology,
                                                        Date date1,
                                                        Date date2,
                                                        TemporalResolution temporalResolution,
                                                        RegionMaskList regionMaskList) throws IOException {

        RegionMask combinedRegionMask = RegionMask.combine(regionMaskList);

        CellGrid combined5DGrid = computeCombinedGrid(productStore, climatology, date1, date2, combinedRegionMask);

        List<Cell> regionalCells = new ArrayList<Cell>();
        for (RegionMask regionMask : regionMaskList) {
            // if we have multiple masks, split the aggregation results we've got to each region grid
            CellGrid regional5DegGrid = getRegionalGrid(combined5DGrid, combinedRegionMask);

            // check if region is Globe or Hemisphere (actually: check if we have fully covered
            // 90 deg grid boxes), if so apply special averaging for all 90 deg grid boxes.
            if (regionMask.getCoverage() == RegionMask.Coverage.Globe
                    || regionMask.getCoverage() == RegionMask.Coverage.N_Hemisphere
                    || regionMask.getCoverage() == RegionMask.Coverage.S_Hemisphere) {
                CellGrid regional90DegGrid = averageTo90DegGrid(regional5DegGrid, combinedRegionMask);
                regionalCells.add(regional90DegGrid.combine());
            } else {
                regionalCells.add(regional5DegGrid.combine());
            }
        }

        return regionalCells;
    }

    private static CellGrid getRegionalGrid(CellGrid combined5DGrid, RegionMask combinedRegionMask) {
        CellGrid regional5DGrid = new CellGrid(combined5DGrid.getGridDef());
        for (int cellY = 0; cellY < combined5DGrid.getGridDef().getHeight(); cellY++) {
            for (int cellX = 0; cellX < combined5DGrid.getGridDef().getWidth(); cellX++) {
                if (combinedRegionMask.getSampleForCell(cellX, cellY)) {
                    regional5DGrid.setCell(cellX, cellY, combined5DGrid.getCellSafe(cellX, cellY));
                }
            }
        }
        return regional5DGrid;
    }

    private static CellGrid averageTo90DegGrid(CellGrid regional5DGrid, RegionMask combinedRegionMask) {
        CellGrid regional90DegGrid = new CellGrid(GLOBAL_90_DEG_GRID_DEF);
        for (int cellY = 0; cellY < regional5DGrid.getGridDef().getHeight(); cellY++) {
            for (int cellX = 0; cellX < regional5DGrid.getGridDef().getWidth(); cellX++) {
                if (combinedRegionMask.getSampleForCell(cellX, cellY)) {
                    Cell regional5DegCell = regional5DGrid.getCell(cellX, cellY);
                    if (regional5DegCell != null) {
                        int cellX90D = (cellX * regional90DegGrid.getGridDef().getWidth()) / regional5DGrid.getGridDef().getWidth();
                        int cellY90D = (cellY * regional90DegGrid.getGridDef().getHeight()) / regional5DGrid.getGridDef().getHeight();
                        Cell regional90DCell = regional90DegGrid.getCellSafe(cellX90D, cellY90D);
                        regional90DCell.accumulate(regional5DegCell);
                    }
                }
            }
        }
        return regional90DegGrid;
    }

    private static CellGrid computeCombinedGrid(ProductStore productStore, Climatology climatology, Date date1, Date date2, RegionMask combinedRegionMask) throws IOException {
        List<File> files = productStore.getFiles(date1, date2);

        DateFormat isoDateFormat = UTC.getIsoFormat();
        System.out.printf("Computing output time step from %s to %s. %d file(s) found.%n",
                          isoDateFormat.format(date1), isoDateFormat.format(date2), files.size());


        CellGrid combined5DGrid = new CellGrid(GLOBAL_5_DEG_GRID_DEF);

        // todo - Aggregate all variables of all netcdfFile into 72x36 5deg grid boxes
        for (File file : files) {
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            long t0 = System.currentTimeMillis();
            System.out.printf("Aggregating file %s\n", netcdfFile.getLocation());
            try {
                // todo - generalise code: the following code is for ARC L2U

                float time = netcdfFile.findTopVariable("time").readScalarFloat();
                int secondsSince1981 = Math.round(time);
                Calendar calendar = UTC.createCalendar(1981);
                calendar.add(Calendar.MILLISECOND, secondsSince1981);
                int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
                if (dayOfYear == 366) {
                    dayOfYear = 365; // Leap year
                }
                Array analysedSst = climatology.getAnalysedSst(dayOfYear - 1);

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

                            Cell combined5DCell = combined5DGrid.getCellSafe(cellX, cellY);
                            aggregator.aggregate(sstData, maskData, getFillValue(sstVar), combined5DCell);
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
        return combined5DGrid;
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
        void aggregate(Array sstData, Array maskData, Number fillValue, Cell cell);
    }

    public static class ArcL3Aggregator implements Aggregator {
        @Override
        public void aggregate(Array sstData, Array maskData, Number fillValue, Cell cell) {
            int size = (int) sstData.getSize();
            for (int i = 0; i < size; i++) {
                double sstSample = sstData.getDouble(i);
                int maskSample = maskData.getInt(i);

                boolean isQualityLevel5 = maskSample == 0; // (maskSample & 0x10) != 0;
                boolean isNumber = !Double.isNaN(sstSample);
                boolean hasData = fillValue == null || sstSample != fillValue.doubleValue();

                if (isQualityLevel5 && isNumber && hasData) {
                    cell.accumulate(sstSample);
                }
            }
        }
    }

}
