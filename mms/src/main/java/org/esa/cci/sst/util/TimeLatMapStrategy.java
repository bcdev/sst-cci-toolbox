package org.esa.cci.sst.util;

import org.esa.cci.sst.tools.samplepoint.TimeRange;

import java.util.Date;
import java.util.List;

class TimeLatMapStrategy implements MapStrategy {

    private final int width;
    private final int height;

    private double scale;
    private long startTime;

    TimeLatMapStrategy(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void initialize(List<SamplingPoint> samplingPoints) {
        final TimeRange timeRange = extractTimeRangeInFullMonths(samplingPoints);
        startTime = timeRange.getStartDate().getTime();
        scale = 1.0 / (timeRange.getStopDate().getTime() - startTime);
    }

    @Override
    public PlotPoint map(SamplingPoint samplingPoint) {
        final double x_scale = scale * (samplingPoint.getTime() - startTime);
        final double y_scale = (90.0 - samplingPoint.getLat()) / 180.0;
        final int x = (int) (x_scale * width);
        final int y = (int) (y_scale * height);
        return new PlotPoint(x, y);
    }

    // package access for testing only tb 2014-02-20
    static TimeRange extractTimeRangeInFullMonths(List<SamplingPoint> points) {
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;

        for (final SamplingPoint samplingPoint : points) {
            final long samplingPointTime = samplingPoint.getTime();
            if (samplingPointTime < minTime) {
                minTime = samplingPointTime;
            }

            if (samplingPointTime > maxTime) {
                maxTime = samplingPointTime;
            }
        }
        final Date startDate = new Date(minTime);
        final Date stopDate = new Date(maxTime);
        return new TimeRange(TimeUtil.getBeginOfMonth(startDate), TimeUtil.getEndOfMonth(stopDate));
    }
}
