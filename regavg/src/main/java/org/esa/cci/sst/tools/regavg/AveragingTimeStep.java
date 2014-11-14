package org.esa.cci.sst.tools.regavg;

import org.esa.cci.sst.aggregate.RegionalAggregation;
import org.esa.cci.sst.common.TimeStep;

import java.util.Date;
import java.util.List;

/**
 * {@author Bettina Scholze}
 * Date: 04.09.12 13:56
 */
class AveragingTimeStep implements TimeStep {

    private final Date startDate;
    private final Date endDate;
    private final List<RegionalAggregation> regionalAggregations;

    AveragingTimeStep(Date startDate, Date endDate, List<RegionalAggregation> regionalAggregations) {
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

    public Number[] getRegionalAggregationResults(int regionIndex) {
        final RegionalAggregation regionalAggregation = regionalAggregations.get(regionIndex);
        if (regionalAggregation == null) {
            return new Number[0];
        }
        return regionalAggregation.getResults();
    }

    RegionalAggregation getRegionalAggregation(int regionIndex) {
        return regionalAggregations.get(regionIndex);
    }
}
