package org.esa.cci.sst.common.calculator;

import org.esa.cci.sst.common.cell.AggregationCell;

/**
 * {@author Bettina Scholze}
 * Date: 13.09.12 10:08
 */
public class SynopticAreaCountEstimator {

    public SynopticAreaCountEstimator() {
        //ArrayGrid
    }

    public double calculateEta(AggregationCell cell) {

        double dxy = 0.0; //todo from LUT (each cell and grid)
        double dt = 0.0; //todo from LUT (each cell and grid)

        double lxy = 100.0; //km
        double lt = 1.0; //day

        double e = -0.5 * (dxy / lxy + dt / lt);
        double r = Math.exp(e);

        return cell.getSampleCount() / (1 + r * (cell.getSampleCount() - 1));
    }
}
