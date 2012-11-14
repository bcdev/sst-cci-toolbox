package org.esa.cci.sst.common.calculator;

/**
 * {@author Bettina Scholze}
 * Date: 09.11.12 14:43
 */
public interface CoverageUncertainty {

    double calculateCoverageUncertainty(int cellX, int cellY, long n, double averageStdDeviation);
}
