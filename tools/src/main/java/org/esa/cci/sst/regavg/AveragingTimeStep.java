package org.esa.cci.sst.regavg;

import org.esa.cci.sst.common.RegionalAggregation;
import org.esa.cci.sst.common.TimeStep;

import java.util.Date;
import java.util.List;

/**
 * {@author Bettina Scholze}
 * Date: 04.09.12 13:56
 */
public class AveragingTimeStep implements TimeStep {

    private final Date startDate;
    private final Date endDate;
    private final List<RegionalAggregation> regionalAggregations;

    public AveragingTimeStep(Date startDate, Date endDate, List<RegionalAggregation> regionalAggregations) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.regionalAggregations = regionalAggregations;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    public int getRegionCount() {
        return regionalAggregations.size();
    }

    public Number[] getRegionalAggregationResults(int regionIndex) {
        RegionalAggregation regionalAggregation = regionalAggregations.get(regionIndex);
        return regionalAggregation.getResults();
    }

    public RegionalAggregation getRegionalAggregation(int regionIndex) {
        return regionalAggregations.get(regionIndex);
    }
}
