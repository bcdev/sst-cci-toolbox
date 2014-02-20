package org.esa.cci.sst.util;


import java.util.List;

interface MapStrategy {

    void initialize(List<SamplingPoint> samplingPoints);

    PlotPoint map(SamplingPoint samplingPoint);
}
