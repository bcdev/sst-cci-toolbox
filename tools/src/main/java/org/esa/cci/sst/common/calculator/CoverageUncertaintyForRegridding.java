package org.esa.cci.sst.common.calculator;

import org.apache.commons.lang.NotImplementedException;
import org.esa.cci.sst.common.auxiliary.LutForStdDeviation;
import org.esa.cci.sst.common.auxiliary.LutForXTimeSpace;

/**
 * {@author Bettina Scholze}
 * Date: 09.11.12 15:28
 */
public class CoverageUncertaintyForRegridding implements CoverageUncertainty {

    private LutForStdDeviation lutForStdDeviation;
    private LutForXTimeSpace lutForXTime;
    private LutForXTimeSpace lutForXSpace;


    @Override
    public double calculateCoverageUncertainty(int cellX, int cellY, long n, double averageStdDeviation) {


        throw new NotImplementedException();
    }
}
