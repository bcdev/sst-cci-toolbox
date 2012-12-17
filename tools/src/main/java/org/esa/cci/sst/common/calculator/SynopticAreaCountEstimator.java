package org.esa.cci.sst.common.calculator;

import org.esa.cci.sst.regrid.auxiliary.LutForSynopticAreas;

/**
 * Eta is the estimated effective number of synoptic areas in the new grid box.
 * According to the Regridding Tool Specification equation 1.5 for synoptically correlated uncertainties.
 * <p/>
 * {@author Bettina Scholze}
 * Date: 13.09.12 10:08
 */
public abstract class SynopticAreaCountEstimator {

    private LutForSynopticAreas lutForSynopticAreas;

    public SynopticAreaCountEstimator(LutForSynopticAreas lutForSynopticAreas) {
        this.lutForSynopticAreas = lutForSynopticAreas;
    }

    /**
     * Calculates eta, the parameter for the aggregation of synoptically correlated uncertainties.
     *
     * @param x           cell index x
     * @param y           cell index y
     * @param sampleCount valid input boxes
     * @return eta
     */
    public double calculateEta(int x, int y, long sampleCount) {

        //average time separation between each pair of input grid boxes
        double dt = lutForSynopticAreas.getDt();
        double dxy = getDxy(x, y);

        double lxy = 100.0; //km
        double lt = 1.0; //day

        double e = -0.5 * (dxy / lxy + dt / lt);
        double r = Math.exp(e);

        return sampleCount / (1 + r * (sampleCount - 1));
    }

    /**
     * dxy average distance in space between each pair of input grid boxes
     *
     * @param x cell index x
     * @param y cell index y
     * @return in km
     */
    public abstract double getDxy(int x, int y);
}
