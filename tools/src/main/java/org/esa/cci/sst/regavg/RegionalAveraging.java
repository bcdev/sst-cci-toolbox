package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.Grid;
import org.esa.cci.sst.util.UTC;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.*;
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

    public static List<Result> computeOutputTimeSteps(ProductStore productStore, Climatology climatology, RegionMaskList regionMaskList, Date startDate, Date endDate, TemporalResolution temporalResolution) throws IOException {
        List<Result> results = new ArrayList<Result>();
        Calendar calendar = UTC.createCalendar(startDate);
        while (calendar.getTime().before(endDate)) {
            Date date1 = calendar.getTime();
            Result result;
            if (temporalResolution == TemporalResolution.daily) {
                calendar.add(Calendar.DATE, 1);
                result = computeOutputTimeStep(productStore, climatology, regionMaskList, date1, calendar.getTime(), temporalResolution);
            } else if (temporalResolution == TemporalResolution.monthly) {
                calendar.add(Calendar.MONTH, 1);
                result = computeOutputTimeStep(productStore, climatology, regionMaskList, date1, calendar.getTime(), temporalResolution);
            } else if (temporalResolution == TemporalResolution.seasonal) {
                calendar.add(Calendar.MONTH, 3);
                result = computeMonthlyOutputTimeStep(productStore, climatology, regionMaskList, date1, calendar.getTime());
            } else /*if (temporalResolution == TemporalResolution.anual)*/ {
                calendar.add(Calendar.YEAR, 1);
                result = computeMonthlyOutputTimeStep(productStore, climatology, regionMaskList, date1, calendar.getTime());
            }
            results.add(result);
        }
        return results;
    }

    private static Result computeMonthlyOutputTimeStep(ProductStore productStore, Climatology climatology, RegionMaskList regionMaskList, Date date1, Date date2) throws IOException {
        List<Result> intermediateResults = computeOutputTimeSteps(productStore, climatology, regionMaskList, date1, date2, TemporalResolution.monthly);
        return aggregateMultiMonthsResults(intermediateResults);
    }

    private static Result aggregateMultiMonthsResults(List<Result> monthlyResults) {
        // todo - aggregate results according to Nick's equations
        return null;
    }

    private static Result computeOutputTimeStep(ProductStore productStore,
                                                Climatology climatology,
                                                RegionMaskList regionMaskList,
                                                Date date1, Date date2,
                                                TemporalResolution temporalResolution) throws IOException {
        DateFormat isoDateFormat = UTC.getIsoFormat();

        List<File> files = productStore.getFiles(date1, date2);

        System.out.printf("Computing output time step from %s to %s. %d file(s) found.%n",
                          isoDateFormat.format(date1), isoDateFormat.format(date2), files.size());

        RegionMask combinedRegionMask = RegionMask.or(regionMaskList);

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
                Grid arcGrid = Grid.createGlobalGrid(3600, 1800);
                // todo - check all variables exist and have the expected grid sizes

                // todo - optimize: the following loop may be inefficient for Global or Hemispheric coverage
                for (int cellY = 0; cellY < combinedRegionMask.getHeight(); cellY++) {
                    for (int cellX = 0; cellX < combinedRegionMask.getWidth(); cellX++) {
                          if (combinedRegionMask.getSampleForCell(cellX, cellY)) {
                              Rectangle2D lonLatRectangle = combinedRegionMask.getGrid().getLonLatRectangle(cellX, cellY);
                              Rectangle gridRectangle = arcGrid.getGridRectangle(lonLatRectangle);

                              Array sstData;
                              sstData = getArcL3UData(sstVar, gridRectangle);
                              // System.out.println("sstData = " + sstData);

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

        for (RegionMask regionMask : regionMaskList) {
            // todo - if we have multiple masks, split the aggregation results we've got to each region
            // todo - check if region is Globe or Hemisphere (actually: check if we have fully covered 90 deg grid boxes), if so apply special averaging for all 90 deg grid boxes.
        }

        // put the per-region results into result
        return new Result();
    }

    private static Array getArcL3UData(Variable sstVar, Rectangle gridRectangle) throws IOException {
        //System.out.println("reading sstVar rect " + gridRectangle);
        Array sstData;
        try {
            sstData = sstVar.read(new int[]{0, gridRectangle.y, gridRectangle.x},
                                        new int[]{1, gridRectangle.height, gridRectangle.width});
        } catch (InvalidRangeException e) {
            throw new IllegalStateException(e);
        }
        return sstData;
    }

    public static class Result {
        // todo
    }

    private static final Grid GLOBAL_5DEG_GRID = Grid.createGlobalGrid(5.0);

    private static class Var {

        final int width;
        final int height;
        final double[] samples;
        final int[] counts;

        private Var() {
            this.width = GLOBAL_5DEG_GRID.getWidth();
            this.height = GLOBAL_5DEG_GRID.getHeight();
            samples = new double[width * height];
            counts = new int[width * height];
        }
    }

}
