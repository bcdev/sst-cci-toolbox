package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.UTC;
import ucar.ma2.Array;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Utility that performs the regional averaging.
 *
 * @author Norman Fomferra
 */
public class RegionalAverager {

    public static void computeOutputTimeSteps(ProductStore productStore, Date startDate, Date endDate, TemporalResolution temporalResolution, RegionList regionList) throws IOException {
        Calendar calendar = UTC.createCalendar(startDate);
        while (calendar.getTime().before(endDate)) {
            Date date1 = calendar.getTime();
            if (temporalResolution == TemporalResolution.daily) {
                calendar.add(Calendar.DATE, 1);
            } else if (temporalResolution == TemporalResolution.monthly) {
                calendar.add(Calendar.MONTH, 1);
            } else if (temporalResolution == TemporalResolution.seasonal) {
                calendar.add(Calendar.MONTH, 3);
            } else /*if (temporalResolution == TemporalResolution.anual)*/ {
                calendar.add(Calendar.YEAR, 1);
            }
            Date date2 = calendar.getTime();

            computeOutputTimeStep(productStore, temporalResolution, date1, date2, regionList);
        }
    }

    private static void computeOutputTimeStep(ProductStore productStore, TemporalResolution temporalResolution, Date date1, Date date2, RegionList regionList) throws IOException {
        DateFormat isoDateFormat = UTC.getIsoFormat();
        System.out.printf("Computing output point for time range from %s to %s%n",
                          isoDateFormat.format(date1), isoDateFormat.format(date2));

        if (temporalResolution.ordinal() <= TemporalResolution.monthly.ordinal()) {
            Calendar calendar = UTC.createCalendar(date1);
            while (calendar.getTime().before(date2)) {
                File[] files = productStore.getFileTree().get(calendar.getTime());
                if (files.length > 0) {
                    System.out.println("  " + files.length + " file(s) found");
                    Aggregator aggregator = new Aggregator();
                    aggregator.add("sst_skin", new Aggregator.MeanAcccumulator());
                    aggregator.add("uncertainty", new Aggregator.MeanSqrAcccumulator());
                    aggregator.aggregate(files, new Aggregator.Processor() {
                        @Override
                        public void process(String name, int x, int y, int w, int h, Array result) {
                            System.out.println("Computed average for " + name + ", line " + y);

                        }
                    });
                }
                calendar.add(Calendar.DATE, 1);
            }
        } else {
            Calendar calendar = UTC.createCalendar(date1);
            while (calendar.getTime().before(date2)) {
                File[] files = productStore.getFileTree().get(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
                new Aggregator().aggregate(files, new Aggregator.Processor() {
                    @Override
                    public void process(String name, int x, int y, int w, int h, Array result) {

                    }
                });
                calendar.add(Calendar.MONTH, 1);
            }
        }
    }
}
