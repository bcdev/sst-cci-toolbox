package org.esa.cci.sst.common.calculator;

/**
 * {@author Bettina Scholze}
 * Date: 09.11.12 14:43
 */
public interface CoverageUncertainty {

    /**
     * Calculates the coverage uncertainty of the cell, which is a grosser target cell.
     *
     * @param cellX Index x of current cell
     * @param cellY Index y of current cell
     * @param n Number of valid source grid boxes used for the averaging
     * @param a e.g. averageStdDeviation (Regridding Tool) or demanded resolution of Lut value (Regional Averaging Tool)
     * @return Coverage uncertainty for current cell
     */
    double calculateCoverageUncertainty(int cellX, int cellY, long n, double a);
}
