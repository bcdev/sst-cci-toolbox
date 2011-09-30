package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.Grid;
import org.esa.cci.sst.util.UTC;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

    private static Result computeOutputTimeStep(ProductStore productStore, Climatology climatology, RegionMaskList regionMaskList, Date date1, Date date2, TemporalResolution temporalResolution) throws IOException {
        DateFormat isoDateFormat = UTC.getIsoFormat();

        List<File> files = productStore.getFiles(date1, date2);

        System.out.printf("Computing output time step from %s to %s. %d file(s) found.%n",
                          isoDateFormat.format(date1), isoDateFormat.format(date2), files.size());

        RegionMask combinedMask = RegionMask.combineMasks(regionMaskList);

        // todo - Aggregate all variables of all netcdfFile into 72x36 5deg grid boxes
        Aggregation aggregation = new Aggregation(combinedMask);
        aggregation.startAggregation();
        for (File file : files) {
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            netcdfFile.findTopVariable("time");
            // todo - get file day-of-year and fetch matching climatology
            try {
                aggregation.aggregateVariables(netcdfFile);
            } finally {
                netcdfFile.close();
            }
        }
        aggregation.endAggregation();

        for (RegionMask regionMask : regionMaskList) {
            // todo - if we have multiple masks, split the aggregation results we've got to each region
            // todo - check if region is Globe or Hemisphere (actually: check if we have fully covered 90 deg grid boxes), if so apply special averaging for all 90 deg grid boxes.
        }

        // put the per-region results into result
        return new Result();
    }

    public static class Result {
        // todo
    }

    public static class Aggregation {
        RegionMask mask;

        public Aggregation(RegionMask mask) {
            this.mask = mask;
        }

        public void startAggregation() {
            // todo
        }

        public void endAggregation() {
            // todo
        }

        public void aggregateVariables(NetcdfFile netcdfFile) {

            // todo - for each SST variable and the uncertainty variables A-G: create a grid comprising max. 72 x 36 aggregation results.
            System.out.printf("Aggregating file %s\n", netcdfFile.getLocation());
            // for each SST variable and the uncertainty variables A-G:
            //    create grid comprising max. 72 x 36 aggregation results.
            //    Variable var = netcdfFile.findTopVariable(name)
            //    int[] shape = getGridBoxShape(productType, mask);
            //    for y=0...35, x=0...71
            //       if mask.isValid(x, y)
            //          int[] offs = getGridBoxOffs(productType, mask, x, y);
            //          Array array = var.read(offs, shape);
            //          aggregateVariable(var, array)
            //          ...
        }
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
