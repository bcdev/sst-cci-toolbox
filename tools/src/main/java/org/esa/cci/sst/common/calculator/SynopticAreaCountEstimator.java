package org.esa.cci.sst.common.calculator;

import org.esa.cci.sst.regrid.SpatialResolution;

/**
 * Eta is the estimated effective number of synoptic areas in the new grid box.
 * According to the Regridding Tool Specification equation 1.5 for synoptically correlated uncertainties.
 *
 * {@author Bettina Scholze}
 * Date: 13.09.12 10:08
 */
public abstract class SynopticAreaCountEstimator {
    private double spatialResolution;
    
    public SynopticAreaCountEstimator(SpatialResolution spatialResolution) {
        this.spatialResolution = spatialResolution.getValue();
    }

    public double getSpatialResolution() {
        return spatialResolution;
    }

    public double calculateEta(int x, int y, long sampleCount) {

        double dxy = getDxy(x, y);
        double dt = getDt(x, y);

        double lxy = 100.0; //km
        double lt = 1.0; //day

        double e = -0.5 * (dxy / lxy + dt / lt);
        double r = Math.exp(e);

        return sampleCount / (1 + r * (sampleCount - 1));
    }

    /**
     * 
     * @param x cell index x
     * @param y cell index y
     * @return in km
     */
   public abstract double getDxy(int x, int y);

    /**
     * 
     * @param x cell index x
     * @param y cell index y
     * @return in days
     */
    public abstract double getDt(int x, int y);
}
